package com.musheng.business.fbashipment.service.impl;

import com.musheng.business.fbashipment.entity.FbaShipment;
import com.musheng.business.fbashipment.entity.FbaShipmentItem;
import com.musheng.business.fbashipment.service.FbaShipmentExcelParser;
import com.musheng.common.exception.BusinessException;
import com.musheng.common.result.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * FBA货件Excel解析服务实现
 * 解析Excel文件（Sheet: 发货单详情）并转换为货件和明细对象
 */
@Slf4j
@Service
public class FbaShipmentExcelParserImpl implements FbaShipmentExcelParser {

    private static final String SHEET_NAME = "发货单详情";

    // Excel列名映射
    private static final String COL_WAREHOUSE_CODE = "物流中心编码";
    private static final String COL_CREATED_DATE = "创建时间";
    private static final String COL_SKU = "SKU";
    private static final String COL_SHOP_NAME = "店铺";
    private static final String COL_COUNTRY = "国家";
    private static final String COL_MSKU = "MSKU";
    private static final String COL_SHIPMENT_NO = "货件单号";
    private static final String COL_QUANTITY = "发货量";

    @Override
    public List<FbaShipment> parseExcel(MultipartFile file, Long shopId, Long importBatchId) throws Exception {
        log.info("开始解析Excel文件: fileName={}, size={}", file.getOriginalFilename(), file.getSize());

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            // 获取指定Sheet
            Sheet sheet = workbook.getSheet(SHEET_NAME);
            if (sheet == null) {
                throw new BusinessException(ErrorCode.IMPORT_PARSE_ERROR,
                    "未找到Sheet: " + SHEET_NAME);
            }

            // 解析表头
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new BusinessException(ErrorCode.IMPORT_PARSE_ERROR, "Excel文件表头为空");
            }

            Map<String, Integer> columnIndexMap = parseHeader(headerRow);
            log.info("表头解析完成: {}", columnIndexMap);

            // 解析数据行
            Map<String, ShipmentData> shipmentDataMap = new LinkedHashMap<>();
            int totalRows = sheet.getPhysicalNumberOfRows();

            for (int rowIndex = 1; rowIndex < totalRows; rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }

