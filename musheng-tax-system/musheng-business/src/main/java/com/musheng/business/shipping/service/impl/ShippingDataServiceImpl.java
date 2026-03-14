package com.musheng.business.shipping.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.business.common.service.csv.CsvHeaderResult;
import com.musheng.business.common.service.csv.CsvParseServiceImpl;
import com.musheng.business.sales.entity.SalesData;
import com.musheng.business.sales.mapper.SalesDataMapper;
import com.musheng.business.shipping.entity.ShippingData;
import com.musheng.business.shipping.mapper.ShippingDataMapper;
import com.musheng.business.shipping.service.ShippingDataService;
import com.musheng.common.exception.BusinessException;
import com.musheng.common.result.ErrorCode;
import com.musheng.config.importrecord.entity.ImportRecord;
import com.musheng.config.importrecord.mapper.ImportRecordMapper;
import com.musheng.config.marketplace.entity.Marketplace;
import com.musheng.config.marketplace.mapper.MarketplaceMapper;
import com.musheng.config.marketplace.dto.MarketplaceRequest;
import com.musheng.config.marketplace.service.MarketplaceService;
import com.musheng.business.common.config.ImportConfig;
import com.musheng.business.rate.service.RateService;
import com.musheng.common.context.ShopContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Shipping Data Service Implementation
 * Implements CSV import with duplicate check (BUG-007)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShippingDataServiceImpl implements ShippingDataService {

    private final ShippingDataMapper shippingDataMapper;
    private final MarketplaceMapper marketplaceMapper;
    private final ImportRecordMapper importRecordMapper;
    private final CsvParseServiceImpl csvParseService;
    private final RateService rateService;
    private final org.apache.ibatis.session.SqlSessionFactory sqlSessionFactory;
    private final ImportConfig importConfig;
    private final MarketplaceService marketplaceService;
    private final SalesDataMapper salesDataMapper;

    @Override
    public Page<ShippingData> list(String siteCode, String trackingNumber, String orderId,
                                   String startDate, String endDate, int page, int size) {
        LambdaQueryWrapper<ShippingData> wrapper = new LambdaQueryWrapper<>();

        // 店铺数据隔离
        Long shopId = ShopContext.requireShopId();
        wrapper.eq(ShippingData::getShopId, shopId);

        if (StringUtils.hasText(siteCode)) {
            wrapper.eq(ShippingData::getSiteCode, siteCode);
        }
        if (StringUtils.hasText(trackingNumber)) {
            wrapper.like(ShippingData::getTrackingNumber, trackingNumber);
        }
        if (StringUtils.hasText(orderId)) {
            wrapper.like(ShippingData::getOrderId, orderId);
        }
        // 按发货日期过滤
        if (StringUtils.hasText(startDate)) {
            try {
                java.time.LocalDate start = java.time.LocalDate.parse(startDate);
                wrapper.ge(ShippingData::getShipDate, start);
            } catch (Exception e) {
                log.warn("Invalid start date format: {}", startDate);
            }
        }
        if (StringUtils.hasText(endDate)) {
            try {
                java.time.LocalDate end = java.time.LocalDate.parse(endDate);
                wrapper.le(ShippingData::getShipDate, end);
            } catch (Exception e) {
                log.warn("Invalid end date format: {}", endDate);
            }
        }

        wrapper.orderByDesc(ShippingData::getShipDate);

        return shippingDataMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public ShippingData getById(Long id) {
        ShippingData entity = shippingDataMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST, "Shipping data not found");
        }
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importData(MultipartFile file) {
        log.info("Importing shipping data: fileName={}, size={} bytes",
                file.getOriginalFilename(), file.getSize());

        Map<String, Object> result = new HashMap<>();
        List<String> errors = new ArrayList<>();
        int totalCount = 0;
        int successCount = 0;
        int failCount = 0;
        int duplicateCount = 0;
        int skipCount = 0;  // 空行等静默跳过的行数

        // 获取当前店铺ID
        Long shopId = ShopContext.requireShopId();
        
        // Create import record
        ImportRecord importRecord = new ImportRecord();
        importRecord.setShopId(shopId);  // 设置店铺ID
        importRecord.setBatchNo(generateBatchNo());
        importRecord.setDataType("shipping");
        importRecord.setFileName(file.getOriginalFilename());
        importRecord.setFileSize(file.getSize());
        importRecord.setImportStatus("processing");
        importRecordMapper.insert(importRecord);

        try {
            // Detect headers and charset
            log.info("Detecting CSV headers...");
            CsvHeaderResult headerResult = csvParseService.parseHeaders(file);
            log.info("Headers detected: language={}, charset={}, headerRowIndex={}, columns={}",
                    headerResult.getHeaderLanguage(),
                    headerResult.getCharset(),
                    headerResult.getHeaderRowIndex(),
                    headerResult.getHeaders().size());

            List<String> headers = headerResult.getHeaders();
            int headerRowIndex = headerResult.getHeaderRowIndex();
            String charsetName = headerResult.getCharset();
            String headerLanguage = headerResult.getHeaderLanguage(); // 用于决定数字格式
            java.nio.charset.Charset charset = charsetName != null ?
                    java.nio.charset.Charset.forName(charsetName) : StandardCharsets.UTF_8;

            // Step 1: Parse all records into a list (避免N+1查询)
            List<ShippingData> parsedRecords = new ArrayList<>();
            Map<Integer, String> rowErrorMap = new HashMap<>(); // Track errors by row number

            // Pre-load all marketplace configurations to avoid N+1 queries
            List<Marketplace> allMarketplaces = marketplaceMapper.selectList(null);
            Map<String, Marketplace> marketplaceMap = allMarketplaces.stream()
                    .collect(java.util.stream.Collectors.toMap(Marketplace::getSiteCode, m -> m));
            log.info("Pre-loaded {} marketplace configurations", marketplaceMap.size());

            // Reset input stream and skip to data rows using detected charset
            try (BufferedReader dataReader = new BufferedReader(
                    new InputStreamReader(file.getInputStream(), charset))) {

                // Skip header rows
                for (int i = 0; i <= headerRowIndex; i++) {
                    dataReader.readLine();
                }

                CSVParser parser = CSVFormat.DEFAULT.parse(dataReader);

                for (CSVRecord record : parser) {
                    totalCount++;
                    try {
                        ShippingData shippingData = parseShippingRecord(record, headers, totalCount, marketplaceMap, headerLanguage);

                        if (shippingData != null) {
                            shippingData.setShopId(shopId);  // 设置店铺ID
                            shippingData.setImportBatchId(importRecord.getId());
                            parsedRecords.add(shippingData);
                        } else {
                            skipCount++;  // 空行（无订单号）静默跳过
                        }
                    } catch (Exception e) {
                        failCount++;
                        rowErrorMap.put(totalCount, e.getMessage());
                        log.warn("Failed to parse row {}: {}", totalCount, e.getMessage());
                    }
                }
            }

            log.info("Parsed {} valid records from {} total rows", parsedRecords.size(), totalCount);

            // Step 1.5: 关联 MCF（Non-Amazon）订单的站点编码
            // 找出所有 siteCode=PENDING 的记录，批量查询销售订单表，用销售订单的站点回填
            List<ShippingData> pendingRecords = parsedRecords.stream()
                    .filter(r -> "PENDING".equals(r.getSiteCode()))
                    .toList();

            if (!pendingRecords.isEmpty()) {
                log.info("发现 {} 条 MCF 订单（Non-Amazon），开始关联销售订单站点...", pendingRecords.size());
                resolveMcfSiteCodes(pendingRecords, shopId, rowErrorMap);
                // 移除仍然是 PENDING 的记录（未找到对应销售订单，已记录错误）
                int pendingBefore = (int) parsedRecords.stream().filter(r -> "PENDING".equals(r.getSiteCode())).count();
                parsedRecords.removeIf(r -> "PENDING".equals(r.getSiteCode()));
                failCount += pendingBefore;
                log.info("MCF 订单关联完成：成功关联 {} 条，未找到销售订单 {} 条",
                        pendingRecords.size() - pendingBefore, pendingBefore);
            }

            // Step 2: Batch check for duplicates (单次查询避免N+1)
            List<ShippingData> toInsert = new ArrayList<>();
            List<ShippingData> duplicates = new ArrayList<>();

            if (!parsedRecords.isEmpty()) {
                // Build batch duplicate check query
                Set<String> existingKeys = batchCheckDuplicates(parsedRecords);

                for (ShippingData data : parsedRecords) {
                    String uniqueKey = buildUniqueKey(data);
                    if (existingKeys.contains(uniqueKey)) {
                        duplicates.add(data);
                        duplicateCount++;
                    } else {
                        toInsert.add(data);
                    }
                }
            }

            log.info("Duplicate check completed: {} to insert, {} duplicates", toInsert.size(), duplicates.size());

            // Step 3: 预热汇率缓存 + 填充汇率
            preloadExchangeRateCache(toInsert);
            for (ShippingData data : toInsert) {
                fillExchangeRate(data);
            }
            log.info("Exchange rates filled for {} records", toInsert.size());

            // Step 4: Batch insert (使用 SqlSession BATCH 模式)
            if (!toInsert.isEmpty()) {
                int batchSize = 500; // 每批500条
                for (int i = 0; i < toInsert.size(); i += batchSize) {
                    int end = Math.min(i + batchSize, toInsert.size());
                    List<ShippingData> batch = toInsert.subList(i, end);

                    // 使用 SqlSession BATCH 模式进行真正的批量插入
                    try (org.apache.ibatis.session.SqlSession sqlSession = sqlSessionFactory.openSession(
                            org.apache.ibatis.session.ExecutorType.BATCH, false)) {
                        ShippingDataMapper batchMapper = sqlSession.getMapper(ShippingDataMapper.class);
                        for (ShippingData data : batch) {
                            batchMapper.insert(data);
                        }
                        sqlSession.flushStatements();
                        sqlSession.commit();
                    }
                    successCount += batch.size();

                    log.info("Batch inserted records {}-{}/{}", i + 1, end, toInsert.size());
                }
            }

            // Add duplicate errors to error list
            for (ShippingData dup : duplicates) {
                if (errors.size() < 10) { // Limit error messages
                    errors.add(String.format("Duplicate: order=%s, site=%s, tracking=%s",
                            dup.getOrderId(), dup.getSiteCode(), dup.getTrackingNumber()));
                }
            }

            // Add parsing errors to error list
            for (Map.Entry<Integer, String> entry : rowErrorMap.entrySet()) {
                if (errors.size() < 10) {
                    errors.add(String.format("Row %d: %s", entry.getKey(), entry.getValue()));
                }
            }

            // Update import record
            importRecord.setTotalCount(totalCount);
            importRecord.setSuccessCount(successCount);
            importRecord.setFailCount(failCount);
            importRecord.setImportStatus(failCount == 0 && duplicateCount == 0 ? "success" :
                    (successCount > 0 ? "partial" : "fail"));
            importRecord.setCompleteTime(LocalDateTime.now());
            if (!errors.isEmpty()) {
                importRecord.setErrorMessage(String.join("\n", errors));
            }
            importRecordMapper.updateById(importRecord);

        } catch (BusinessException e) {
            importRecord.setImportStatus("fail");
            importRecord.setErrorMessage(e.getMessage());
            importRecordMapper.updateById(importRecord);
            throw e;
        } catch (Exception e) {
            log.error("Failed to import shipping data", e);
            importRecord.setImportStatus("fail");
            importRecord.setErrorMessage(e.getMessage());
            importRecordMapper.updateById(importRecord);
            throw new BusinessException(ErrorCode.IMPORT_PARSE_ERROR, "Failed to parse file: " + e.getMessage());
        }

        result.put("totalCount", totalCount);
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("duplicateCount", duplicateCount);
        result.put("skipCount", skipCount);
        result.put("errors", errors);
        result.put("batchNo", importRecord.getBatchNo());

        log.info("Shipping data import completed: total={}, success={}, fail={}, duplicate={}, skip={}",
                totalCount, successCount, failCount, duplicateCount, skipCount);

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> batchImportData(List<MultipartFile> files) {
        log.info("Batch importing shipping data: fileCount={}", files.size());

        Map<String, Object> batchResult = new HashMap<>();
        List<Map<String, Object>> fileResults = new ArrayList<>();

        int totalFiles = files.size();
        int successFiles = 0;
        int failFiles = 0;
        int totalCount = 0;
        int successCount = 0;
        int failCount = 0;
        int duplicateCount = 0;
        int skipCount = 0;

        for (MultipartFile file : files) {
            Map<String, Object> fileResult = new HashMap<>();
            fileResult.put("fileName", file.getOriginalFilename());

            try {
                Map<String, Object> importResult = importData(file);
                fileResult.put("status", "success");
                fileResult.put("result", importResult);

                totalCount += (Integer) importResult.getOrDefault("totalCount", 0);
                successCount += (Integer) importResult.getOrDefault("successCount", 0);
                failCount += (Integer) importResult.getOrDefault("failCount", 0);
                duplicateCount += (Integer) importResult.getOrDefault("duplicateCount", 0);
                skipCount += (Integer) importResult.getOrDefault("skipCount", 0);
                successFiles++;
            } catch (Exception e) {
                log.error("Import file failed: {}", file.getOriginalFilename(), e);
                fileResult.put("status", "fail");
                fileResult.put("message", e.getMessage());
                failFiles++;
            }

            fileResults.add(fileResult);
        }

        batchResult.put("totalFiles", totalFiles);
        batchResult.put("successFiles", successFiles);
        batchResult.put("failFiles", failFiles);
        batchResult.put("totalCount", totalCount);
        batchResult.put("successCount", successCount);
        batchResult.put("failCount", failCount);
        batchResult.put("duplicateCount", duplicateCount);
        batchResult.put("skipCount", skipCount);
        batchResult.put("fileResults", fileResults);
        String batchNo = null;
        for (Map<String, Object> fr : fileResults) {
            Object res = fr.get("result");
            if (res instanceof Map<?, ?> m && m.get("batchNo") != null) {
                batchNo = m.get("batchNo").toString();
                break;
            }
        }
        batchResult.put("batchNo", batchNo);

        log.info("Batch import completed: files={}/{}, total={}, success={}, fail={}, duplicate={}, skip={}",
                successFiles, totalFiles, totalCount, successCount, failCount, duplicateCount, skipCount);

        return batchResult;
    }

    /**
     * Parse a single CSV record into ShippingData entity (自动识别销售渠道)
     * @param marketplaceMap Pre-loaded marketplace configurations to avoid N+1 queries
     * @param headerLanguage 表头语言（EN/DE/CN），用于决定数字格式：
     *                       - DE: 德语表头，使用逗号作为小数分隔符
     *                       - EN/CN/其他: 使用点作为小数分隔符
     */
    private ShippingData parseShippingRecord(CSVRecord record, List<String> headers, int rowNum,
                                             Map<String, Marketplace> marketplaceMap, String headerLanguage) {

        Map<String, String> rowData = new HashMap<>();
        for (int i = 0; i < Math.min(headers.size(), record.size()); i++) {
            rowData.put(headers.get(i).toLowerCase().trim(), record.get(i).trim());
        }

        // 空行跳过（无订单号视为空行，不记入失败）
        String orderIdEarly = getFieldValue(rowData, "order id", "order-id", "orderid", "bestellnummer", "亚马逊订单编号");
        if (!StringUtils.hasText(orderIdEarly)) {
            return null;
        }

        // 从CSV中读取销售渠道（部分 Amazon 导出中 "sales channel" 可能指配送渠道 AFN/MFN，需从 marketplace 列获取实际站点；
        // 合并文件存在列错位时，销售渠道列可能被填成 AFN，需从整行扫描 amazon.xx）
        String salesChannel = resolveSalesChannel(rowData);
        if (!StringUtils.hasText(salesChannel)) {
            throw new BusinessException(ErrorCode.IMPORT_PARSE_ERROR, "Missing sales channel");
        }

        // 将销售渠道映射为站点编码（Non-Amazon MCF 订单返回 null，后续关联销售订单赋值）
        String siteCode = mapSalesChannelToSiteCode(salesChannel);

        ShippingData shippingData = new ShippingData();

        if (siteCode == null) {
            // MCF 订单：站点待定，标记为 PENDING，后续通过关联销售订单表回填
            shippingData.setSiteCode("PENDING");
            shippingData.setMarketplace(salesChannel);
            log.debug("MCF 订单（Non-Amazon），站点待关联：orderId={}", orderIdEarly);
        } else {
            // 从预加载的配置中获取 marketplace（避免 N+1 查询）
            Marketplace marketplace = marketplaceMap.get(siteCode);
            if (marketplace == null) {
                if (importConfig.isAutoCreateMarketplace()) {
                    marketplace = autoCreateMarketplace(siteCode);
                    marketplaceMap.put(siteCode, marketplace);
                    log.info("Auto-created marketplace for siteCode: {}", siteCode);
                } else {
                    throw new BusinessException(ErrorCode.DATA_NOT_EXIST, "Marketplace not found for site: " + siteCode);
                }
            }
            shippingData.setSiteCode(siteCode);
            shippingData.setMarketplace(marketplace.getMarketplaceId());
        }

        // 解析货币编码（优先使用 CSV 中的货币，否则使用 marketplace 默认值）
        // 列错位时货币列可能被填成数量等，需校验；有效货币为3位字母如 GBP/USD
        String currencyCode = getFieldValue(rowData, "currency", "货币", "currencycode");
        if (StringUtils.hasText(currencyCode) && isValidCurrencyCode(currencyCode)) {
            shippingData.setCurrencyCode(currencyCode.trim().toUpperCase());
        } else if ("PENDING".equals(shippingData.getSiteCode())) {
            // MCF 订单：货币暂时留空，resolveMcfSiteCodes 关联站点后会从 marketplace 配置补充
            shippingData.setCurrencyCode(null);
        } else {
            // 从 marketplace 配置中取默认货币
            Marketplace marketplace = marketplaceMap.get(shippingData.getSiteCode());
            shippingData.setCurrencyCode(marketplace != null ? marketplace.getCurrencyCode() : "USD");
        }

        // Parse order ID（已在前面用于空行判断）
        shippingData.setOrderId(orderIdEarly);

        // Parse ship date (支持中文列名)
        // MCF 订单站点未知时，使用 "US" 作为兼容性 fallback（不依赖站点格式）
        String dateStr = getFieldValue(rowData, "ship date", "shipment date", "shipped date", "versanddatum", "配送日期");
        if (StringUtils.hasText(dateStr)) {
            String parseSiteCode = siteCode != null ? siteCode : "US";
            LocalDateTime dateTime = csvParseService.parseDate(dateStr, parseSiteCode);
            if (dateTime != null) {
                shippingData.setShipDate(dateTime.toLocalDate());
            }
        }

        // Parse tracking number (支持中文列名)
        shippingData.setTrackingNumber(getFieldValue(rowData, "tracking number", "tracking", "trackingnumber", "追踪编码"));

        // Parse carrier (支持中文列名)
        shippingData.setCarrier(getFieldValue(rowData, "carrier", "shipping carrier", "versandunternehmen", "承运人"));

        // Parse SKU (支持中文列名)
        shippingData.setSku(getFieldValue(rowData, "sku", "asin", "product sku", "卖家 sku"));

        // Parse quantity (支持中文列名)
        String quantityStr = getFieldValue(rowData, "quantity", "qty", "menge", "已发货数量");
        if (StringUtils.hasText(quantityStr)) {
            try {
                shippingData.setQuantity(Integer.parseInt(quantityStr.replace(",", "")));
            } catch (NumberFormatException e) {
                shippingData.setQuantity(0);
            }
        }

        // Parse price fields (支持中文列名)
        // 注意：使用 headerLanguage 而不是 siteCode 来决定数字格式
        // 只有德语表头(DE)的文件才使用逗号作为小数分隔符
        // 中文表头(CN)和英文表头(EN)的文件使用点作为小数分隔符
        String numberFormatLocale = "DE".equals(headerLanguage) ? "DE" : "EN";
        shippingData.setProductPrice(parseDecimalFieldMulti(rowData, numberFormatLocale, "product price", "item price", "商品价格"));
        shippingData.setProductTax(parseDecimalFieldMulti(rowData, numberFormatLocale, "product tax", "item tax", "商品税"));
        shippingData.setShippingPrice(parseDecimalFieldMulti(rowData, numberFormatLocale, "shipping price", "shipping", "运费"));
        shippingData.setShippingTax(parseDecimalFieldMulti(rowData, numberFormatLocale, "shipping tax", "运费税"));
        shippingData.setGiftWrapPrice(parseDecimalFieldMulti(rowData, numberFormatLocale, "gift wrap price", "giftwrap", "礼品包装价格"));
        shippingData.setGiftWrapTax(parseDecimalFieldMulti(rowData, numberFormatLocale, "gift wrap tax", "礼品包装税费"));
        shippingData.setProductPromotionDiscount(parseDecimalFieldMulti(rowData, numberFormatLocale, "item promotion discount", "product promotion", "商品促销折扣"));
        shippingData.setShipmentPromotionDiscount(parseDecimalFieldMulti(rowData, numberFormatLocale, "ship promotion discount", "shipment promotion", "货件促销折扣"));
        shippingData.setShippingCost(parseDecimalFieldMulti(rowData, numberFormatLocale, "shipping cost", "cost"));

        // 导入时计算总计费用（各分项相加）
        shippingData.setTotalAmount(calculateShippingTotalAmount(shippingData));

        return shippingData;
    }

    /**
     * 计算总计费用（各分项相加）
     * 总计 = 商品价格 + 商品税 + 运费 + 运费税 + 礼品包装价格 + 礼品包装税 + 商品促销折扣 + 货件促销折扣
     */
    private BigDecimal calculateShippingTotalAmount(ShippingData d) {
        BigDecimal sum = BigDecimal.ZERO;
        if (d.getProductPrice() != null) sum = sum.add(d.getProductPrice());
        if (d.getProductTax() != null) sum = sum.add(d.getProductTax());
        if (d.getShippingPrice() != null) sum = sum.add(d.getShippingPrice());
        if (d.getShippingTax() != null) sum = sum.add(d.getShippingTax());
        if (d.getGiftWrapPrice() != null) sum = sum.add(d.getGiftWrapPrice());
        if (d.getGiftWrapTax() != null) sum = sum.add(d.getGiftWrapTax());
        if (d.getProductPromotionDiscount() != null) sum = sum.add(d.getProductPromotionDiscount());
        if (d.getShipmentPromotionDiscount() != null) sum = sum.add(d.getShipmentPromotionDiscount());
        return sum;
    }

    /**
     * 解析销售渠道：优先使用 marketplace 格式（amazon.xx），因部分 Amazon 导出中
     * "sales channel" 列实际为配送渠道（AFN/MFN），需从 marketplace/销售渠道 列获取实际站点。
     * 合并 CSV 存在列错位时，销售渠道列可能被填成 AFN，需从整行扫描含 amazon. 的值
     *（如买家电子邮件 xxx@marketplace.amazon.co.uk）。
     */
    private String resolveSalesChannel(Map<String, String> rowData) {
        String[] possibleNames = {"marketplace", "销售渠道", "sales channel", "saleschannel", "店铺"};
        String fallback = null;
        for (String name : possibleNames) {
            String value = rowData.get(name.toLowerCase().trim());
            if (value != null && !value.trim().isEmpty()) {
                String v = value.trim();
                // AFN/MFN 为配送渠道，非销售渠道；优先返回 marketplace 格式（含 amazon.）
                if (v.toLowerCase().contains("amazon.")) {
                    return v;
                }
                if ("AFN".equalsIgnoreCase(v) || "MFN".equalsIgnoreCase(v)) {
                    fallback = v; // 暂存，继续查找
                } else if (fallback == null) {
                    return v;
                }
            }
        }
        // 列错位时，已知列可能全是 AFN/MFN；从整行扫描含 amazon. 的值（如 @marketplace.amazon.co.uk）
        for (String v : rowData.values()) {
            if (v != null) {
                String s = v.trim();
                if (!s.isEmpty()) {
                    int idx = s.toLowerCase().indexOf("amazon.");
                    if (idx >= 0) {
                        String extracted = s.substring(idx);
                        int slash = extracted.indexOf("/");
                        if (slash > 0) {
                            extracted = extracted.substring(0, slash);
                        }
                        if (extracted.length() > 7) { // "amazon.x" 至少 8 字符
                            return extracted;
                        }
                    }
                }
            }
        }
        return fallback != null ? fallback : "";
    }

    /**
     * 批量关联 MCF（Non-Amazon）订单的站点编码
     * 通过订单号查询销售订单表，用销售订单的站点编码回填配送记录
     *
     * @param pendingRecords siteCode=PENDING 的配送记录列表
     * @param shopId         当前店铺ID（数据隔离）
     * @param rowErrorMap    行错误记录（key=行号/负数，value=错误信息）
     * @author wanhua
     * 10:00 2026年03月14日
     */
    private void resolveMcfSiteCodes(List<ShippingData> pendingRecords, Long shopId,
                                     Map<Integer, String> rowErrorMap) {
        // 收集所有待关联的订单号
        Set<String> pendingOrderIds = pendingRecords.stream()
                .map(ShippingData::getOrderId)
                .filter(StringUtils::hasText)
                .collect(java.util.stream.Collectors.toSet());

        if (pendingOrderIds.isEmpty()) {
            return;
        }

        // 批量查询销售订单表，获取订单号 -> 站点编码的映射
        LambdaQueryWrapper<SalesData> salesWrapper = new LambdaQueryWrapper<>();
        salesWrapper.eq(SalesData::getShopId, shopId)
                .in(SalesData::getOrderId, pendingOrderIds)
                .select(SalesData::getOrderId, SalesData::getSiteCode);

        List<SalesData> salesList = salesDataMapper.selectList(salesWrapper);

        // 构建 orderId -> siteCode 映射（同一订单可能有多条销售记录，取第一条的站点）
        Map<String, String> orderSiteMap = new HashMap<>();
        for (SalesData sales : salesList) {
            if (StringUtils.hasText(sales.getOrderId()) && StringUtils.hasText(sales.getSiteCode())) {
                orderSiteMap.putIfAbsent(sales.getOrderId(), sales.getSiteCode());
            }
        }

        log.info("销售订单查询完成：查询 {} 个订单号，找到 {} 个站点映射",
                pendingOrderIds.size(), orderSiteMap.size());

        // 回填站点编码，找不到的记录标记为失败
        int mcfErrorIndex = -1;
        for (ShippingData record : pendingRecords) {
            String siteCode = orderSiteMap.get(record.getOrderId());
            if (StringUtils.hasText(siteCode)) {
                record.setSiteCode(siteCode);
                // 补充 marketplace 配置（含货币编码）
                Marketplace marketplace = marketplaceMapper.selectOne(
                        new LambdaQueryWrapper<Marketplace>().eq(Marketplace::getSiteCode, siteCode));
                if (marketplace != null) {
                    record.setMarketplace(marketplace.getMarketplaceId());
                    // 用 marketplace 配置的货币覆盖解析阶段的值，确保准确
                    record.setCurrencyCode(marketplace.getCurrencyCode());
                } else {
                    // marketplace 配置不存在，无法确定货币，报错让处理人知晓
                    rowErrorMap.put(mcfErrorIndex--, String.format(
                            "MCF 订单关联站点成功（siteCode=%s），但未找到对应 Marketplace 配置，无法确定货币：orderId=%s",
                            siteCode, record.getOrderId()));
                    record.setSiteCode("PENDING"); // 回退为 PENDING，调用方会移除
                    log.warn("MCF 订单 Marketplace 配置缺失：orderId={}, siteCode={}", record.getOrderId(), siteCode);
                    continue;
                }
                log.debug("MCF 订单站点关联成功：orderId={}, siteCode={}, currency={}",
                        record.getOrderId(), siteCode, record.getCurrencyCode());
            } else {
                // 未找到对应销售订单，标记为失败（保持 PENDING，调用方会移除）
                // 用负数 key 区分 MCF 关联失败与解析阶段失败
                rowErrorMap.put(mcfErrorIndex--, String.format(
                        "MCF 订单未找到对应销售订单，无法确定站点：orderId=%s", record.getOrderId()));
                log.warn("MCF 订单关联失败，未找到销售订单：orderId={}", record.getOrderId());
            }
        }
    }

    /**
     * 将销售渠道映射为站点编码
     * 例如: amazon.de -> DE, amazon.co.uk -> UK, amazon.fr -> FR
     * Non-Amazon（MCF 多渠道配送订单）返回 null，由调用方关联销售订单确定站点
     *
     * @param salesChannel 销售渠道字符串
     * @return 站点编码，Non-Amazon 时返回 null
     * @author wanhua
     * 10:00 2026年03月14日
     */
    private String mapSalesChannelToSiteCode(String salesChannel) {
        if (!StringUtils.hasText(salesChannel)) {
            throw new BusinessException(ErrorCode.IMPORT_PARSE_ERROR, "Sales channel is empty");
        }

        String channel = salesChannel.toLowerCase().trim();

        // 映射规则
        if (channel.contains("amazon.de")) {
            return "DE";
        } else if (channel.contains("amazon.co.uk") || channel.contains("amazon.uk")) {
            return "UK";
        } else if (channel.contains("amazon.fr")) {
            return "FR";
        } else if (channel.contains("amazon.it")) {
            return "IT";
        } else if (channel.contains("amazon.es")) {
            return "ES";
        } else if (channel.contains("amazon.nl")) {
            return "NL";
        } else if (channel.contains("amazon.pl")) {
            return "PL";
        } else if (channel.contains("amazon.se")) {
            return "SE";
        } else if (channel.contains("amazon.ca")) {
            return "CA";
        } else if (channel.contains("amazon.com")) {
            return "US";
        } else if (channel.contains("non-amazon")) {
            // MCF（多渠道配送）订单：卖家通过非亚马逊渠道接单，委托亚马逊仓库发货
            // 站点未知，返回 null，后续通过关联销售订单表来确定站点
            return null;
        } else {
            throw new BusinessException(ErrorCode.IMPORT_PARSE_ERROR,
                    "Unsupported sales channel: " + salesChannel);
        }
    }

    /**
     * Parse decimal field from row data
     */
    private BigDecimal parseDecimalField(Map<String, String> rowData, String fieldName, String siteCode) {
        String value = getFieldValue(rowData, fieldName);
        if (StringUtils.hasText(value)) {
            return csvParseService.parseDecimal(value, siteCode);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Parse decimal field with alternate names
     */
    private BigDecimal parseDecimalField(Map<String, String> rowData, String fieldName, String altName, String siteCode) {
        String value = getFieldValue(rowData, fieldName, altName);
        if (StringUtils.hasText(value)) {
            return csvParseService.parseDecimal(value, siteCode);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Parse decimal field with multiple possible names (支持多个列名)
     */
    private BigDecimal parseDecimalFieldMulti(Map<String, String> rowData, String siteCode, String... possibleNames) {
        String value = getFieldValue(rowData, possibleNames);
        if (StringUtils.hasText(value)) {
            return csvParseService.parseDecimal(value, siteCode);
        }
        return BigDecimal.ZERO;
    }

    /**
     * 校验是否为有效货币代码（3位字母，如 GBP/USD/EUR）
     * 列错位时货币列可能被填成数字等，需过滤
     */
    private boolean isValidCurrencyCode(String code) {
        if (code == null || code.trim().length() != 3) {
            return false;
        }
        String c = code.trim().toUpperCase();
        return c.chars().allMatch(Character::isLetter);
    }

    /**
     * Get field value from row data with multiple possible column names
     */
    private String getFieldValue(Map<String, String> rowData, String... possibleNames) {
        for (String name : possibleNames) {
            if (name == null) continue;
            String key = name.toLowerCase().trim();
            String value = rowData.get(key);
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }

    /**
     * Batch check for duplicates to avoid N+1 query problem
     * Returns a set of unique keys that already exist in database
     * Unique constraint: order_id + site_code + tracking_number
     */
    private Set<String> batchCheckDuplicates(List<ShippingData> records) {
        if (records.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> existingKeys = new HashSet<>();

        // Build conditions for batch query
        LambdaQueryWrapper<ShippingData> wrapper = new LambdaQueryWrapper<>();

        // Group records by site code for efficient querying
        Map<String, List<ShippingData>> bySite = records.stream()
                .filter(r -> StringUtils.hasText(r.getOrderId()))
                .collect(java.util.stream.Collectors.groupingBy(ShippingData::getSiteCode));

        for (Map.Entry<String, List<ShippingData>> entry : bySite.entrySet()) {
            String siteCode = entry.getKey();
            List<ShippingData> siteRecords = entry.getValue();

            // Get all order IDs for this site
            Set<String> orderIds = siteRecords.stream()
                    .map(ShippingData::getOrderId)
                    .filter(StringUtils::hasText)
                    .collect(java.util.stream.Collectors.toSet());

            if (orderIds.isEmpty()) {
                continue;
            }

            // Query existing records for this site
            LambdaQueryWrapper<ShippingData> siteWrapper = new LambdaQueryWrapper<>();
            siteWrapper.eq(ShippingData::getSiteCode, siteCode)
                    .in(ShippingData::getOrderId, orderIds)
                    .select(ShippingData::getOrderId, ShippingData::getSiteCode, ShippingData::getTrackingNumber);

            List<ShippingData> existing = shippingDataMapper.selectList(siteWrapper);

            // Build set of existing unique keys
            for (ShippingData existingData : existing) {
                existingKeys.add(buildUniqueKey(existingData));
            }
        }

        log.info("Batch duplicate check: found {} existing records", existingKeys.size());
        return existingKeys;
    }

    /**
     * Build unique key for duplicate detection
     * Format: orderId|siteCode|trackingNumber
     */
    private String buildUniqueKey(ShippingData data) {
        String orderId = data.getOrderId() != null ? data.getOrderId() : "";
        String siteCode = data.getSiteCode() != null ? data.getSiteCode() : "";
        String trackingNumber = data.getTrackingNumber() != null ? data.getTrackingNumber() : "";
        return orderId + "|" + siteCode + "|" + trackingNumber;
    }

    /**
     * Generate unique batch number
     */
    private String generateBatchNo() {
        return "SHIP-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 自动创建站点（当 import.auto-create-marketplace=true 时）
     */
    private Marketplace autoCreateMarketplace(String siteCode) {
        String currencyCode = mapSiteCodeToCurrency(siteCode);

        MarketplaceRequest request = new MarketplaceRequest();
        request.setSiteCode(siteCode);
        request.setSiteName(siteCode);
        request.setMarketplaceId("AUTO_" + siteCode);
        request.setCurrencyCode(currencyCode);
        request.setStatus(1);

        return marketplaceService.create(request);
    }

    /**
     * 根据站点编码推断默认货币
     */
    private String mapSiteCodeToCurrency(String siteCode) {
        if (siteCode == null) {
            return "USD";
        }
        return switch (siteCode.toUpperCase()) {
            case "US" -> "USD";
            case "CA" -> "CAD";
            case "MX" -> "MXN";
            case "UK", "GB" -> "GBP";
            case "DE", "FR", "IT", "ES", "NL", "BE", "AT", "PL" -> "EUR";
            case "JP" -> "JPY";
            case "AU" -> "AUD";
            case "SG" -> "SGD";
            case "AE", "SA" -> "AED";
            case "IN" -> "INR";
            case "BR" -> "BRL";
            case "SE" -> "SEK";
            default -> "USD";
        };
    }

    /**
     * 预热汇率缓存（批量查询去重的 currencyCode+date 组合）
     * 避免在 fillExchangeRate 循环中产生 N 次 DB 查询
     */
    private void preloadExchangeRateCache(List<ShippingData> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            return;
        }

        // 收集唯一的 (currencyCode, date) 组合
        Set<String> uniquePairs = new HashSet<>();
        for (ShippingData data : dataList) {
            if (data.getShipDate() != null && StringUtils.hasText(data.getCurrencyCode())) {
                String currencyCode = data.getCurrencyCode();
                if (!"CNY".equalsIgnoreCase(currencyCode)) {
                    uniquePairs.add(currencyCode + "|" + data.getShipDate());
                }
            }
        }

        if (uniquePairs.isEmpty()) {
            return;
        }

        log.info("Preloading exchange rate cache for {} unique (currency, date) pairs", uniquePairs.size());

        int loadedCount = 0;
        for (String pair : uniquePairs) {
            String[] parts = pair.split("\\|");
            String currencyCode = parts[0];
            LocalDate date = LocalDate.parse(parts[1]);
            try {
                rateService.getRateWithDate(currencyCode, date);
                loadedCount++;
            } catch (Exception e) {
                log.warn("Failed to preload rate for {}/{}: {}", currencyCode, date, e.getMessage());
            }
        }

        log.info("Exchange rate cache preloaded: {}/{} pairs", loadedCount, uniquePairs.size());
    }

    /**
     * Fill exchange rate for shipping data based on ship date
     * If the ship date is a holiday/weekend, the rate service will automatically
     * use the next workday's rate
     */
    private void fillExchangeRate(ShippingData data) {
        if (data.getShipDate() == null || !StringUtils.hasText(data.getCurrencyCode())) {
            log.debug("Skipping exchange rate fill: shipDate={}, currencyCode={}",
                    data.getShipDate(), data.getCurrencyCode());
            return;
        }

        // CNY doesn't need exchange rate conversion
        if ("CNY".equalsIgnoreCase(data.getCurrencyCode())) {
            data.setExchangeRate(BigDecimal.ONE);
            data.setExchangeRateDate(data.getShipDate());
            return;
        }

        var rateWithDate = rateService.getRateWithDate(data.getCurrencyCode(), data.getShipDate());
        data.setExchangeRate(rateWithDate.getRate());
        data.setExchangeRateDate(rateWithDate.getActualDate());

        log.debug("Exchange rate filled: currency={}, shipDate={}, rate={}, actualRateDate={}",
                data.getCurrencyCode(), data.getShipDate(), rateWithDate.getRate(), rateWithDate.getActualDate());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ShippingData entity = shippingDataMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST, "Shipping data not found");
        }

        shippingDataMapper.deleteById(id);
        log.info("Deleted shipping data: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        shippingDataMapper.deleteBatchIds(ids);
        log.info("Batch deleted shipping data: ids={}", ids);
    }

    @Override
    public Map<String, Object> getSummary(String siteCode, String startDate, String endDate) {
        LambdaQueryWrapper<ShippingData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShippingData::getShopId, ShopContext.requireShopId());

        if (StringUtils.hasText(siteCode)) {
            wrapper.eq(ShippingData::getSiteCode, siteCode);
        }
        if (StringUtils.hasText(startDate)) {
            try {
                java.time.LocalDate start = java.time.LocalDate.parse(startDate);
                wrapper.ge(ShippingData::getShipDate, start);
            } catch (Exception e) {
                log.warn("Invalid start date format: {}", startDate);
            }
        }
        if (StringUtils.hasText(endDate)) {
            try {
                java.time.LocalDate end = java.time.LocalDate.parse(endDate);
                wrapper.le(ShippingData::getShipDate, end);
            } catch (Exception e) {
                log.warn("Invalid end date format: {}", endDate);
            }
        }

        List<ShippingData> dataList = shippingDataMapper.selectList(wrapper);

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalOrders", dataList.size());
        summary.put("totalQuantity", dataList.stream()
                .mapToInt(d -> d.getQuantity() != null ? d.getQuantity() : 0).sum());

        // 按汇率转换为人民币后汇总（多站点数据统一货币）
        summary.put("totalProductPriceCny", dataList.stream()
                .map(d -> convertToCny(d.getProductPrice(), d.getExchangeRate()))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        summary.put("totalProductTaxCny", dataList.stream()
                .map(d -> convertToCny(d.getProductTax(), d.getExchangeRate()))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        summary.put("totalShippingPriceCny", dataList.stream()
                .map(d -> convertToCny(d.getShippingPrice(), d.getExchangeRate()))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        summary.put("totalShippingTaxCny", dataList.stream()
                .map(d -> convertToCny(d.getShippingTax(), d.getExchangeRate()))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        summary.put("totalGiftWrapPriceCny", dataList.stream()
                .map(d -> convertToCny(d.getGiftWrapPrice(), d.getExchangeRate()))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        summary.put("totalGiftWrapTaxCny", dataList.stream()
                .map(d -> convertToCny(d.getGiftWrapTax(), d.getExchangeRate()))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        summary.put("totalProductPromotionDiscountCny", dataList.stream()
                .map(d -> convertToCny(d.getProductPromotionDiscount(), d.getExchangeRate()))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        summary.put("totalShipmentPromotionDiscountCny", dataList.stream()
                .map(d -> convertToCny(d.getShipmentPromotionDiscount(), d.getExchangeRate()))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        summary.put("totalShippingCostCny", dataList.stream()
                .map(d -> convertToCny(d.getShippingCost(), d.getExchangeRate()))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        // 总计费用(CNY) = 8项费用之和：商品价格+商品税+运费+运费税+礼品包装价格+礼品包装税费+商品促销折扣+货件促销折扣
        BigDecimal totalAmountCny = ((BigDecimal) summary.get("totalProductPriceCny")).add((BigDecimal) summary.get("totalProductTaxCny"))
                .add((BigDecimal) summary.get("totalShippingPriceCny")).add((BigDecimal) summary.get("totalShippingTaxCny"))
                .add((BigDecimal) summary.get("totalGiftWrapPriceCny")).add((BigDecimal) summary.get("totalGiftWrapTaxCny"))
                .add((BigDecimal) summary.get("totalProductPromotionDiscountCny")).add((BigDecimal) summary.get("totalShipmentPromotionDiscountCny"));
        summary.put("totalAmountCny", totalAmountCny);

        // 货币统一为人民币
        summary.put("currencyCode", "CNY");

        return summary;
    }

    /**
     * 将金额按汇率转换为人民币
     * @param amount 原始金额
     * @param exchangeRate 汇率（对人民币）
     * @return 人民币金额
     */
    private BigDecimal convertToCny(BigDecimal amount, BigDecimal exchangeRate) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        if (exchangeRate == null || exchangeRate.compareTo(BigDecimal.ZERO) == 0) {
            // 如果没有汇率，返回0（避免错误累加）
            log.warn("Missing exchange rate for conversion, amount={}", amount);
            return BigDecimal.ZERO;
        }
        return amount.multiply(exchangeRate);
    }

    @Override
    public void exportData(String siteCode, String startDate, String endDate,
                           jakarta.servlet.http.HttpServletResponse response) {
        LambdaQueryWrapper<ShippingData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShippingData::getShopId, ShopContext.requireShopId());

        if (StringUtils.hasText(siteCode)) {
            wrapper.eq(ShippingData::getSiteCode, siteCode);
        }
        if (StringUtils.hasText(startDate)) {
            try {
                java.time.LocalDate start = java.time.LocalDate.parse(startDate);
                wrapper.ge(ShippingData::getShipDate, start);
            } catch (Exception e) {
                log.warn("Invalid start date format: {}", startDate);
            }
        }
        if (StringUtils.hasText(endDate)) {
            try {
                java.time.LocalDate end = java.time.LocalDate.parse(endDate);
                wrapper.le(ShippingData::getShipDate, end);
            } catch (Exception e) {
                log.warn("Invalid end date format: {}", endDate);
            }
        }

        wrapper.orderByDesc(ShippingData::getShipDate);
        List<ShippingData> dataList = shippingDataMapper.selectList(wrapper);

        try {
            String fileName = "shipping_data_" + System.currentTimeMillis() + ".xlsx";
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=" +
                    java.net.URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8));

            try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
                 java.io.OutputStream outputStream = response.getOutputStream()) {

                org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Shipping Data");

                // Create header row
                org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
                String[] headers = {"订单号", "站点", "发货日期", "SKU", "数量",
                        "商品价格", "商品税", "运费", "运费税", "礼品包装价格", "礼品包装税",
                        "商品促销折扣", "货件促销折扣", "物流费用", "货币", "总计费用",
                        "汇率", "总计费用(CNY)", "汇率日期"};
                for (int i = 0; i < headers.length; i++) {
                    headerRow.createCell(i).setCellValue(headers[i]);
                }

                // Fill data rows
                int rowNum = 1;
                for (ShippingData data : dataList) {
                    org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(data.getOrderId());
                    row.createCell(1).setCellValue(data.getSiteCode());
                    row.createCell(2).setCellValue(data.getShipDate() != null ? data.getShipDate().toString() : "");
                    row.createCell(3).setCellValue(data.getSku());
                    row.createCell(4).setCellValue(data.getQuantity() != null ? data.getQuantity() : 0);
                    row.createCell(5).setCellValue(data.getProductPrice() != null ? data.getProductPrice().doubleValue() : 0);
                    row.createCell(6).setCellValue(data.getProductTax() != null ? data.getProductTax().doubleValue() : 0);
                    row.createCell(7).setCellValue(data.getShippingPrice() != null ? data.getShippingPrice().doubleValue() : 0);
                    row.createCell(8).setCellValue(data.getShippingTax() != null ? data.getShippingTax().doubleValue() : 0);
                    row.createCell(9).setCellValue(data.getGiftWrapPrice() != null ? data.getGiftWrapPrice().doubleValue() : 0);
                    row.createCell(10).setCellValue(data.getGiftWrapTax() != null ? data.getGiftWrapTax().doubleValue() : 0);
                    row.createCell(11).setCellValue(data.getProductPromotionDiscount() != null ? data.getProductPromotionDiscount().doubleValue() : 0);
                    row.createCell(12).setCellValue(data.getShipmentPromotionDiscount() != null ? data.getShipmentPromotionDiscount().doubleValue() : 0);
                    row.createCell(13).setCellValue(data.getShippingCost() != null ? data.getShippingCost().doubleValue() : 0);
                    row.createCell(14).setCellValue(data.getCurrencyCode());
                    row.createCell(15).setCellValue(data.getTotalAmount() != null ? data.getTotalAmount().doubleValue() : 0);
                    row.createCell(16).setCellValue(data.getExchangeRate() != null ? data.getExchangeRate().doubleValue() : 0);
                    row.createCell(17).setCellValue(convertToCny(data.getTotalAmount(), data.getExchangeRate()).doubleValue());
                    row.createCell(18).setCellValue(data.getExchangeRateDate() != null ? data.getExchangeRateDate().toString() : "");
                }

                workbook.write(outputStream);
                outputStream.flush();
            }
        } catch (java.io.IOException e) {
            log.error("Failed to export shipping data", e);
            throw new BusinessException(ErrorCode.EXPORT_FAILED, "导出失败: " + e.getMessage());
        }
    }
}
