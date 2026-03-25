package com.musheng.business.fbashipment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.business.fbashipment.entity.FbaShipment;
import com.musheng.business.fbashipment.entity.FbaShipmentItem;
import com.musheng.business.fbashipment.mapper.FbaShipmentItemMapper;
import com.musheng.business.fbashipment.repository.FbaShipmentRepository;
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
import java.time.LocalDateTime;
import java.util.*;

/**
 * FBA货件服务实现
 * 
 * 实现货件的导入、查询、删除等业务功能。
 * 
 * ⚠️ 核心原则：
 * 1. 禁止修改业务流程
 * 2. 禁止改变输出结果
 * 3. 只是将 Mapper 调用替换为 Repository 调用
 *
 * @author wanhua
 * 10:30 2026年02月02日
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FbaShipmentServiceImpl implements FbaShipmentService {

    private final FbaShipmentRepository fbaShipmentRepository;
    private final FbaShipmentItemMapper fbaShipmentItemMapper;
    private final ImportRecordMapper importRecordMapper;
    private final FbaShipmentExcelParser excelParser;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importData(MultipartFile file) {
        return importData(file, null);
    }

    /**
     * 导入Excel文件（带站点代码）
     *
     * @param file Excel文件
     * @param siteCode 站点代码（如 US/CA/UK/DE），可为空
     * @return 导入结果
     * @author wanhua
     * 10:30 2026年03月07日
     */
    private Map<String, Object> importData(MultipartFile file, String siteCode) {
        log.info("导入FBA货件明细: fileName={}, size={} bytes, siteCode={}",
                file.getOriginalFilename(), file.getSize(), siteCode);

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

            // Step 2: 批量检测重复货件 - 使用 Repository
            Set<String> shipmentIds = new HashSet<>();
            for (FbaShipment shipment : shipments) {
                if (StringUtils.hasText(shipment.getShipmentId())) {
                    shipmentIds.add(shipment.getShipmentId());
                }
            }
            Set<String> existingShipmentIds = fbaShipmentRepository.findExistingShipmentIds(shipmentIds);
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
                    // 设置站点代码
                    shipment.setSiteCode(siteCode);

                    // 保存货件主表 - 使用 Repository
                    fbaShipmentRepository.save(shipment);

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
    public Page<FbaShipment> list(String shipmentId, String status, String shopName, String country,
                                  String siteCode, String startDate, String endDate, int page, int size) {
        return fbaShipmentRepository.findByQuery(shipmentId, status, shopName, country, siteCode, startDate, endDate, page, size);
    }

    @Override
    public FbaShipment getById(Long id) {
        // 使用 Repository 查询
        FbaShipment shipment = fbaShipmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_EXIST, "货件不存在"));

        // 校验店铺数据隔离
        Long shopId = ShopContext.requireShopId();
        if (!shopId.equals(shipment.getShopId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该数据");
        }

        // 查询关联的SKU明细
        LambdaQueryWrapper<FbaShipmentItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(FbaShipmentItem::getShipmentId, id);
        itemWrapper.orderByAsc(FbaShipmentItem::getMsku);
        List<FbaShipmentItem> items = fbaShipmentItemMapper.selectList(itemWrapper);
        shipment.setItems(items);

        return shipment;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        // 使用 Repository 查询
        FbaShipment shipment = fbaShipmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_EXIST, "货件不存在"));

        // 校验店铺数据隔离
        Long shopId = ShopContext.requireShopId();
        if (!shopId.equals(shipment.getShopId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该数据");
        }

        // 删除明细
        LambdaQueryWrapper<FbaShipmentItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(FbaShipmentItem::getShipmentId, id);
        fbaShipmentItemMapper.delete(itemWrapper);

        // 删除主表 - 使用 Repository
        fbaShipmentRepository.deleteById(id);

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
    public Map<String, Object> getSummary(String status, String shopName, String country,
                                         String startDate, String endDate) {
        List<FbaShipment> shipments = fbaShipmentRepository.findListByQuery(status, shopName, country, startDate, endDate);

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalShipments", shipments.size());
        summary.put("totalSkuCount", shipments.stream()
                .mapToInt(s -> s.getSkuCount() != null ? s.getSkuCount() : 0).sum());
        summary.put("totalQuantity", shipments.stream()
                .mapToInt(s -> s.getTotalQuantity() != null ? s.getTotalQuantity() : 0).sum());

        return summary;
    }

    @Override
    public void exportData(String status, String shopName, String country, String startDate, String endDate,
                          jakarta.servlet.http.HttpServletResponse response) {
        List<FbaShipment> shipments = fbaShipmentRepository.findListByQuery(status, shopName, country, startDate, endDate);

        try {
            String fileName = "慕声FBA发货明细数据_" + System.currentTimeMillis() + ".csv";
            response.setContentType("text/csv; charset=UTF-8");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=" +
                    java.net.URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8));

            java.io.OutputStream outputStream = response.getOutputStream();
            java.io.OutputStreamWriter writer = new java.io.OutputStreamWriter(outputStream, java.nio.charset.StandardCharsets.UTF_8);
            java.io.BufferedWriter bw = new java.io.BufferedWriter(writer);

            // BOM for Excel UTF-8
            bw.write('\ufeff');

            // 表头（与导入文档格式一致）
            bw.write("货件单号,货件名称,货件状态,创建时间,更新时间,MSKU,申报量,签收量,收件人,物流中心编码,收件邮编,收件国家,收件州/省,收件城市,收件街道地址,收件门牌号");
            bw.newLine();

            java.time.format.DateTimeFormatter dateFmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

            for (FbaShipment shipment : shipments) {
                // 加载明细
                LambdaQueryWrapper<FbaShipmentItem> itemWrapper = new LambdaQueryWrapper<>();
                itemWrapper.eq(FbaShipmentItem::getShipmentId, shipment.getId());
                itemWrapper.orderByAsc(FbaShipmentItem::getMsku);
                List<FbaShipmentItem> items = fbaShipmentItemMapper.selectList(itemWrapper);

                String shipmentNo = shipment.getShipmentId() != null ? shipment.getShipmentId() : "";
                String shipmentName = shipment.getShipmentName() != null ? shipment.getShipmentName() : "";
                String shipmentStatus = shipment.getStatus() != null ? shipment.getStatus() : "";
                String createdStr = shipment.getCreatedDate() != null ? shipment.getCreatedDate().format(dateFmt) : "";
                String updatedStr = shipment.getUpdatedDate() != null ? shipment.getUpdatedDate().format(dateFmt) : "";
                String recipient = shipment.getRecipient() != null ? escapeCsv(shipment.getRecipient()) : "";
                String warehouseCode = shipment.getWarehouseCode() != null ? shipment.getWarehouseCode() : "";
                String postalCode = shipment.getPostalCode() != null ? shipment.getPostalCode() : "";
                String countryStr = shipment.getCountry() != null ? shipment.getCountry() : "";
                String state = shipment.getState() != null ? shipment.getState() : "";
                String city = shipment.getCity() != null ? shipment.getCity() : "";
                String street = shipment.getStreetAddress() != null ? escapeCsv(shipment.getStreetAddress()) : "";
                String houseNumber = shipment.getHouseNumber() != null ? shipment.getHouseNumber() : "";

                for (int i = 0; i < items.size(); i++) {
                    FbaShipmentItem item = items.get(i);
                    StringBuilder row = new StringBuilder();
                    if (i == 0) {
                        row.append(shipmentNo).append(",").append(shipmentName).append(",").append(shipmentStatus)
                                .append(",").append(createdStr).append(",").append(updatedStr);
                    } else {
                        row.append(",,,,,");
                    }
                    row.append(",").append(item.getMsku() != null ? item.getMsku() : "")
                            .append(",").append(item.getQuantity() != null ? item.getQuantity() : 0)
                            .append(",").append(item.getReceivedQuantity() != null ? item.getReceivedQuantity() : 0);
                    if (i == 0) {
                        row.append(",").append(recipient).append(",").append(warehouseCode).append(",").append(postalCode)
                                .append(",").append(countryStr).append(",").append(state).append(",").append(city)
                                .append(",").append(street).append(",").append(houseNumber);
                    } else {
                        row.append(",,,,,,,,,");
                    }
                    bw.write(row.toString());
                    bw.newLine();
                }
            }

            bw.flush();
            outputStream.flush();
        } catch (java.io.IOException e) {
            log.error("导出FBA货件失败", e);
            throw new BusinessException(ErrorCode.EXPORT_FAILED, "导出失败: " + e.getMessage());
        }
    }

    private String escapeCsv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    /**
     * 生成唯一批次号
     *
     * @return 批次号
     * @author wanhua
     * 10:30 2026年02月02日
     */
    private String generateBatchNo() {
        return "FBA-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Override
    public List<String> getCountryList() {
        // 使用 Repository 查询
        return fbaShipmentRepository.findDistinctCountries();
    }

    /**
     * 计算文件哈希值（MD5）
     * 用于记录文件标识，不用于阻止导入
     *
     * @param file 上传的文件
     * @return MD5哈希值
     * @author wanhua
     * 10:30 2026年02月02日
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
    public Map<String, Object> batchImportData(List<MultipartFile> files, String siteCode) {
        log.info("批量导入FBA货件明细: 文件数={}, siteCode={}", files.size(), siteCode);

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
                // 导入文件（数据级别幂等，自动跳过重复记录，传入站点代码）
                Map<String, Object> importResult = importData(file, siteCode);

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