                try {
                    parseRow(row, columnIndexMap, shipmentDataMap, shopId, importBatchId);
                } catch (Exception e) {
                    log.warn("解析第{}行失败: {}", rowIndex + 1, e.getMessage());
                    throw new BusinessException(ErrorCode.IMPORT_PARSE_ERROR,
                        String.format("第%d行解析失败: %s", rowIndex + 1, e.getMessage()));
                }
            }

            // 转换为FbaShipment对象列表
            List<FbaShipment> shipments = convertToShipments(shipmentDataMap);
            log.info("Excel解析完成: 共{}个货件, {}个SKU",
                shipments.size(),
                shipments.stream().mapToInt(s -> s.getItems().size()).sum());

            return shipments;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("解析Excel文件失败", e);
            throw new BusinessException(ErrorCode.IMPORT_PARSE_ERROR,
                "解析Excel文件失败: " + e.getMessage());
        }
    }

    /**
     * 解析表头，建立列名到索引的映射
     */
    private Map<String, Integer> parseHeader(Row headerRow) {
        Map<String, Integer> columnIndexMap = new HashMap<>();

        for (int i = 0; i < headerRow.getPhysicalNumberOfCells(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell != null) {
                String columnName = getCellValueAsString(cell).trim();
                if (StringUtils.hasText(columnName)) {
                    columnIndexMap.put(columnName, i);
                }
            }
        }

        // 验证必需列是否存在
        List<String> requiredColumns = Arrays.asList(
            COL_SKU, COL_SHOP_NAME, COL_COUNTRY, COL_MSKU, COL_SHIPMENT_NO, COL_QUANTITY
        );

        for (String col : requiredColumns) {
            if (!columnIndexMap.containsKey(col)) {
                throw new BusinessException(ErrorCode.IMPORT_PARSE_ERROR,
                    "缺少必需列: " + col);
            }
        }

        return columnIndexMap;
    }

    /**
     * 解析单行数据
     */
    private void parseRow(Row row, Map<String, Integer> columnIndexMap,
                         Map<String, ShipmentData> shipmentDataMap,
                         Long shopId, Long importBatchId) {

        // 读取货件单号（必填）
        String shipmentNo = getCellValue(row, columnIndexMap, COL_SHIPMENT_NO);
        if (!StringUtils.hasText(shipmentNo)) {
            return; // 跳过空行
        }

        // 获取或创建货件数据
        ShipmentData shipmentData = shipmentDataMap.computeIfAbsent(shipmentNo, k -> {
            ShipmentData data = new ShipmentData();
            data.setShipmentNo(shipmentNo);
            data.setShopId(shopId);
            data.setImportBatchId(importBatchId);
            return data;
        });

        // 读取货件级别信息（只在第一次遇到时设置）
        if (shipmentData.getWarehouseCode() == null) {
            String warehouseCode = getCellValue(row, columnIndexMap, COL_WAREHOUSE_CODE);
            if (StringUtils.hasText(warehouseCode)) {
                shipmentData.setWarehouseCode(warehouseCode);
            }
        }

        if (shipmentData.getCreatedDate() == null) {
            LocalDateTime createdDate = getCellValueAsDate(row, columnIndexMap, COL_CREATED_DATE);
            if (createdDate != null) {
                shipmentData.setCreatedDate(createdDate);
            }
        }

        // 店铺名称和国家（每行都有）
        String shopName = getCellValue(row, columnIndexMap, COL_SHOP_NAME);
        if (StringUtils.hasText(shopName)) {
            shipmentData.setShopName(shopName);
        }

        String country = getCellValue(row, columnIndexMap, COL_COUNTRY);
        if (StringUtils.hasText(country)) {
            shipmentData.setCountry(country);
        }

        // 读取SKU明细信息
        String sku = getCellValue(row, columnIndexMap, COL_SKU);
        String msku = getCellValue(row, columnIndexMap, COL_MSKU);
        String quantityStr = getCellValue(row, columnIndexMap, COL_QUANTITY);

        if (!StringUtils.hasText(sku)) {
            log.warn("SKU为空，跳过该行");
            return;
        }

        Integer quantity = 0;
        if (StringUtils.hasText(quantityStr)) {
            try {
                quantity = Integer.parseInt(quantityStr.trim());
            } catch (NumberFormatException e) {
                log.warn("发货量解析失败: {}", quantityStr);
            }
        }

        // 创建明细对象
        ItemData itemData = new ItemData();
        itemData.setSku(sku);
        itemData.setMsku(msku);
        itemData.setQuantity(quantity);

        shipmentData.addItem(itemData);
    }

    /**
     * 转换为FbaShipment对象列表
     */
    private List<FbaShipment> convertToShipments(Map<String, ShipmentData> shipmentDataMap) {
        List<FbaShipment> shipments = new ArrayList<>();

        for (ShipmentData data : shipmentDataMap.values()) {
            FbaShipment shipment = new FbaShipment();
            shipment.setShopId(data.getShopId());
            shipment.setShipmentId(data.getShipmentNo());
            shipment.setWarehouseCode(data.getWarehouseCode());
            shipment.setShopName(data.getShopName());
            shipment.setCountry(data.getCountry());
            shipment.setCreatedDate(data.getCreatedDate());
            shipment.setImportBatchId(data.getImportBatchId());

            // 转换明细
            List<FbaShipmentItem> items = new ArrayList<>();
            for (ItemData itemData : data.getItems()) {
                FbaShipmentItem item = new FbaShipmentItem();
                item.setShopId(data.getShopId());
                item.setShipmentNo(data.getShipmentNo());
                item.setSku(itemData.getSku());
                item.setMsku(itemData.getMsku());
                item.setQuantity(itemData.getQuantity());
                item.setImportBatchId(data.getImportBatchId());
                items.add(item);
            }

            shipment.setItems(items);
            shipment.setSkuCount(items.size());
            shipment.setTotalQuantity(items.stream().mapToInt(FbaShipmentItem::getQuantity).sum());

            shipments.add(shipment);
        }

        return shipments;
    }

    /**
     * 获取单元格值（字符串）
     */
    private String getCellValue(Row row, Map<String, Integer> columnIndexMap, String columnName) {
        Integer colIndex = columnIndexMap.get(columnName);
        if (colIndex == null) {
            return "";
        }

        Cell cell = row.getCell(colIndex);
        if (cell == null) {
            return "";
        }

        return getCellValueAsString(cell);
    }

    /**
     * 获取单元格值（日期）
     */
    private LocalDateTime getCellValueAsDate(Row row, Map<String, Integer> columnIndexMap, String columnName) {
        Integer colIndex = columnIndexMap.get(columnName);
        if (colIndex == null) {
            return null;
        }

        Cell cell = row.getCell(colIndex);
        if (cell == null) {
            return null;
        }

        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                Date date = cell.getDateCellValue();
                return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            } else if (cell.getCellType() == CellType.STRING) {
                // 尝试解析字符串日期
                String dateStr = cell.getStringCellValue().trim();
                if (StringUtils.hasText(dateStr)) {
                    // 可以添加多种日期格式解析
                    return LocalDateTime.parse(dateStr.replace(" ", "T"));
                }
            }
        } catch (Exception e) {
            log.warn("日期解析失败: {}", e.getMessage());
        }

        return null;
    }

    /**
     * 将单元格值转换为字符串
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    // 避免科学计数法
                    double value = cell.getNumericCellValue();
                    if (value == (long) value) {
                        return String.valueOf((long) value);
                    } else {
                        return String.valueOf(value);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue().trim();
                } catch (Exception e) {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BLANK:
            case ERROR:
            default:
                return "";
        }
    }

    /**
     * 货件数据临时对象
     */
    private static class ShipmentData {
        private Long shopId;
        private String shipmentNo;
        private String warehouseCode;
        private String shopName;
        private String country;
        private LocalDateTime createdDate;
        private Long importBatchId;
        private List<ItemData> items = new ArrayList<>();

        public Long getShopId() { return shopId; }
        public void setShopId(Long shopId) { this.shopId = shopId; }
        public String getShipmentNo() { return shipmentNo; }
        public void setShipmentNo(String shipmentNo) { this.shipmentNo = shipmentNo; }
        public String getWarehouseCode() { return warehouseCode; }
        public void setWarehouseCode(String warehouseCode) { this.warehouseCode = warehouseCode; }
        public String getShopName() { return shopName; }
        public void setShopName(String shopName) { this.shopName = shopName; }
        public String getCountry() { return country; }
        public void setCountry(String country) { this.country = country; }
        public LocalDateTime getCreatedDate() { return createdDate; }
        public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
        public Long getImportBatchId() { return importBatchId; }
        public void setImportBatchId(Long importBatchId) { this.importBatchId = importBatchId; }
        public List<ItemData> getItems() { return items; }
        public void addItem(ItemData item) { this.items.add(item); }
    }

    /**
     * SKU明细数据临时对象
     */
    private static class ItemData {
        private String sku;
        private String msku;
        private Integer quantity;

        public String getSku() { return sku; }
        public void setSku(String sku) { this.sku = sku; }
        public String getMsku() { return msku; }
        public void setMsku(String msku) { this.msku = msku; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }
}
