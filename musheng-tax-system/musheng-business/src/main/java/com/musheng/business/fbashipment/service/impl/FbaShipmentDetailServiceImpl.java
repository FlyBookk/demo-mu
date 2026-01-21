package com.musheng.business.fbashipment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.business.common.service.csv.CsvHeaderResult;
import com.musheng.business.common.service.csv.CsvParseServiceImpl;
import com.musheng.business.fbashipment.entity.FbaShipmentDetail;
import com.musheng.business.fbashipment.mapper.FbaShipmentDetailMapper;
import com.musheng.business.fbashipment.service.FbaShipmentDetailService;
import com.musheng.common.exception.BusinessException;
import com.musheng.common.result.ErrorCode;
import com.musheng.config.importrecord.entity.ImportRecord;
import com.musheng.config.importrecord.mapper.ImportRecordMapper;
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
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

/**
 * FBA货件明细服务实现
 * 实现CSV导入批量处理，避免N+1查询问题
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FbaShipmentDetailServiceImpl implements FbaShipmentDetailService {

    private final FbaShipmentDetailMapper fbaShipmentDetailMapper;
    private final ImportRecordMapper importRecordMapper;
    private final CsvParseServiceImpl csvParseService;

    @Override
    public Page<FbaShipmentDetail> list(String status, String shipmentId, String receivingAddress, int page, int size) {
        LambdaQueryWrapper<FbaShipmentDetail> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(status)) {
            wrapper.eq(FbaShipmentDetail::getStatus, status);
        }
        if (StringUtils.hasText(shipmentId)) {
            wrapper.like(FbaShipmentDetail::getShipmentId, shipmentId);
        }
        if (StringUtils.hasText(receivingAddress)) {
            wrapper.eq(FbaShipmentDetail::getReceivingAddress, receivingAddress);
        }

        wrapper.orderByDesc(FbaShipmentDetail::getCreateTime);

        return fbaShipmentDetailMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public FbaShipmentDetail getById(Long id) {
        FbaShipmentDetail entity = fbaShipmentDetailMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST, "FBA货件明细不存在");
        }
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importData(MultipartFile file) {
        log.info("导入FBA货件明细: fileName={}, size={} bytes",
                file.getOriginalFilename(), file.getSize());

        Map<String, Object> result = new HashMap<>();
        List<String> errors = new ArrayList<>();
        int totalCount = 0;
        int successCount = 0;
        int failCount = 0;
        int duplicateCount = 0;

        // 创建导入记录
        ImportRecord importRecord = new ImportRecord();
        importRecord.setBatchNo(generateBatchNo());
        importRecord.setDataType("fba_shipment");
        importRecord.setFileName(file.getOriginalFilename());
        importRecord.setFileSize(file.getSize());
        importRecord.setImportStatus("processing");
        importRecordMapper.insert(importRecord);

        try {
            // 检测CSV表头和编码
            log.info("检测CSV表头...");
            CsvHeaderResult headerResult = csvParseService.parseHeaders(file);
            log.info("表头检测成功: language={}, charset={}, headerRowIndex={}, columns={}",
                    headerResult.getHeaderLanguage(),
                    headerResult.getCharset(),
                    headerResult.getHeaderRowIndex(),
                    headerResult.getHeaders().size());

            List<String> headers = headerResult.getHeaders();
            int headerRowIndex = headerResult.getHeaderRowIndex();
            String charsetName = headerResult.getCharset();
            java.nio.charset.Charset charset = charsetName != null ?
                    java.nio.charset.Charset.forName(charsetName) : StandardCharsets.UTF_8;

            // Step 1: 解析所有记录到List（避免N+1查询）
            List<FbaShipmentDetail> parsedRecords = new ArrayList<>();
            Map<Integer, String> rowErrorMap = new HashMap<>();

            // 重新打开文件流并跳过表头
            try (BufferedReader dataReader = new BufferedReader(
                    new InputStreamReader(file.getInputStream(), charset))) {

                // 跳过表头行
                for (int i = 0; i <= headerRowIndex; i++) {
                    dataReader.readLine();
                }

                CSVParser parser = CSVFormat.DEFAULT.parse(dataReader);

                for (CSVRecord record : parser) {
                    totalCount++;
                    try {
                        FbaShipmentDetail detail = parseRecord(record, headers, totalCount);

                        if (detail != null) {
                            detail.setImportBatchId(importRecord.getId());
                            parsedRecords.add(detail);
                        }
                    } catch (Exception e) {
                        failCount++;
                        rowErrorMap.put(totalCount, e.getMessage());
                        log.warn("解析第{}行失败: {}", totalCount, e.getMessage());
                    }
                }
            }

            log.info("解析完成: 共{}行, 成功解析{}条", totalCount, parsedRecords.size());

            // Step 2: 批量检测重复（单次查询避免N+1）
            List<FbaShipmentDetail> toInsert = new ArrayList<>();
            List<FbaShipmentDetail> duplicates = new ArrayList<>();

            if (!parsedRecords.isEmpty()) {
                Set<String> existingShipmentIds = batchCheckDuplicates(parsedRecords);

                for (FbaShipmentDetail detail : parsedRecords) {
                    if (existingShipmentIds.contains(detail.getShipmentId())) {
                        duplicates.add(detail);
                        duplicateCount++;
                    } else {
                        toInsert.add(detail);
                    }
                }
            }

            log.info("重复检测完成: {}条待插入, {}条重复", toInsert.size(), duplicates.size());

            // Step 3: 批量插入（每批500条）
            if (!toInsert.isEmpty()) {
                int batchSize = 500;
                for (int i = 0; i < toInsert.size(); i += batchSize) {
                    int end = Math.min(i + batchSize, toInsert.size());
                    List<FbaShipmentDetail> batch = toInsert.subList(i, end);

                    batch.forEach(fbaShipmentDetailMapper::insert);
                    successCount += batch.size();

                    log.info("批量插入记录 {}-{}/{}", i + 1, end, toInsert.size());
                }
            }

            // 添加重复记录错误信息
            for (FbaShipmentDetail dup : duplicates) {
                if (errors.size() < 10) {
                    errors.add(String.format("重复: shipmentId=%s", dup.getShipmentId()));
                }
            }

            // 添加解析错误信息
            for (Map.Entry<Integer, String> entry : rowErrorMap.entrySet()) {
                if (errors.size() < 10) {
                    errors.add(String.format("第%d行: %s", entry.getKey(), entry.getValue()));
                }
            }

            // 更新导入记录
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
            log.error("导入FBA货件明细失败", e);
            importRecord.setImportStatus("fail");
            importRecord.setErrorMessage(e.getMessage());
            importRecordMapper.updateById(importRecord);
            throw new BusinessException(ErrorCode.IMPORT_PARSE_ERROR, "解析文件失败: " + e.getMessage());
        }

        result.put("totalCount", totalCount);
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("duplicateCount", duplicateCount);
        result.put("errors", errors);
        result.put("batchNo", importRecord.getBatchNo());

        log.info("FBA货件明细导入完成: total={}, success={}, fail={}, duplicate={}",
                totalCount, successCount, failCount, duplicateCount);

        return result;
    }

    /**
     * 解析单条CSV记录
     */
    private FbaShipmentDetail parseRecord(CSVRecord record, List<String> headers, int rowNum) {
        Map<String, String> rowData = new HashMap<>();
        for (int i = 0; i < Math.min(headers.size(), record.size()); i++) {
            rowData.put(headers.get(i).toLowerCase().trim(), record.get(i).trim());
        }

        // 解析货件编号（必填）
        String shipmentId = getFieldValue(rowData, "货件编号", "shipment id", "shipmentid");
        if (!StringUtils.hasText(shipmentId)) {
            return null; // 跳过空行
        }

        FbaShipmentDetail detail = new FbaShipmentDetail();
        detail.setShipmentId(shipmentId);

        // 解析货件名称
        detail.setShipmentName(getFieldValue(rowData, "货件名称", "shipment name", "shipmentname"));

        // 解析日期字段（使用Hutool）
        String createdDateStr = getFieldValue(rowData, "已创建", "created", "created date");
        if (StringUtils.hasText(createdDateStr)) {
            detail.setCreatedDate(csvParseService.parseDate(createdDateStr, "CN"));
        }

        String lastUpdatedStr = getFieldValue(rowData, "上次更新", "last updated", "lastupdated");
        if (StringUtils.hasText(lastUpdatedStr)) {
            detail.setLastUpdated(csvParseService.parseDate(lastUpdatedStr, "CN"));
        }

        // 解析收货地址
        detail.setReceivingAddress(getFieldValue(rowData, "收货地址", "receiving address", "destination"));

        // 解析数量字段
        String skuCountStr = getFieldValue(rowData, "sku", "sku count");
        if (StringUtils.hasText(skuCountStr)) {
            try {
                detail.setSkuCount(Integer.parseInt(skuCountStr.replace(",", "")));
            } catch (NumberFormatException e) {
                log.warn("SKU数量解析失败: {}", skuCountStr);
            }
        }

        String expectedQtyStr = getFieldValue(rowData, "预计商品数量", "expected quantity", "shipped");
        if (StringUtils.hasText(expectedQtyStr)) {
            try {
                detail.setExpectedQuantity(Integer.parseInt(expectedQtyStr.replace(",", "")));
            } catch (NumberFormatException e) {
                log.warn("预计数量解析失败: {}", expectedQtyStr);
            }
        }

        String foundQtyStr = getFieldValue(rowData, "找到的商品数量", "found quantity", "received");
        if (StringUtils.hasText(foundQtyStr)) {
            try {
                detail.setFoundQuantity(Integer.parseInt(foundQtyStr.replace(",", "")));
            } catch (NumberFormatException e) {
                log.warn("找到数量解析失败: {}", foundQtyStr);
            }
        }

        // 解析状态
        detail.setStatus(getFieldValue(rowData, "状态", "status"));

        return detail;
    }

    /**
     * 批量检测重复（避免N+1查询）
     * 唯一性约束: shipment_id
     */
    private Set<String> batchCheckDuplicates(List<FbaShipmentDetail> records) {
        if (records.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> existingIds = new HashSet<>();

        // 收集所有货件编号
        Set<String> shipmentIds = new HashSet<>();
        for (FbaShipmentDetail record : records) {
            if (StringUtils.hasText(record.getShipmentId())) {
                shipmentIds.add(record.getShipmentId());
            }
        }

        if (shipmentIds.isEmpty()) {
            return existingIds;
        }

        // 批量查询已存在的货件编号
        LambdaQueryWrapper<FbaShipmentDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(FbaShipmentDetail::getShipmentId, shipmentIds)
                .select(FbaShipmentDetail::getShipmentId);

        List<FbaShipmentDetail> existing = fbaShipmentDetailMapper.selectList(wrapper);

        for (FbaShipmentDetail detail : existing) {
            existingIds.add(detail.getShipmentId());
        }

        log.info("批量重复检测: 找到{}条已存在记录", existingIds.size());
        return existingIds;
    }

    /**
     * 获取字段值（支持多个可能的字段名）
     */
    private String getFieldValue(Map<String, String> rowData, String... possibleNames) {
        for (String name : possibleNames) {
            String value = rowData.get(name.toLowerCase());
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }
        return "";
    }

    /**
     * 生成唯一批次号
     */
    private String generateBatchNo() {
        return "FBA-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long add(FbaShipmentDetail fbaShipmentDetail) {
        // 检查货件编号是否重复
        LambdaQueryWrapper<FbaShipmentDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FbaShipmentDetail::getShipmentId, fbaShipmentDetail.getShipmentId());
        if (fbaShipmentDetailMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.IMPORT_DUPLICATE_DATA, "货件编号已存在");
        }

        fbaShipmentDetailMapper.insert(fbaShipmentDetail);
        log.info("新增FBA货件明细: id={}, shipmentId={}",
                fbaShipmentDetail.getId(), fbaShipmentDetail.getShipmentId());
        return fbaShipmentDetail.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(FbaShipmentDetail fbaShipmentDetail) {
        FbaShipmentDetail existing = fbaShipmentDetailMapper.selectById(fbaShipmentDetail.getId());
        if (existing == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST, "FBA货件明细不存在");
        }

        // 检查货件编号是否与其他记录重复
        LambdaQueryWrapper<FbaShipmentDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FbaShipmentDetail::getShipmentId, fbaShipmentDetail.getShipmentId())
                .ne(FbaShipmentDetail::getId, fbaShipmentDetail.getId());
        if (fbaShipmentDetailMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.IMPORT_DUPLICATE_DATA, "货件编号已存在");
        }

        fbaShipmentDetailMapper.updateById(fbaShipmentDetail);
        log.info("更新FBA货件明细: id={}", fbaShipmentDetail.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        FbaShipmentDetail entity = fbaShipmentDetailMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST, "FBA货件明细不存在");
        }

        fbaShipmentDetailMapper.deleteById(id);
        log.info("删除FBA货件明细: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        fbaShipmentDetailMapper.deleteBatchIds(ids);
        log.info("批量删除FBA货件明细: ids={}", ids);
    }

    @Override
    public Map<String, Object> getSummary(String status, String receivingAddress,
                                          String startDate, String endDate) {
        LambdaQueryWrapper<FbaShipmentDetail> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(status)) {
            wrapper.eq(FbaShipmentDetail::getStatus, status);
        }
        if (StringUtils.hasText(receivingAddress)) {
            wrapper.eq(FbaShipmentDetail::getReceivingAddress, receivingAddress);
        }
        if (StringUtils.hasText(startDate)) {
            try {
                java.time.LocalDate start = java.time.LocalDate.parse(startDate);
                wrapper.ge(FbaShipmentDetail::getCreatedDate, start.atStartOfDay());
            } catch (Exception e) {
                log.warn("开始日期格式错误: {}", startDate);
            }
        }
        if (StringUtils.hasText(endDate)) {
            try {
                java.time.LocalDate end = java.time.LocalDate.parse(endDate);
                wrapper.le(FbaShipmentDetail::getCreatedDate, end.atTime(23, 59, 59));
            } catch (Exception e) {
                log.warn("结束日期格式错误: {}", endDate);
            }
        }

        List<FbaShipmentDetail> dataList = fbaShipmentDetailMapper.selectList(wrapper);

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalShipments", dataList.size());
        summary.put("totalSkuCount", dataList.stream()
                .mapToInt(d -> d.getSkuCount() != null ? d.getSkuCount() : 0).sum());
        summary.put("totalExpectedQuantity", dataList.stream()
                .mapToInt(d -> d.getExpectedQuantity() != null ? d.getExpectedQuantity() : 0).sum());
        summary.put("totalFoundQuantity", dataList.stream()
                .mapToInt(d -> d.getFoundQuantity() != null ? d.getFoundQuantity() : 0).sum());

        return summary;
    }

    @Override
    public void exportData(String status, String receivingAddress, String startDate, String endDate,
                          jakarta.servlet.http.HttpServletResponse response) {
        LambdaQueryWrapper<FbaShipmentDetail> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(status)) {
            wrapper.eq(FbaShipmentDetail::getStatus, status);
        }
        if (StringUtils.hasText(receivingAddress)) {
            wrapper.eq(FbaShipmentDetail::getReceivingAddress, receivingAddress);
        }
        if (StringUtils.hasText(startDate)) {
            try {
                java.time.LocalDate start = java.time.LocalDate.parse(startDate);
                wrapper.ge(FbaShipmentDetail::getCreatedDate, start.atStartOfDay());
            } catch (Exception e) {
                log.warn("开始日期格式错误: {}", startDate);
            }
        }
        if (StringUtils.hasText(endDate)) {
            try {
                java.time.LocalDate end = java.time.LocalDate.parse(endDate);
                wrapper.le(FbaShipmentDetail::getCreatedDate, end.atTime(23, 59, 59));
            } catch (Exception e) {
                log.warn("结束日期格式错误: {}", endDate);
            }
        }

        wrapper.orderByDesc(FbaShipmentDetail::getCreatedDate);
        List<FbaShipmentDetail> dataList = fbaShipmentDetailMapper.selectList(wrapper);

        try {
            String fileName = "fba_shipment_detail_" + System.currentTimeMillis() + ".xlsx";
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=" +
                    java.net.URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8));

            try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
                 java.io.OutputStream outputStream = response.getOutputStream()) {

                org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("FBA货件明细");

                // 创建表头
                org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
                String[] headers = {"货件名称", "货件编号", "创建时间", "最后更新", "收货地址",
                        "SKU数", "预计数量", "找到数量", "状态"};
                for (int i = 0; i < headers.length; i++) {
                    headerRow.createCell(i).setCellValue(headers[i]);
                }

                // 填充数据
                int rowNum = 1;
                for (FbaShipmentDetail data : dataList) {
                    org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(data.getShipmentName());
                    row.createCell(1).setCellValue(data.getShipmentId());
                    row.createCell(2).setCellValue(data.getCreatedDate() != null ? data.getCreatedDate().toString() : "");
                    row.createCell(3).setCellValue(data.getLastUpdated() != null ? data.getLastUpdated().toString() : "");
                    row.createCell(4).setCellValue(data.getReceivingAddress());
                    row.createCell(5).setCellValue(data.getSkuCount() != null ? data.getSkuCount() : 0);
                    row.createCell(6).setCellValue(data.getExpectedQuantity() != null ? data.getExpectedQuantity() : 0);
                    row.createCell(7).setCellValue(data.getFoundQuantity() != null ? data.getFoundQuantity() : 0);
                    row.createCell(8).setCellValue(data.getStatus());
                }

                workbook.write(outputStream);
                outputStream.flush();
            }
        } catch (java.io.IOException e) {
            log.error("导出FBA货件明细失败", e);
            throw new BusinessException(ErrorCode.EXPORT_FAILED, "导出失败: " + e.getMessage());
        }
    }
}
