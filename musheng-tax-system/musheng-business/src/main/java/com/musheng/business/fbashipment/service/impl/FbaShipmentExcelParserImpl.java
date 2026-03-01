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

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * FBA货件文件解析服务（V2）
 * 支持CSV和Excel格式，适配新列名和分组数据格式
 */
@Slf4j
@Service
public class FbaShipmentExcelParserImpl implements FbaShipmentExcelParser {

    private static final String COL_SHIPMENT_NO = "货件单号";
    private static final String COL_SHIPMENT_NAME = "货件名称";
    private static final String COL_STATUS = "货件状态";
    private static final String COL_CREATED_DATE = "创建时间";
    private static final String COL_UPDATED_DATE = "更新时间";
    private static final String COL_MSKU = "MSKU";
    private static final String COL_DECLARED_QTY = "申报量";
    private static final String COL_RECEIVED_QTY = "签收量";
    private static final String COL_RECIPIENT = "收件人";
    private static final String COL_WAREHOUSE_CODE = "物流中心编码";
    private static final String COL_POSTAL_CODE = "收件邮编";
    private static final String COL_COUNTRY = "收件国家";
    private static final String COL_STATE = "收件州/省";
    private static final String COL_CITY = "收件城市";
    private static final String COL_STREET = "收件街道地址";
    private static final String COL_HOUSE_NUMBER = "收件门牌号";

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public List<FbaShipment> parseExcel(MultipartFile file, Long shopId, Long importBatchId) throws Exception {
        String fileName = file.getOriginalFilename();
        log.info("开始解析文件: fileName={}, size={}", fileName, file.getSize());

        List<String[]> rows;
        if (fileName != null && fileName.toLowerCase().endsWith(".csv")) {
            rows = parseCsvRows(file);
        } else {
            rows = parseExcelRows(file);
        }

        if (rows.isEmpty()) {
            throw new BusinessException(ErrorCode.IMPORT_PARSE_ERROR, "文件内容为空");
        }

        Map<String, Integer> colMap = buildColumnIndex(rows.get(0));
        log.info("表头列名: {}", colMap.keySet());
        validateRequired(colMap);

        Map<String, ShipmentBuilder> shipmentMap = new LinkedHashMap<>();
        ShipmentBuilder current = null;

        for (int i = 1; i < rows.size(); i++) {
            String[] row = rows.get(i);
            String shipmentNo = val(row, colMap, COL_SHIPMENT_NO);

            if (StringUtils.hasText(shipmentNo)) {
                current = new ShipmentBuilder();
                current.shopId = shopId;
                current.importBatchId = importBatchId;
                current.shipmentNo = shipmentNo;
                current.shipmentName = val(row, colMap, COL_SHIPMENT_NAME);
                current.status = val(row, colMap, COL_STATUS);
                current.createdDate = parseDate(val(row, colMap, COL_CREATED_DATE));
                current.updatedDate = parseDate(val(row, colMap, COL_UPDATED_DATE));
                current.recipient = val(row, colMap, COL_RECIPIENT);
                current.warehouseCode = val(row, colMap, COL_WAREHOUSE_CODE);
                current.postalCode = val(row, colMap, COL_POSTAL_CODE);
                current.country = val(row, colMap, COL_COUNTRY);
                current.state = val(row, colMap, COL_STATE);
                current.city = val(row, colMap, COL_CITY);
                current.street = val(row, colMap, COL_STREET);
                current.houseNumber = val(row, colMap, COL_HOUSE_NUMBER);
                shipmentMap.put(shipmentNo, current);
            }

            if (current == null) continue;

            String msku = val(row, colMap, COL_MSKU);
            if (!StringUtils.hasText(msku)) continue;

            current.addItem(msku,
                intVal(val(row, colMap, COL_DECLARED_QTY)),
                intVal(val(row, colMap, COL_RECEIVED_QTY)));
        }

        List<FbaShipment> result = new ArrayList<>();
        for (ShipmentBuilder b : shipmentMap.values()) {
            result.add(b.build());
        }

        log.info("解析完成: {}个货件, {}个MSKU",
            result.size(), result.stream().mapToInt(s -> s.getItems().size()).sum());
        return result;
    }

    // ========== CSV解析 ==========

