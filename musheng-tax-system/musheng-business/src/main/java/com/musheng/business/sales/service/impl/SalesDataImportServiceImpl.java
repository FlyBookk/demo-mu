package com.musheng.business.sales.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.musheng.business.common.service.csv.CsvHeaderResult;
import com.musheng.business.common.service.csv.CsvParseServiceImpl;
import com.musheng.business.sales.dto.*;
import com.musheng.business.sales.entity.SalesData;
import com.musheng.business.sales.mapper.SalesDataMapper;
import com.musheng.business.sales.parser.ParseContext;
import com.musheng.business.sales.parser.ParseResult;
import com.musheng.business.sales.parser.SalesDataParser;
import com.musheng.business.sales.parser.SalesDataParserFactory;
import com.musheng.business.sales.parser.SiteCodeResolver;
import com.musheng.business.sales.repository.SalesDataRepository;
import com.musheng.business.sales.service.SalesDataImportService;
import com.musheng.common.context.ShopContext;
import com.musheng.common.enums.SalesSourceType;
import com.musheng.common.exception.BusinessException;
import com.musheng.common.result.ErrorCode;
import com.musheng.config.importrecord.entity.ImportRecord;
import com.musheng.config.importrecord.mapper.ImportRecordMapper;
import com.musheng.config.mapping.entity.FieldMappingTemplate;
import com.musheng.config.mapping.entity.TransactionTypeMapping;
import com.musheng.config.mapping.mapper.FieldMappingTemplateMapper;
import com.musheng.config.mapping.mapper.TransactionTypeMappingMapper;
import com.musheng.config.marketplace.entity.Marketplace;
import com.musheng.config.marketplace.mapper.MarketplaceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 销售数据导入服务实现类
 * 
 * 职责：
 * 1. 处理销售数据文件上传
 * 2. 解析和预览导入数据
 * 3. 执行数据导入
 * 4. 跟踪导入进度
 * 
 * ⚠️ 核心原则：
 * 1. 禁止修改业务流程
 * 2. 禁止改变输出结果
 * 3. 只是将 Mapper 调用替换为 Repository 调用
 * 
 * @author wanhua
 * 10:30 2026年02月01日
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SalesDataImportServiceImpl implements SalesDataImportService {

    private final SalesDataMapper salesDataMapper;
    private final SalesDataRepository salesDataRepository;
    private final FieldMappingTemplateMapper fieldMappingTemplateMapper;
    private final TransactionTypeMappingMapper transactionTypeMappingMapper;
    private final MarketplaceMapper marketplaceMapper;
    private final ImportRecordMapper importRecordMapper;
    private final CsvParseServiceImpl csvParseService;
    private final ObjectMapper objectMapper;
    private final SalesDataParserFactory parserFactory;
    private final com.musheng.business.rate.service.RateService rateService;
    private final SqlSessionFactory sqlSessionFactory;
    
    // 文件缓存（临时存储上传的文件信息）
    private final Map<String, UploadedFileCache> uploadedFileCache = new ConcurrentHashMap<>();
    // 导入进度缓存
    private final Map<String, SalesImportProgress> importProgressCache = new ConcurrentHashMap<>();
    
    /**
     * 上传文件缓存内部类
     */
    private static class UploadedFileCache {
        String fileName;
        long fileSize;
        byte[] content;
        List<String> sourceFields;
        int headerRow;
        String detectedSiteCode;
        long uploadTime;
        
        UploadedFileCache(String fileName, long fileSize, byte[] content, 
                         List<String> sourceFields, int headerRow, String detectedSiteCode) {
            this.fileName = fileName;
            this.fileSize = fileSize;
            this.content = content;
            this.sourceFields = sourceFields;
            this.headerRow = headerRow;
            this.detectedSiteCode = detectedSiteCode;
            this.uploadTime = System.currentTimeMillis();
        }
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importData(String siteCode, MultipartFile file) {
        log.info("Importing sales data: siteCode={}, fileName={}",
                siteCode, file.getOriginalFilename());

        Map<String, Object> result = new HashMap<>();
        List<String> errors = new ArrayList<>();
        int totalCount = 0;
        int successCount = 0;
        int failCount = 0;

        // 获取当前店铺ID
        Long shopId = ShopContext.requireShopId();
        
        // Create import record
        ImportRecord importRecord = new ImportRecord();
        importRecord.setShopId(shopId);
        importRecord.setBatchNo(generateBatchNo());
        importRecord.setDataType("sales");
        importRecord.setFileName(file.getOriginalFilename());
        importRecord.setFileSize(file.getSize());
        importRecord.setImportStatus("processing");
        importRecordMapper.insert(importRecord);

        try {
            // Get marketplace config
            LambdaQueryWrapper<Marketplace> mpWrapper = new LambdaQueryWrapper<>();
            mpWrapper.eq(Marketplace::getSiteCode, siteCode);
            Marketplace marketplace = marketplaceMapper.selectOne(mpWrapper);

            if (marketplace == null) {
                throw new BusinessException(ErrorCode.DATA_NOT_EXIST, "Marketplace not found: " + siteCode);
            }

            // Get field mapping template
            Map<String, String> fieldMapping = getFieldMapping(siteCode, "sales");

            // Get transaction type mappings
            Map<String, String> transactionTypeMapping = getTransactionTypeMapping(siteCode);

            // Parse CSV file
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

                // Detect and skip header rows
                CsvHeaderResult headerResult = csvParseService.parseHeaders(file);
                List<String> headers = headerResult.getHeaders();
                int headerRowIndex = headerResult.getHeaderRowIndex();

                // Reset input stream and skip to data rows
                try (BufferedReader dataReader = new BufferedReader(
                        new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

                    // Skip header rows
                    for (int i = 0; i <= headerRowIndex; i++) {
                        dataReader.readLine();
                    }

                    CSVParser parser = CSVFormat.DEFAULT.parse(dataReader);

                    for (CSVRecord record : parser) {
                        totalCount++;
                        try {
                            SalesData salesData = parseSalesRecord(record, headers, fieldMapping,
                                    transactionTypeMapping, siteCode, marketplace, totalCount);

                            if (salesData != null) {
                                // Check for duplicate
                                if (!isDuplicate(salesData)) {
                                    salesData.setShopId(shopId);
                                    salesData.setImportBatchId(importRecord.getId());
                                    salesDataRepository.save(salesData);
                                    successCount++;
                                } else {
                                    failCount++;
                                    errors.add(String.format("Row %d: Duplicate order %s", totalCount, salesData.getOrderId()));
                                }
                            }
                        } catch (Exception e) {
                            failCount++;
                            errors.add(String.format("Row %d: %s", totalCount, e.getMessage()));
                            log.warn("Failed to import sales data at row {}: {}", totalCount, e.getMessage());
                        }
                    }
                }
            }

            // Update import record
            importRecord.setTotalCount(totalCount);
            importRecord.setSuccessCount(successCount);
            importRecord.setFailCount(failCount);
            importRecord.setImportStatus(failCount == 0 ? "success" : (successCount > 0 ? "partial" : "fail"));
            importRecord.setCompleteTime(LocalDateTime.now());
            if (!errors.isEmpty()) {
                importRecord.setErrorMessage(String.join("\n", errors.subList(0, Math.min(10, errors.size()))));
            }
            importRecordMapper.updateById(importRecord);

        } catch (BusinessException e) {
            importRecord.setImportStatus("fail");
            importRecord.setErrorMessage(e.getMessage());
            importRecordMapper.updateById(importRecord);
            throw e;
        } catch (Exception e) {
            log.error("Failed to import sales data", e);
            importRecord.setImportStatus("fail");
            importRecord.setErrorMessage(e.getMessage());
            importRecordMapper.updateById(importRecord);
            throw new BusinessException(ErrorCode.IMPORT_PARSE_ERROR, "Failed to parse file: " + e.getMessage());
        }

        result.put("totalCount", totalCount);
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("errors", errors.size() > 10 ? errors.subList(0, 10) : errors);
        result.put("batchNo", importRecord.getBatchNo());

        log.info("Sales data import completed: total={}, success={}, fail={}", totalCount, successCount, failCount);

        return result;
    }


    @Override
    public SalesUploadResult uploadFile(MultipartFile file, SalesSourceType sourceType, String siteCode) {
        log.info("上传销售数据文件: fileName={}, sourceType={}, siteCode={}", 
                file.getOriginalFilename(), sourceType, siteCode);
        
        try {
            byte[] content = file.getBytes();
            String fileContent = new String(content, StandardCharsets.UTF_8);
            
            // 解析表头
            List<String> sourceFields = new ArrayList<>();
            int headerRow = 1;
            String detectedSiteCode = siteCode;
            int totalRows = 0;
            
            // 使用更健壮的换行符分割（兼容 Windows/Unix/Mac）
            String[] lines = fileContent.split("\\r?\\n|\\r");
            totalRows = lines.length;
            
            log.debug("文件共 {} 行", totalRows);
            
            if (sourceType == SalesSourceType.ORIGINAL) {
                // 亚马逊原始数据：前7-8行是说明，需要找到真正的表头行
                for (int i = 0; i < Math.min(15, lines.length); i++) {
                    String line = lines[i].trim();
                    String lineLower = line.toLowerCase();
                    
                    // 检查是否是 CSV 格式行（以引号开头，包含多个字段）
                    boolean isCsvFormat = line.startsWith("\"") && line.contains("\",\"");
                    
                    if (!isCsvFormat) {
                        continue;
                    }
                    
                    // 表头通常包含 "date/time" 或 "datum/uhrzeit" 等时间字段作为第一列
                    boolean hasDateField = lineLower.contains("\"date/time\"") || 
                        lineLower.contains("\"datum/uhrzeit\"");
                    boolean hasDataFields = lineLower.contains("\"sku\"") || 
                        lineLower.contains("\"order id\"") ||
                        lineLower.contains("\"settlement id\"") ||
                        lineLower.contains("\"abrechnungsnummer\"");
                    
                    if (hasDateField && hasDataFields) {
                        headerRow = i + 1;
                        sourceFields = parseCsvLine(line);
                        log.info("检测到表头行: row={}, fieldsCount={}, firstFields={}", 
                                headerRow, sourceFields.size(), 
                                sourceFields.size() > 3 ? sourceFields.subList(0, 3) : sourceFields);
                        break;
                    }
                }
                
                // 如果没有检测到表头，尝试宽松匹配
                if (sourceFields.isEmpty()) {
                    log.warn("严格表头检测失败，尝试宽松匹配");
                    for (int i = 0; i < Math.min(15, lines.length); i++) {
                        String line = lines[i].trim();
                        String lineLower = line.toLowerCase();
                        
                        if (lineLower.contains("marketplace") && lineLower.contains("sku")) {
                            headerRow = i + 1;
                            sourceFields = parseCsvLine(line);
                            log.info("宽松检测到表头行: row={}, fieldsCount={}", headerRow, sourceFields.size());
                            break;
                        }
                    }
                }
                
                // 从数据中自动识别站点
                if (headerRow < lines.length) {
                    for (int i = headerRow; i < Math.min(headerRow + 10, lines.length); i++) {
                        String line = lines[i];
                        String detected = SiteCodeResolver.detectSiteFromLine(line);
                        if (detected != null) {
                            detectedSiteCode = detected;
                            break;
                        }
                    }
                }
                
                totalRows = totalRows - headerRow;
            } else {
                // ERP数据：第一行就是表头
                if (lines.length > 0) {
                    sourceFields = parseCsvLine(lines[0]);
                }
                totalRows = lines.length - 1;
            }
            
            // 生成文件ID
            String fileId = UUID.randomUUID().toString().replace("-", "");
            
            // 缓存文件信息
            uploadedFileCache.put(fileId, new UploadedFileCache(
                file.getOriginalFilename(),
                file.getSize(),
                content,
                sourceFields,
                headerRow,
                detectedSiteCode
            ));
            
            // 清理过期缓存（超过1小时）
            cleanExpiredCache();
            
            // 构建返回结果
            SalesUploadResult result = new SalesUploadResult();
            result.setFileId(fileId);
            result.setFileName(file.getOriginalFilename());
            result.setFileSize(file.getSize());
            result.setTotalRows(totalRows);
            result.setHeaderRow(headerRow);
            result.setSourceFields(sourceFields);
            result.setDetectedSiteCode(detectedSiteCode);
            
            log.info("文件上传成功: fileId={}, headerRow={}, sourceFields={}, detectedSiteCode={}", 
                    fileId, headerRow, sourceFields.size(), detectedSiteCode);
            
            return result;
            
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_ERROR, "文件上传失败: " + e.getMessage());
        }
    }


    @Override
    public SalesPreviewResult previewImport(SalesPreviewRequest request) {
        log.info("预览导入数据: fileId={}, sourceType={}, siteCode={}, templateId={}", 
                request.getFileId(), request.getSourceType(), request.getSiteCode(), request.getTemplateId());
        
        // 获取缓存的文件
        UploadedFileCache fileCache = uploadedFileCache.get(request.getFileId());
        if (fileCache == null) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND, "文件已过期，请重新上传");
        }
        
        // 获取映射模板
        FieldMappingTemplate template = fieldMappingTemplateMapper.selectById(request.getTemplateId());
        if (template == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST, "模板不存在");
        }
        
        try {
            // 解析映射配置
            Map<String, String> mappingConfig = parseMappingConfig(template.getMappingConfig());
            
            // 构建解析上下文
            ParseContext context = ParseContext.builder()
                    .sourceType(request.getSourceType())
                    .siteCode(request.getSiteCode())
                    .templateId(request.getTemplateId())
                    .fieldMapping(mappingConfig)
                    .quarter(request.getQuarter())
                    .build();
            
            // 获取解析器
            SalesDataParser parser = parserFactory.getParser(request.getSourceType());
            
            // 解析数据（仅预览前20条）
            String fileContent = new String(fileCache.content, StandardCharsets.UTF_8);
            ParseResult parseResult = parser.parse(fileContent, context, 20);
            
            // 构建预览结果
            SalesPreviewResult result = new SalesPreviewResult();
            result.setTotalRows(parseResult.getTotalRows());
            result.setPreviewRows(parseResult.getDataList().size());
            
            // 构建列元信息
            List<ColumnMeta> columns = buildColumnMeta(mappingConfig);
            result.setColumns(columns);
            
            // 构建预览数据
            List<Map<String, Object>> previewData = new ArrayList<>();
            for (SalesData data : parseResult.getDataList()) {
                previewData.add(salesDataToMap(data));
            }
            result.setData(previewData);
            
            // 构建映射状态
            SalesSourceType sourceType = request.getSourceType();
            Set<String> mappedTargets = new HashSet<>(mappingConfig.values());
            MappingStatus mappingStatus = new MappingStatus();
            mappingStatus.setTotalFields(getRequiredFields(sourceType).size() + getOptionalFields(sourceType).size());
            mappingStatus.setMappedFields(mappingConfig.size());
            
            // 检查必填字段是否都已映射
            List<String> requiredMissing = new ArrayList<>();
            for (String required : getRequiredFields(sourceType)) {
                if (!mappedTargets.contains(required)) {
                    requiredMissing.add(required);
                }
            }
            mappingStatus.setRequiredMissing(requiredMissing);
            result.setMappingStatus(mappingStatus);
            
            // 添加警告信息
            List<String> warnings = new ArrayList<>();
            if (parseResult.getErrors() != null && !parseResult.getErrors().isEmpty()) {
                for (int i = 0; i < Math.min(5, parseResult.getErrors().size()); i++) {
                    ParseResult.ParseError error = parseResult.getErrors().get(i);
                    warnings.add(String.format("第%d行: %s", error.getRow(), error.getMessage()));
                }
            }
            result.setWarnings(warnings);
            
            return result;
            
        } catch (Exception e) {
            log.error("预览数据失败", e);
            throw new BusinessException(ErrorCode.IMPORT_PARSE_ERROR, "预览数据失败: " + e.getMessage());
        }
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public SalesImportResult executeImport(SalesImportRequest request) {
        log.info("执行导入: fileId={}, sourceType={}, siteCode={}, templateId={}", 
                request.getFileId(), request.getSourceType(), request.getSiteCode(), request.getTemplateId());
        
        // 获取缓存的文件
        UploadedFileCache fileCache = uploadedFileCache.get(request.getFileId());
        if (fileCache == null) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND, "文件已过期，请重新上传");
        }
        
        // 获取映射模板
        FieldMappingTemplate template = fieldMappingTemplateMapper.selectById(request.getTemplateId());
        if (template == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST, "模板不存在");
        }
        
        // 获取当前店铺ID
        Long shopId = ShopContext.requireShopId();
        
        // 生成批次号
        String batchNo = generateBatchNo();
        
        // 创建导入记录
        ImportRecord importRecord = new ImportRecord();
        importRecord.setShopId(shopId);
        importRecord.setBatchNo(batchNo);
        importRecord.setDataType("sales");
        importRecord.setFileName(fileCache.fileName);
        importRecord.setFileSize(fileCache.fileSize);
        importRecord.setImportStatus("processing");
        importRecordMapper.insert(importRecord);
        
        // 初始化进度
        SalesImportProgress progress = new SalesImportProgress();
        progress.setBatchNo(batchNo);
        progress.setStatus("PROCESSING");
        progress.setTotalCount(0);
        progress.setSuccessCount(0);
        progress.setFailCount(0);
        progress.setSkipCount(0);
        progress.setProgress(0);
        importProgressCache.put(batchNo, progress);
        
        int totalCount = 0;
        int successCount = 0;
        int failCount = 0;
        int skipCount = 0;
        List<String> errors = new ArrayList<>();
        
        try {
            // 解析映射配置
            Map<String, String> mappingConfig = parseMappingConfig(template.getMappingConfig());
            
            // 获取交易类型映射
            Map<String, String> transactionTypeMapping = getTransactionTypeMapping(request.getSiteCode());
            
            // 构建解析上下文
            ParseContext context = ParseContext.builder()
                    .sourceType(request.getSourceType())
                    .siteCode(request.getSiteCode())
                    .templateId(request.getTemplateId())
                    .fieldMapping(mappingConfig)
                    .transactionTypeMapping(transactionTypeMapping)
                    .quarter(request.getQuarter())
                    .build();
            
            // 获取解析器
            SalesDataParser parser = parserFactory.getParser(request.getSourceType());
            
            // 解析全部数据
            String fileContent = new String(fileCache.content, StandardCharsets.UTF_8);
            ParseResult parseResult = parser.parse(fileContent, context, Integer.MAX_VALUE);
            
            totalCount = parseResult.getDataList().size();
            progress.setTotalCount(totalCount);
            
            List<SalesData> dataList = parseResult.getDataList();

            // 批量设置额外字段和汇率
            for (SalesData data : dataList) {
                data.setShopId(shopId);
                data.setImportBatchId(importRecord.getId());
                data.setSourceType(request.getSourceType().getCode());
                fillExchangeRate(data);
            }
            
            // 批量检查重复
            boolean isErpData = SalesSourceType.ERP.equals(request.getSourceType());
            Set<String> existingOrderKeys = batchCheckDuplicates(dataList, isErpData);
            
            // 分批处理数据
            List<SalesData> batchToInsert = new ArrayList<>();
            int batchSize = 500;
            
            for (int i = 0; i < dataList.size(); i++) {
                SalesData data = dataList.get(i);
                String orderKey = buildOrderKey(data, isErpData);
                
                // 检查重复
                if (existingOrderKeys.contains(orderKey)) {
                    if (Boolean.TRUE.equals(request.getSkipDuplicate())) {
                        skipCount++;
                        continue;
                    } else if (Boolean.TRUE.equals(request.getOverwriteDuplicate())) {
                        deleteExistingData(data, isErpData);
                    } else {
                        failCount++;
                        if (errors.size() < 100) {
                            String keyInfo = isErpData 
                                    ? String.format("结算编号%s+订单%s+来源%s", data.getSettlementId(), data.getOrderId(), data.getTransactionType())
                                    : String.format("订单%s", data.getOrderId());
                            errors.add(String.format("第%d行: %s已存在", i + 1, keyInfo));
                        }
                        continue;
                    }
                }
                
                batchToInsert.add(data);
                
                // 达到批量大小，执行批量插入
                if (batchToInsert.size() >= batchSize) {
                    int inserted = executeBatchInsert(batchToInsert, errors, i - batchToInsert.size() + 2);
                    successCount += inserted;
                    failCount += batchToInsert.size() - inserted;
                    batchToInsert.clear();
                }
                
                // 更新进度
                if (i % 500 == 0) {
                    progress.setSuccessCount(successCount);
                    progress.setFailCount(failCount);
                    progress.setSkipCount(skipCount);
                    progress.setProgress((i + 1) * 100 / totalCount);
                }
            }
            
            // 处理剩余数据
            if (!batchToInsert.isEmpty()) {
                int inserted = executeBatchInsert(batchToInsert, errors, totalCount - batchToInsert.size() + 1);
                successCount += inserted;
                failCount += batchToInsert.size() - inserted;
            }
            
            // 添加解析错误
            if (parseResult.getErrors() != null && !parseResult.getErrors().isEmpty()) {
                for (ParseResult.ParseError parseError : parseResult.getErrors()) {
                    errors.add(String.format("第%d行: %s", parseError.getRow(), parseError.getMessage()));
                }
                failCount += parseResult.getErrors().size();
            }
            
            // 更新导入记录
            importRecord.setTotalCount(totalCount);
            importRecord.setSuccessCount(successCount);
            importRecord.setFailCount(failCount);
            String finalStatus = failCount == 0 ? "success" : (successCount > 0 ? "partial" : "fail");
            importRecord.setImportStatus(finalStatus);
            importRecord.setCompleteTime(LocalDateTime.now());
            if (!errors.isEmpty()) {
                importRecord.setErrorMessage(String.join("\n", errors.subList(0, Math.min(20, errors.size()))));
            }
            importRecordMapper.updateById(importRecord);
            
            // 更新最终进度
            progress.setStatus(finalStatus.equals("success") ? "SUCCESS" : 
                              (finalStatus.equals("partial") ? "PARTIAL" : "FAIL"));
            progress.setSuccessCount(successCount);
            progress.setFailCount(failCount);
            progress.setSkipCount(skipCount);
            progress.setProgress(100);
            
            // 清理文件缓存
            uploadedFileCache.remove(request.getFileId());
            
        } catch (Exception e) {
            log.error("导入失败", e);
            importRecord.setImportStatus("fail");
            importRecord.setErrorMessage(e.getMessage());
            importRecordMapper.updateById(importRecord);
            
            progress.setStatus("FAIL");
            progress.setCurrentError(e.getMessage());
            
            throw new BusinessException(ErrorCode.IMPORT_FAILED, "导入失败: " + e.getMessage());
        }
        
        // 构建返回结果
        SalesImportResult result = new SalesImportResult();
        result.setBatchNo(batchNo);
        result.setStatus(progress.getStatus());
        result.setTotalCount(totalCount);
        result.setSuccessCount(successCount);
        result.setFailCount(failCount);
        result.setSkipCount(skipCount);
        result.setAsync(false);
        
        if (!errors.isEmpty()) {
            result.setErrors(errors.subList(0, Math.min(10, errors.size())));
        }
        
        log.info("导入完成: batchNo={}, total={}, success={}, fail={}, skip={}", 
                batchNo, totalCount, successCount, failCount, skipCount);
        
        return result;
    }


    @Override
    public SalesImportProgress getImportProgress(String batchNo) {
        SalesImportProgress progress = importProgressCache.get(batchNo);
        if (progress != null) {
            return progress;
        }
        
        // 如果缓存中没有，从数据库查询
        LambdaQueryWrapper<ImportRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ImportRecord::getBatchNo, batchNo);
        ImportRecord record = importRecordMapper.selectOne(wrapper);
        
        if (record == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST, "导入记录不存在");
        }
        
        progress = new SalesImportProgress();
        progress.setBatchNo(batchNo);
        progress.setStatus(mapImportStatus(record.getImportStatus()));
        progress.setTotalCount(record.getTotalCount() != null ? record.getTotalCount() : 0);
        progress.setSuccessCount(record.getSuccessCount() != null ? record.getSuccessCount() : 0);
        progress.setFailCount(record.getFailCount() != null ? record.getFailCount() : 0);
        progress.setSkipCount(0);
        progress.setProgress(100);
        
        return progress;
    }
    
    // ========== 私有辅助方法 ==========
    
    /**
     * 解析单条 CSV 记录为 SalesData 实体
     */
    private SalesData parseSalesRecord(CSVRecord record, List<String> headers,
                                       Map<String, String> fieldMapping, Map<String, String> transactionTypeMapping,
                                       String siteCode, Marketplace marketplace, int rowNum) {

        Map<String, String> rowData = new HashMap<>();
        for (int i = 0; i < Math.min(headers.size(), record.size()); i++) {
            rowData.put(headers.get(i).toLowerCase().trim(), record.get(i).trim());
        }

        SalesData salesData = new SalesData();
        salesData.setSiteCode(siteCode);
        salesData.setMarketplace(marketplace.getMarketplaceId());
        salesData.setCurrencyCode(marketplace.getCurrencyCode());

        // Map fields using field mapping configuration
        String orderId = getMappedValue(rowData, fieldMapping, "order_id");
        if (!StringUtils.hasText(orderId)) {
            return null;
        }
        salesData.setOrderId(orderId);

        // Parse transaction type and map to category
        String transactionType = getMappedValue(rowData, fieldMapping, "transaction_type");
        salesData.setTransactionType(transactionType);

        // Map transaction type to standard category
        String category = transactionTypeMapping.getOrDefault(transactionType, "other");
        salesData.setTransactionCategory(category);

        // Parse date
        String dateStr = getMappedValue(rowData, fieldMapping, "date_time");
        if (StringUtils.hasText(dateStr)) {
            LocalDateTime dateTime = csvParseService.parseDate(dateStr, siteCode);
            if (dateTime != null) {
                salesData.setTransactionDate(dateTime);
            }
        }

        // Parse other fields
        salesData.setSettlementId(getMappedValue(rowData, fieldMapping, "settlement_id"));
        salesData.setSku(getMappedValue(rowData, fieldMapping, "sku"));
        salesData.setDescription(getMappedValue(rowData, fieldMapping, "description"));
        salesData.setFulfillment(getMappedValue(rowData, fieldMapping, "fulfillment"));

        // Parse quantity
        String quantityStr = getMappedValue(rowData, fieldMapping, "quantity");
        if (StringUtils.hasText(quantityStr)) {
            try {
                salesData.setQuantity(Integer.parseInt(quantityStr.replace(",", "")));
            } catch (NumberFormatException e) {
                salesData.setQuantity(0);
            }
        }

        // Parse amount fields
        salesData.setProductSales(parseDecimalField(rowData, fieldMapping, "product_sales", siteCode));
        salesData.setProductSalesTax(parseDecimalField(rowData, fieldMapping, "product_sales_tax", siteCode));
        salesData.setShippingCredits(parseDecimalField(rowData, fieldMapping, "shipping_credits", siteCode));
        salesData.setShippingCreditsTax(parseDecimalField(rowData, fieldMapping, "shipping_credits_tax", siteCode));
        salesData.setGiftWrapCredits(parseDecimalField(rowData, fieldMapping, "gift_wrap_credits", siteCode));
        salesData.setGiftWrapCreditsTax(parseDecimalField(rowData, fieldMapping, "gift_wrap_credits_tax", siteCode));
        salesData.setPromotionalRebates(parseDecimalField(rowData, fieldMapping, "promotional_rebates", siteCode));
        salesData.setPromotionalRebatesTax(parseDecimalField(rowData, fieldMapping, "promotional_rebates_tax", siteCode));
        salesData.setMarketplaceWithheldTax(parseDecimalField(rowData, fieldMapping, "marketplace_withheld_tax", siteCode));
        salesData.setSellingFees(parseDecimalField(rowData, fieldMapping, "selling_fees", siteCode));
        salesData.setFbaFees(parseDecimalField(rowData, fieldMapping, "fba_fees", siteCode));
        salesData.setOtherTransactionFees(parseDecimalField(rowData, fieldMapping, "other_transaction_fees", siteCode));
        salesData.setOther(parseDecimalField(rowData, fieldMapping, "other", siteCode));
        salesData.setTotal(parseDecimalField(rowData, fieldMapping, "total", siteCode));

        return salesData;
    }

    /**
     * 解析金额字段
     */
    private BigDecimal parseDecimalField(Map<String, String> rowData, Map<String, String> fieldMapping, 
                                         String targetField, String siteCode) {
        String value = getMappedValue(rowData, fieldMapping, targetField);
        if (StringUtils.hasText(value)) {
            return csvParseService.parseDecimal(value, siteCode);
        }
        return BigDecimal.ZERO;
    }

    /**
     * 获取映射值
     */
    private String getMappedValue(Map<String, String> rowData, Map<String, String> fieldMapping, String targetField) {
        String sourceField = fieldMapping.get(targetField);
        if (sourceField != null) {
            return rowData.getOrDefault(sourceField.toLowerCase(), "");
        }

        // Try direct match with common field names
        String[] commonNames = getCommonFieldNames(targetField);
        for (String name : commonNames) {
            String value = rowData.get(name.toLowerCase());
            if (value != null) {
                return value;
            }
        }
        return "";
    }

    /**
     * 获取通用字段名
     */
    private String[] getCommonFieldNames(String targetField) {
        return switch (targetField) {
            case "order_id" -> new String[]{"order id", "order-id", "orderid", "bestellnummer"};
            case "date_time" -> new String[]{"date/time", "date", "datetime", "datum/uhrzeit"};
            case "transaction_type" -> new String[]{"type", "transaction type", "typ"};
            case "settlement_id" -> new String[]{"settlement id", "settlementid", "abrechnungsnummer"};
            case "sku" -> new String[]{"sku", "asin"};
            case "description" -> new String[]{"description", "product name", "beschreibung"};
            case "quantity" -> new String[]{"quantity", "qty", "menge"};
            case "total" -> new String[]{"total", "sum", "gesamt"};
            case "product_sales" -> new String[]{"product sales", "productsales"};
            case "shipping_credits" -> new String[]{"shipping credits", "shippingcredits"};
            default -> new String[]{targetField};
        };
    }


    /**
     * 获取字段映射配置
     */
    private Map<String, String> getFieldMapping(String siteCode, String dataType) {
        LambdaQueryWrapper<FieldMappingTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FieldMappingTemplate::getSiteCode, siteCode)
                .eq(FieldMappingTemplate::getDataType, dataType)
                .eq(FieldMappingTemplate::getIsDefault, 1);

        FieldMappingTemplate template = fieldMappingTemplateMapper.selectOne(wrapper);

        if (template != null && template.getMappingConfig() != null) {
            try {
                return objectMapper.readValue(template.getMappingConfig(),
                        new TypeReference<Map<String, String>>() {});
            } catch (Exception e) {
                log.warn("Failed to parse field mapping config", e);
            }
        }

        return new HashMap<>();
    }

    /**
     * 获取交易类型映射
     */
    private Map<String, String> getTransactionTypeMapping(String siteCode) {
        Map<String, String> mapping = new HashMap<>();

        // Get site-specific mappings
        LambdaQueryWrapper<TransactionTypeMapping> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TransactionTypeMapping::getSiteCode, siteCode)
                .eq(TransactionTypeMapping::getStatus, 1);
        List<TransactionTypeMapping> siteList = transactionTypeMappingMapper.selectList(wrapper);

        for (TransactionTypeMapping m : siteList) {
            mapping.put(m.getOriginalType(), m.getStandardCategory());
        }

        // Get universal mappings
        wrapper = new LambdaQueryWrapper<>();
        wrapper.isNull(TransactionTypeMapping::getSiteCode)
                .eq(TransactionTypeMapping::getStatus, 1);
        List<TransactionTypeMapping> universalList = transactionTypeMappingMapper.selectList(wrapper);

        for (TransactionTypeMapping m : universalList) {
            mapping.putIfAbsent(m.getOriginalType(), m.getStandardCategory());
        }

        return mapping;
    }

    /**
     * 检查是否重复
     * 
     * ⚠️ 使用 Repository 替代直接使用 Mapper
     */
    private boolean isDuplicate(SalesData salesData) {
        return salesDataRepository.existsByOrderIdAndCategory(
                salesData.getOrderId(), 
                salesData.getTransactionCategory());
    }

    /**
     * 生成批次号
     */
    private String generateBatchNo() {
        return "SALES-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 填充汇率信息
     */
    private void fillExchangeRate(SalesData data) {
        if (data.getTransactionDate() == null || !StringUtils.hasText(data.getCurrencyCode())) {
            log.debug("Skipping exchange rate fill: transactionDate={}, currencyCode={}",
                    data.getTransactionDate(), data.getCurrencyCode());
            return;
        }

        // 人民币不需要汇率转换
        if ("CNY".equalsIgnoreCase(data.getCurrencyCode())) {
            data.setExchangeRate(BigDecimal.ONE);
            data.setExchangeRateDate(data.getTransactionDate().toLocalDate());
            return;
        }

        try {
            java.time.LocalDate transactionDate = data.getTransactionDate().toLocalDate();
            BigDecimal rate = rateService.getRate(data.getCurrencyCode(), transactionDate.toString());
            data.setExchangeRate(rate);
            data.setExchangeRateDate(transactionDate);

            log.debug("Exchange rate filled: currency={}, date={}, rate={}",
                    data.getCurrencyCode(), transactionDate, rate);
        } catch (Exception e) {
            log.warn("Failed to get exchange rate for currency={}, date={}: {}",
                    data.getCurrencyCode(), data.getTransactionDate(), e.getMessage());
            data.setExchangeRate(null);
            data.setExchangeRateDate(null);
        }
    }

    /**
     * 解析 CSV 行
     */
    private List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        if (line == null || line.isEmpty()) {
            return fields;
        }
        
        boolean inQuotes = false;
        StringBuilder field = new StringBuilder();
        
        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(field.toString().trim());
                field = new StringBuilder();
            } else {
                field.append(c);
            }
        }
        fields.add(field.toString().trim());
        
        return fields;
    }

    /**
     * 清理过期缓存
     */
    private void cleanExpiredCache() {
        long now = System.currentTimeMillis();
        long expireTime = 60 * 60 * 1000; // 1小时
        
        uploadedFileCache.entrySet().removeIf(entry -> 
            now - entry.getValue().uploadTime > expireTime);
    }

    /**
     * 解析映射配置
     */
    private Map<String, String> parseMappingConfig(String mappingConfigJson) {
        Map<String, String> result = new HashMap<>();
        if (mappingConfigJson == null || mappingConfigJson.isBlank()) {
            return result;
        }
        
        try {
            List<Map<String, String>> mappingList = objectMapper.readValue(
                    mappingConfigJson,
                    new TypeReference<List<Map<String, String>>>() {});
            
            for (Map<String, String> item : mappingList) {
                String source = item.get("source");
                String target = item.get("target");
                if (source != null && target != null) {
                    result.put(source, target);
                }
            }
        } catch (Exception e) {
            log.warn("解析映射配置失败: {}", e.getMessage());
        }
        
        return result;
    }


    /**
     * 构建列元信息
     */
    private List<ColumnMeta> buildColumnMeta(Map<String, String> mappingConfig) {
        List<ColumnMeta> columns = new ArrayList<>();
        
        Set<String> mappedTargets = new HashSet<>(mappingConfig.values());
        
        String[][] standardColumns = {
            {"orderId", "订单号"},
            {"siteCode", "站点"},
            {"transactionDate", "交易日期"},
            {"transactionType", "交易类型"},
            {"transactionCategory", "交易分类"},
            {"sku", "SKU"},
            {"quantity", "数量"},
            {"productSales", "产品销售"},
            {"sellingFees", "销售费用"},
            {"fbaFees", "FBA费用"},
            {"total", "合计"},
            {"currencyCode", "货币"}
        };
        
        for (String[] col : standardColumns) {
            ColumnMeta meta = new ColumnMeta();
            meta.setField(col[0]);
            meta.setLabel(col[1]);
            meta.setMapped(mappedTargets.contains(col[0]));
            columns.add(meta);
        }
        
        return columns;
    }

    /**
     * SalesData 转 Map
     */
    private Map<String, Object> salesDataToMap(SalesData data) {
        Map<String, Object> map = new HashMap<>();
        map.put("orderId", data.getOrderId());
        map.put("siteCode", data.getSiteCode());
        map.put("transactionDate", data.getTransactionDate() != null ? 
                data.getTransactionDate().toString() : null);
        map.put("transactionType", data.getTransactionType());
        map.put("transactionCategory", data.getTransactionCategory());
        map.put("sku", data.getSku());
        map.put("quantity", data.getQuantity());
        map.put("productSales", data.getProductSales());
        map.put("sellingFees", data.getSellingFees());
        map.put("fbaFees", data.getFbaFees());
        map.put("total", data.getTotal());
        map.put("currencyCode", data.getCurrencyCode());
        return map;
    }

    /**
     * 获取必填字段列表
     */
    private List<String> getRequiredFields(SalesSourceType sourceType) {
        if (sourceType == SalesSourceType.ERP) {
            return Arrays.asList("orderId");
        }
        return Arrays.asList("orderId", "transactionDate");
    }

    /**
     * 获取可选字段列表
     */
    private List<String> getOptionalFields(SalesSourceType sourceType) {
        if (sourceType == SalesSourceType.ERP) {
            return Arrays.asList("sku", "quantity", "settlementId");
        }
        return Arrays.asList("sku", "quantity", "transactionType", "productSales", "sellingFees", 
                "fbaFees", "shippingCredits", "promotionalRebates", "other", "total");
    }

    /**
     * 删除已存在的数据（用于覆盖导入）
     */
    private void deleteExistingData(SalesData data, boolean isErpData) {
        LambdaQueryWrapper<SalesData> wrapper = new LambdaQueryWrapper<>();
        
        // settlementId
        if (data.getSettlementId() != null && !data.getSettlementId().isEmpty()) {
            wrapper.eq(SalesData::getSettlementId, data.getSettlementId());
        } else {
            wrapper.and(w -> w.isNull(SalesData::getSettlementId).or().eq(SalesData::getSettlementId, ""));
        }
        
        // orderId
        if (data.getOrderId() != null && !data.getOrderId().isEmpty()) {
            wrapper.eq(SalesData::getOrderId, data.getOrderId());
        } else {
            wrapper.and(w -> w.isNull(SalesData::getOrderId).or().eq(SalesData::getOrderId, ""));
        }
        
        // transactionType
        if (data.getTransactionType() != null && !data.getTransactionType().isEmpty()) {
            wrapper.eq(SalesData::getTransactionType, data.getTransactionType());
        } else {
            wrapper.and(w -> w.isNull(SalesData::getTransactionType).or().eq(SalesData::getTransactionType, ""));
        }
        
        // sku
        if (data.getSku() != null && !data.getSku().isEmpty()) {
            wrapper.eq(SalesData::getSku, data.getSku());
        } else {
            wrapper.and(w -> w.isNull(SalesData::getSku).or().eq(SalesData::getSku, ""));
        }
        
        salesDataMapper.delete(wrapper);
    }

    /**
     * 批量检查重复数据
     */
    private Set<String> batchCheckDuplicates(List<SalesData> dataList, boolean isErpData) {
        Set<String> existingKeys = new HashSet<>();
        
        if (dataList == null || dataList.isEmpty()) {
            return existingKeys;
        }
        
        Set<String> settlementIds = dataList.stream()
                .map(SalesData::getSettlementId)
                .filter(id -> id != null && !id.isEmpty())
                .collect(Collectors.toSet());
        
        if (settlementIds.isEmpty()) {
            return existingKeys;
        }
        
        List<String> settlementIdList = new ArrayList<>(settlementIds);
        int batchSize = 500;
        
        for (int i = 0; i < settlementIdList.size(); i += batchSize) {
            int end = Math.min(i + batchSize, settlementIdList.size());
            List<String> batch = settlementIdList.subList(i, end);
            
            LambdaQueryWrapper<SalesData> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(SalesData::getSettlementId, batch)
                    .select(SalesData::getSettlementId, SalesData::getOrderId, 
                            SalesData::getTransactionType, SalesData::getSku);
            
            List<SalesData> existingData = salesDataMapper.selectList(wrapper);
            
            for (SalesData data : existingData) {
                existingKeys.add(buildOrderKey(data, isErpData));
            }
        }
        
        return existingKeys;
    }

    /**
     * 构建订单唯一 key
     */
    private String buildOrderKey(SalesData data, boolean isErpData) {
        return String.format("%s|%s|%s|%s", 
                data.getSettlementId() != null ? data.getSettlementId() : "",
                data.getOrderId() != null ? data.getOrderId() : "",
                data.getTransactionType() != null ? data.getTransactionType() : "",
                data.getSku() != null ? data.getSku() : "");
    }

    /**
     * 执行批量插入
     */
    private int executeBatchInsert(List<SalesData> batchList, List<String> errors, int startRow) {
        if (batchList == null || batchList.isEmpty()) {
            return 0;
        }
        
        try {
            try (SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH, false)) {
                SalesDataMapper batchMapper = sqlSession.getMapper(SalesDataMapper.class);
                
                for (SalesData data : batchList) {
                    batchMapper.insert(data);
                }
                
                sqlSession.flushStatements();
                sqlSession.commit();
            }
            
            log.info("Batch insert {} records successfully", batchList.size());
            return batchList.size();
            
        } catch (Exception e) {
            log.error("Batch insert failed, falling back to single insert: {}", e.getMessage());
            
            int successCount = 0;
            for (int i = 0; i < batchList.size(); i++) {
                try {
                    salesDataMapper.insert(batchList.get(i));
                    successCount++;
                } catch (Exception ex) {
                    if (errors.size() < 100) {
                        errors.add(String.format("第%d行: %s", startRow + i, ex.getMessage()));
                    }
                }
            }
            return successCount;
        }
    }

    /**
     * 映射导入状态
     */
    private String mapImportStatus(String dbStatus) {
        if (dbStatus == null) return "PENDING";
        return switch (dbStatus) {
            case "processing" -> "PROCESSING";
            case "success" -> "SUCCESS";
            case "partial" -> "PARTIAL";
            case "fail" -> "FAIL";
            default -> "PENDING";
        };
    }
}
