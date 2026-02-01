package com.musheng.business.fbashipment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.business.fbashipment.entity.FbaShipment;
import com.musheng.business.fbashipment.entity.FbaShipmentItem;
import com.musheng.business.fbashipment.mapper.FbaShipmentItemMapper;
import com.musheng.business.fbashipment.mapper.FbaShipmentMapper;
import com.musheng.business.fbashipment.service.FbaShipmentExcelParser;
import com.musheng.business.fbashipment.service.FbaShipmentService;
import com.musheng.common.context.ShopContext;
import com.musheng.common.exception.BusinessException;
import com.musheng.common.result.ErrorCode;
import com.musheng.config.importrecord.entity.ImportRecord;
import com.musheng.config.importrecord.mapper.ImportRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * FBA货件服务实现
 * 实现货件的导入、查询、删除等业务功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FbaShipmentServiceImpl implements FbaShipmentService {

    private final FbaShipmentMapper fbaShipmentMapper;
    private final FbaShipmentItemMapper fbaShipmentItemMapper;
    private final ImportRecordMapper importRecordMapper;
    private final FbaShipmentExcelParser excelParser;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importData(MultipartFile file) {
        log.info("导入FBA货件明细: fileName={}, size={} bytes",
                file.getOriginalFilename(), file.getSize());

        // 获取当前店铺ID
        Long shopId = ShopContext.requireShopId();

        // 计算文件哈希值（仅用于记录，不用于阻止导入）
        String fileHash = calculateFileHash(file);

        Map<String, Object> result = new HashMap<>();
        List<String> errors = new ArrayList<>();
        int totalSkuCount = 0;
        int successSkuCount = 0;
        int failSkuCount = 0;
        int duplicateSkuCount = 0;  // 重复的SKU数量
        int duplicateShipmentCount = 0;  // 重复的货件数量
        int successShipmentCount = 0;

        // 创建导入记录
        ImportRecord importRecord = new ImportRecord();
        importRecord.setShopId(shopId);
        importRecord.setBatchNo(generateBatchNo());
        importRecord.setDataType("fba_shipment");
        importRecord.setFileName(file.getOriginalFilename());
        importRecord.setFileSize(file.getSize());
        importRecord.setFileHash(fileHash);
        importRecord.setImportStatus("processing");
        importRecordMapper.insert(importRecord);

        try {
            // Step 1: 解析Excel文件
            log.info("开始解析Excel文件...");
            List<FbaShipment> shipments = excelParser.parseExcel(file, shopId, importRecord.getId());
            log.info("Excel解析完成: 共{}个货件", shipments.size());

            // Step 2: 批量检测重复货件
            Set<String> existingShipmentIds = batchCheckDuplicates(shipments, shopId);
            log.info("重复检测完成: 找到{}个已存在的货件", existingShipmentIds.size());

            // Step 3: 保存数据
            for (FbaShipment shipment : shipments) {
                totalSkuCount += shipment.getItems().size();

                // 检查是否重复（数据级别幂等）
                if (existingShipmentIds.contains(shipment.getShipmentId())) {
                    duplicateShipmentCount++;
                    duplicateSkuCount += shipment.getItems().size();
                    log.debug("跳过重复货件: shipmentId={}, skuCount={}",
                            shipment.getShipmentId(), shipment.getItems().size());
                    continue;
                }

                try {
                    // 保存货件主表
                    fbaShipmentMapper.insert(shipment);

                    // 批量保存明细
                    for (FbaShipmentItem item : shipment.getItems()) {
                        item.setShipmentId(shipment.getId());
                        fbaShipmentItemMapper.insert(item);
                    }

                    successShipmentCount++;
                    successSkuCount += shipment.getItems().size();

                    log.debug("保存货件成功: shipmentId={}, skuCount={}",
                            shipment.getShipmentId(), shipment.getItems().size());

                } catch (Exception e) {
                    log.error("保存货件失败: shipmentId={}", shipment.getShipmentId(), e);
                    failSkuCount += shipment.getItems().size();
                    if (errors.size() < 10) {
                        errors.add(String.format("货件保存失败: %s - %s",
                                shipment.getShipmentId(), e.getMessage()));
                    }
                }
            }

            // 更新导入记录
            importRecord.setTotalCount(totalSkuCount);
            importRecord.setSuccessCount(successSkuCount);
            importRecord.setFailCount(failSkuCount);
            importRecord.setImportStatus(
                    failSkuCount == 0 ? "success" : (successSkuCount > 0 ? "partial" : "fail")
            );
            importRecord.setCompleteTime(LocalDateTime.now());
            if (!errors.isEmpty()) {
                importRecord.setErrorMessage(String.join("\n", errors));
            }
            importRecordMapper.updateById(importRecord);

        } catch (BusinessException e) {
            importRecord.setImportStatus("fail");
            importRecord.setErrorMessage(e.getMessage());
            importRecord.setCompleteTime(LocalDateTime.now());
            importRecordMapper.updateById(importRecord);
            throw e;
        } catch (Exception e) {
            log.error("导入FBA货件明细失败", e);
            importRecord.setImportStatus("fail");
            importRecord.setErrorMessage(e.getMessage());
            importRecord.setCompleteTime(LocalDateTime.now());
            importRecordMapper.updateById(importRecord);
            throw new BusinessException(ErrorCode.IMPORT_PARSE_ERROR, "导入失败: " + e.getMessage());
        }

        result.put("batchNo", importRecord.getBatchNo());
        result.put("totalCount", totalSkuCount);
        result.put("successCount", successSkuCount);
        result.put("failCount", failSkuCount);
        result.put("duplicateCount", duplicateSkuCount);  // 重复的SKU数量
        result.put("duplicateShipmentCount", duplicateShipmentCount);  // 重复的货件数量
        result.put("shipmentCount", successShipmentCount);
        result.put("errors", errors);

        log.info("FBA货件导入完成: totalSku={}, successSku={}, failSku={}, duplicateSku={}, duplicateShipment={}, successShipment={}",
                totalSkuCount, successSkuCount, failSkuCount, duplicateSkuCount, duplicateShipmentCount, successShipmentCount);

        return result;
    }

    @Override
    public Page<FbaShipment> list(String shipmentId, String shopName, String country,
                                  String startDate, String endDate, int page, int size) {
        LambdaQueryWrapper<FbaShipment> wrapper = new LambdaQueryWrapper<>();

        // 店铺数据隔离
        Long shopId = ShopContext.requireShopId();
        wrapper.eq(FbaShipment::getShopId, shopId);

        // 条件筛选
        if (StringUtils.hasText(shipmentId)) {
            wrapper.like(FbaShipment::getShipmentId, shipmentId);
        }
        if (StringUtils.hasText(shopName)) {
            wrapper.like(FbaShipment::getShopName, shopName);
        }
        if (StringUtils.hasText(country)) {
            wrapper.eq(FbaShipment::getCountry, country);
        }
        if (StringUtils.hasText(startDate)) {
            try {
                LocalDate start = LocalDate.parse(startDate);
                wrapper.ge(FbaShipment::getCreatedDate, start.atStartOfDay());
            } catch (Exception e) {
                log.warn("开始日期格式错误: {}", startDate);
            }
        }
        if (StringUtils.hasText(endDate)) {
            try {
                LocalDate end = LocalDate.parse(endDate);
                wrapper.le(FbaShipment::getCreatedDate, end.atTime(23, 59, 59));
            } catch (Exception e) {
                log.warn("结束日期格式错误: {}", endDate);
            }
        }

        wrapper.orderByDesc(FbaShipment::getCreatedDate);

        return fbaShipmentMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public FbaShipment getById(Long id) {
        FbaShipment shipment = fbaShipmentMapper.selectById(id);
        if (shipment == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST, "货件不存在");
        }

        // 查询关联的SKU明细
        LambdaQueryWrapper<FbaShipmentItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(FbaShipmentItem::getShipmentId, id);
        itemWrapper.orderByAsc(FbaShipmentItem::getSku);
        List<FbaShipmentItem> items = fbaShipmentItemMapper.selectList(itemWrapper);
        shipment.setItems(items);

        return shipment;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        FbaShipment shipment = fbaShipmentMapper.selectById(id);
        if (shipment == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST, "货件不存在");
        }

        // 删除明细
        LambdaQueryWrapper<FbaShipmentItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(FbaShipmentItem::getShipmentId, id);
        fbaShipmentItemMapper.delete(itemWrapper);

        // 删除主表
        fbaShipmentMapper.deleteById(id);

        log.info("删除FBA货件: id={}, shipmentId={}", id, shipment.getShipmentId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        for (Long id : ids) {
            delete(id);
        }

        log.info("批量删除FBA货件: count={}", ids.size());
    }

    @Override
    public Map<String, Object> getSummary(String shopName, String country,
                                         String startDate, String endDate) {
        LambdaQueryWrapper<FbaShipment> wrapper = new LambdaQueryWrapper<>();

        // 店铺数据隔离
        Long shopId = ShopContext.requireShopId();
        wrapper.eq(FbaShipment::getShopId, shopId);

        // 条件筛选
        if (StringUtils.hasText(shopName)) {
            wrapper.like(FbaShipment::getShopName, shopName);
        }
        if (StringUtils.hasText(country)) {
            wrapper.eq(FbaShipment::getCountry, country);
        }
        if (StringUtils.hasText(startDate)) {
            try {
                LocalDate start = LocalDate.parse(startDate);
                wrapper.ge(FbaShipment::getCreatedDate, start.atStartOfDay());
            } catch (Exception e) {
                log.warn("开始日期格式错误: {}", startDate);
            }
        }
        if (StringUtils.hasText(endDate)) {
            try {
                LocalDate end = LocalDate.parse(endDate);
                wrapper.le(FbaShipment::getCreatedDate, end.atTime(23, 59, 59));
            } catch (Exception e) {
                log.warn("结束日期格式错误: {}", endDate);
            }
        }

        List<FbaShipment> shipments = fbaShipmentMapper.selectList(wrapper);

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalShipments", shipments.size());
        summary.put("totalSkuCount", shipments.stream()
                .mapToInt(s -> s.getSkuCount() != null ? s.getSkuCount() : 0).sum());
        summary.put("totalQuantity", shipments.stream()
                .mapToInt(s -> s.getTotalQuantity() != null ? s.getTotalQuantity() : 0).sum());

        return summary;
    }

    @Override
    public void exportData(String shopName, String country, String startDate, String endDate,
                          jakarta.servlet.http.HttpServletResponse response) {
        LambdaQueryWrapper<FbaShipment> wrapper = new LambdaQueryWrapper<>();

        // 店铺数据隔离
        Long shopId = ShopContext.requireShopId();
        wrapper.eq(FbaShipment::getShopId, shopId);

        // 条件筛选
        if (StringUtils.hasText(shopName)) {
            wrapper.like(FbaShipment::getShopName, shopName);
        }
        if (StringUtils.hasText(country)) {
            wrapper.eq(FbaShipment::getCountry, country);
        }
        if (StringUtils.hasText(startDate)) {
            try {
                LocalDate start = LocalDate.parse(startDate);
                wrapper.ge(FbaShipment::getCreatedDate, start.atStartOfDay());
            } catch (Exception e) {
                log.warn("开始日期格式错误: {}", startDate);
            }
        }
        if (StringUtils.hasText(endDate)) {
            try {
                LocalDate end = LocalDate.parse(endDate);
                wrapper.le(FbaShipment::getCreatedDate, end.atTime(23, 59, 59));
            } catch (Exception e) {
                log.warn("结束日期格式错误: {}", endDate);
            }
        }

        wrapper.orderByDesc(FbaShipment::getCreatedDate);
        List<FbaShipment> shipments = fbaShipmentMapper.selectList(wrapper);

        try {
            String fileName = "fba_shipment_" + System.currentTimeMillis() + ".xlsx";
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=" +
                    java.net.URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8));

            try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
                 java.io.OutputStream outputStream = response.getOutputStream()) {

                org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("FBA货件");

                // 创建表头
                org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
                String[] headers = {"货件单号", "物流中心编码", "店铺名称", "国家",
                        "创建时间", "SKU种类数", "总发货量"};
                for (int i = 0; i < headers.length; i++) {
                    headerRow.createCell(i).setCellValue(headers[i]);
                }

                // 填充数据
                int rowNum = 1;
                for (FbaShipment shipment : shipments) {
                    org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(shipment.getShipmentId());
                    row.createCell(1).setCellValue(shipment.getWarehouseCode());
                    row.createCell(2).setCellValue(shipment.getShopName());
                    row.createCell(3).setCellValue(shipment.getCountry());
                    row.createCell(4).setCellValue(shipment.getCreatedDate() != null ?
                            shipment.getCreatedDate().toString() : "");
                    row.createCell(5).setCellValue(shipment.getSkuCount() != null ? shipment.getSkuCount() : 0);
                    row.createCell(6).setCellValue(shipment.getTotalQuantity() != null ?
                            shipment.getTotalQuantity() : 0);
                }

                workbook.write(outputStream);
                outputStream.flush();
            }
        } catch (java.io.IOException e) {
            log.error("导出FBA货件失败", e);
            throw new BusinessException(ErrorCode.EXPORT_FAILED, "导出失败: " + e.getMessage());
        }
    }

    /**
     * 批量检测重复货件
     */
    private Set<String> batchCheckDuplicates(List<FbaShipment> shipments, Long shopId) {
        if (shipments.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> existingIds = new HashSet<>();

        // 收集所有货件单号
        Set<String> shipmentIds = new HashSet<>();
        for (FbaShipment shipment : shipments) {
            if (StringUtils.hasText(shipment.getShipmentId())) {
                shipmentIds.add(shipment.getShipmentId());
            }
        }

        if (shipmentIds.isEmpty()) {
            return existingIds;
        }

        // 批量查询已存在的货件
        LambdaQueryWrapper<FbaShipment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FbaShipment::getShopId, shopId)
                .in(FbaShipment::getShipmentId, shipmentIds)
                .select(FbaShipment::getShipmentId);

        List<FbaShipment> existing = fbaShipmentMapper.selectList(wrapper);

        for (FbaShipment shipment : existing) {
            existingIds.add(shipment.getShipmentId());
        }

        return existingIds;
    }

    /**
     * 生成唯一批次号
     */
    private String generateBatchNo() {
        return "FBA-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Override
    public List<String> getCountryList() {
        Long shopId = ShopContext.requireShopId();

        LambdaQueryWrapper<FbaShipment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FbaShipment::getShopId, shopId)
                .select(FbaShipment::getCountry)
                .groupBy(FbaShipment::getCountry)
                .orderByAsc(FbaShipment::getCountry);

        List<FbaShipment> list = fbaShipmentMapper.selectList(wrapper);
        return list.stream()
                .map(FbaShipment::getCountry)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getShopNameList() {
        Long shopId = ShopContext.requireShopId();

        LambdaQueryWrapper<FbaShipment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FbaShipment::getShopId, shopId)
                .select(FbaShipment::getShopName)
                .groupBy(FbaShipment::getShopName)
                .orderByAsc(FbaShipment::getShopName);

        List<FbaShipment> list = fbaShipmentMapper.selectList(wrapper);
        return list.stream()
                .map(FbaShipment::getShopName)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 计算文件哈希值（MD5）
     * 用于记录文件标识，不用于阻止导入
     */
    private String calculateFileHash(MultipartFile file) {
        try {
            byte[] fileBytes = file.getBytes();
            return DigestUtils.md5DigestAsHex(fileBytes);
        } catch (IOException e) {
            log.error("计算文件哈希值失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "计算文件哈希值失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> batchImportData(List<MultipartFile> files) {
        log.info("批量导入FBA货件明细: 文件数={}", files.size());

        Long shopId = ShopContext.requireShopId();

        Map<String, Object> batchResult = new HashMap<>();
        List<Map<String, Object>> fileResults = new ArrayList<>();

        int totalFiles = files.size();
        int successFiles = 0;
        int failFiles = 0;

        int totalSkuCount = 0;
        int successSkuCount = 0;
        int failSkuCount = 0;
        int duplicateSkuCount = 0;
        int totalShipmentCount = 0;

        for (MultipartFile file : files) {
            Map<String, Object> fileResult = new HashMap<>();
            fileResult.put("fileName", file.getOriginalFilename());

            try {
                // 导入文件（数据级别幂等，自动跳过重复记录）
                Map<String, Object> importResult = importData(file);

                fileResult.put("status", "success");
                fileResult.put("result", importResult);

                // 累加统计
                totalSkuCount += (Integer) importResult.getOrDefault("totalCount", 0);
                successSkuCount += (Integer) importResult.getOrDefault("successCount", 0);
                failSkuCount += (Integer) importResult.getOrDefault("failCount", 0);
                duplicateSkuCount += (Integer) importResult.getOrDefault("duplicateCount", 0);
                totalShipmentCount += (Integer) importResult.getOrDefault("shipmentCount", 0);

                successFiles++;
            } catch (Exception e) {
                log.error("导入文件失败: {}", file.getOriginalFilename(), e);
                fileResult.put("status", "fail");
                fileResult.put("message", e.getMessage());
                failFiles++;
            }

            fileResults.add(fileResult);
        }

        batchResult.put("totalFiles", totalFiles);
        batchResult.put("successFiles", successFiles);
        batchResult.put("failFiles", failFiles);
        batchResult.put("totalSkuCount", totalSkuCount);
        batchResult.put("successSkuCount", successSkuCount);
        batchResult.put("failSkuCount", failSkuCount);
        batchResult.put("duplicateSkuCount", duplicateSkuCount);
        batchResult.put("totalShipmentCount", totalShipmentCount);
        batchResult.put("fileResults", fileResults);

        log.info("批量导入完成: 总文件={}, 成功={}, 失败={}, 总SKU={}, 成功SKU={}, 重复SKU={}",
                totalFiles, successFiles, failFiles, totalSkuCount, successSkuCount, duplicateSkuCount);

        return batchResult;
    }
}