    private List<String[]> parseCsvRows(MultipartFile file) throws Exception {
        List<String[]> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                rows.add(splitCsvLine(line));
            }
        }
        return rows;
    }

    private String[] splitCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        fields.add(sb.toString().trim());
        return fields.toArray(new String[0]);
    }

    // ========== Excel解析 ==========

    private List<String[]> parseExcelRows(MultipartFile file) throws Exception {
        List<String[]> rows = new ArrayList<>();
        try (InputStream is = file.getInputStream(); Workbook wb = new XSSFWorkbook(is)) {
            if (wb.getNumberOfSheets() == 0) {
                throw new BusinessException(ErrorCode.IMPORT_PARSE_ERROR, "Excel文件中没有Sheet");
            }
            Sheet sheet = wb.getSheetAt(0);
            log.info("使用Sheet: {}", sheet.getSheetName());

            int maxCol = 0;
            Row header = sheet.getRow(0);
            if (header != null) maxCol = header.getPhysicalNumberOfCells();

            for (int i = 0; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                int len = Math.max(maxCol, row.getLastCellNum());
                String[] cells = new String[len];
                for (int j = 0; j < len; j++) {
                    cells[j] = cellStr(row.getCell(j));
                }
                rows.add(cells);
            }
        }
        return rows;
    }

    private String cellStr(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDateTime().format(DATE_FMT);
                }
                double v = cell.getNumericCellValue();
                return v == (long) v ? String.valueOf((long) v) : String.valueOf(v);
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try { return cell.getStringCellValue().trim(); }
                catch (Exception e) { return String.valueOf(cell.getNumericCellValue()); }
            default: return "";
        }
    }

    // ========== 工具方法 ==========

    private Map<String, Integer> buildColumnIndex(String[] header) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < header.length; i++) {
            String name = header[i] == null ? "" : header[i].trim();
            if (!name.isEmpty()) map.put(name, i);
        }
        return map;
    }

    private void validateRequired(Map<String, Integer> colMap) {
        List<String> missing = new ArrayList<>();
        for (String col : Arrays.asList(COL_SHIPMENT_NO, COL_MSKU)) {
            if (!colMap.containsKey(col)) missing.add(col);
        }
        if (!missing.isEmpty()) {
            throw new BusinessException(ErrorCode.IMPORT_PARSE_ERROR,
                "缺少必需列: " + String.join(", ", missing) + "。实际列名: " + colMap.keySet());
        }
    }

    private String val(String[] row, Map<String, Integer> colMap, String colName) {
        Integer idx = colMap.get(colName);
        if (idx == null || idx >= row.length) return "";
        return row[idx] == null ? "" : row[idx].trim();
    }

    private LocalDateTime parseDate(String s) {
        if (!StringUtils.hasText(s)) return null;
        try { return LocalDateTime.parse(s, DATE_FMT); }
        catch (Exception e) {
            try { return LocalDateTime.parse(s.replace(" ", "T")); }
            catch (Exception e2) { log.warn("日期解析失败: {}", s); return null; }
        }
    }

    private int intVal(String s) {
        if (!StringUtils.hasText(s)) return 0;
        try { return Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { return 0; }
    }

    // ========== 内部构建器 ==========

    private static class ShipmentBuilder {
        Long shopId, importBatchId;
        String shipmentNo, shipmentName, status, recipient, warehouseCode;
        String postalCode, country, state, city, street, houseNumber;
        LocalDateTime createdDate, updatedDate;
        List<ItemData> items = new ArrayList<>();

        void addItem(String msku, int declared, int received) {
            ItemData d = new ItemData();
            d.msku = msku; d.declared = declared; d.received = received;
            items.add(d);
        }

        FbaShipment build() {
            FbaShipment s = new FbaShipment();
            s.setShopId(shopId);
            s.setImportBatchId(importBatchId);
            s.setShipmentId(shipmentNo);
            s.setShipmentName(shipmentName);
            s.setStatus(status);
            s.setCreatedDate(createdDate);
            s.setUpdatedDate(updatedDate);
            s.setRecipient(recipient);
            s.setWarehouseCode(warehouseCode);
            s.setPostalCode(postalCode);
            s.setCountry(country);
            s.setState(state);
            s.setCity(city);
            s.setStreetAddress(street);
            s.setHouseNumber(houseNumber);
            s.setSkuCount(items.size());
            s.setTotalQuantity(items.stream().mapToInt(i -> i.declared).sum());
            s.setTotalReceivedQuantity(items.stream().mapToInt(i -> i.received).sum());

            List<FbaShipmentItem> itemList = new ArrayList<>();
            for (ItemData d : items) {
                FbaShipmentItem item = new FbaShipmentItem();
                item.setShopId(shopId);
                item.setShipmentNo(shipmentNo);
                item.setMsku(d.msku);
                item.setQuantity(d.declared);
                item.setReceivedQuantity(d.received);
                item.setImportBatchId(importBatchId);
                // sku 可选，新格式以 MSKU 为主
                item.setSku(null);
                itemList.add(item);
            }
            s.setItems(itemList);
            return s;
        }
    }

    private static class ItemData {
        String msku;
        int declared, received;
    }
}
