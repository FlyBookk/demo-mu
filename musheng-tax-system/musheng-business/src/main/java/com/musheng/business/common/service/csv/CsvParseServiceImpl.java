package com.musheng.business.common.service.csv;

import cn.hutool.core.date.DateUtil;
import com.musheng.business.sales.parser.DateConverter;
import com.musheng.common.exception.BusinessException;
import com.musheng.common.result.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * CSV Parse Service Implementation
 * Handles multi-language CSV files from Amazon (EN/DE)
 */
@Slf4j
@Service
@lombok.RequiredArgsConstructor
public class CsvParseServiceImpl implements CsvParseService {

    private final com.musheng.business.common.config.MarketplaceConfigService marketplaceConfigService;

    /**
     * English header keywords for detection
     */
    private static final List<String> EN_HEADER_KEYWORDS = List.of(
            "date/time", "settlement id", "type", "order id", "marketplace"
    );

    /**
     * German header keywords for detection
     */
    private static final List<String> DE_HEADER_KEYWORDS = List.of(
            "datum/uhrzeit", "abrechnungsnummer", "typ", "bestellnummer", "marketplace"
    );

    /**
     * French header keywords for detection
     */
    private static final List<String> FR_HEADER_KEYWORDS = List.of(
            "date/heure", "type", "sku", "marketplace", "ventes de produits"
    );

    /**
     * Italian header keywords for detection
     */
    private static final List<String> IT_HEADER_KEYWORDS = List.of(
            "data/ora:", "tipo", "sku", "marketplace", "vendite"
    );

    /**
     * Spanish header keywords for detection
     */
    private static final List<String> ES_HEADER_KEYWORDS = List.of(
            "fecha y hora", "tipo", "sku", "web de amazon", "ventas de productos"
    );

    /**
     * Chinese shipping data header keywords for detection
     */
    private static final List<String> CN_SHIPPING_HEADER_KEYWORDS = List.of(
            "亚马逊订单编号", "配送日期", "销售渠道", "卖家 sku", "商品价格"
    );

    /**
     * Chinese FBA shipment header keywords for detection
     */
    private static final List<String> CN_FBA_SHIPMENT_HEADER_KEYWORDS = List.of(
            "货件名称", "货件编号", "已创建", "收货地址", "预计商品数量"
    );

    // MARKETPLACE_SITE_MAP 已移除，改为从 t_marketplace 动态加载（见 marketplaceConfigService.buildDomainToSiteCodeMap()）

    @Override
    public CsvHeaderResult parseHeaders(MultipartFile file) {
        // Try UTF-8 first, then GBK if UTF-8 fails
        CsvHeaderResult result = tryParseWithCharset(file, StandardCharsets.UTF_8);
        if (result == null) {
            log.info("UTF-8 parsing failed, trying GBK encoding...");
            result = tryParseWithCharset(file, java.nio.charset.Charset.forName("GBK"));
        }

        if (result == null) {
            throw new BusinessException(ErrorCode.IMPORT_HEADER_NOT_FOUND, "Cannot detect header with UTF-8 or GBK encoding");
        }

        return result;
    }

    private CsvHeaderResult tryParseWithCharset(MultipartFile file, java.nio.charset.Charset charset) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), charset))) {

            List<String[]> lines = new ArrayList<>();
            String line;
            int maxLines = 50; // Read up to 50 lines to find header

            while ((line = reader.readLine()) != null && lines.size() < maxLines) {
                // Remove BOM if present (UTF-8 BOM is EF BB BF)
                if (lines.isEmpty() && line.startsWith("\uFEFF")) {
                    line = line.substring(1);
                }
                lines.add(parseCsvLine(line));
            }

            // Detect header row
            int headerRowIndex = detectHeaderRow(lines);
            if (headerRowIndex < 0) {
                log.debug("Cannot detect header row with charset: {}", charset.name());
                return null;
            }

            log.info("Successfully detected headers with charset: {}", charset.name());

            String[] headers = lines.get(headerRowIndex);
            String headerLanguage = detectLanguage(headers);
            String siteCode = detectSiteFromHeaders(lines, headerRowIndex);

            // Count total rows
            int totalRows = lines.size();
            while (reader.readLine() != null) {
                totalRows++;
            }

            // Build sample data
            List<Map<String, String>> sampleData = new ArrayList<>();
            int dataStartRow = headerRowIndex + 1;
            for (int i = dataStartRow; i < Math.min(dataStartRow + 5, lines.size()); i++) {
                Map<String, String> rowData = new LinkedHashMap<>();
                String[] row = lines.get(i);
                for (int j = 0; j < Math.min(headers.length, row.length); j++) {
                    rowData.put(headers[j], row[j]);
                }
                sampleData.add(rowData);
            }

            return CsvHeaderResult.builder()
                    .headerRowIndex(headerRowIndex)
                    .headers(Arrays.asList(headers))
                    .detectedSiteCode(siteCode)
                    .headerLanguage(headerLanguage)
                    .totalRows(totalRows)
                    .dataRows(totalRows - headerRowIndex - 1)
                    .sampleData(sampleData)
                    .charset(charset.name())
                    .build();

        } catch (IOException e) {
            log.warn("Failed to parse CSV headers with charset {}: {}", charset.name(), e.getMessage());
            return null;
        }
    }

    @Override
    public String detectMarketplace(MultipartFile file) {
        CsvHeaderResult headerResult = parseHeaders(file);
        return headerResult.getDetectedSiteCode();
    }

    @Override
    public <T> CsvParseResult<T> parseSalesData(MultipartFile file, String siteCode, Class<T> entityClass) {
        // Implementation will be completed in actual business module
        // This is a placeholder for the interface
        throw new UnsupportedOperationException("Sales data parsing should be implemented in SalesDataService");
    }

    @Override
    public <T> CsvParseResult<T> parseShippingData(MultipartFile file, String siteCode, Class<T> entityClass) {
        // Implementation will be completed in actual business module
        throw new UnsupportedOperationException("Shipping data parsing should be implemented in ShippingDataService");
    }

    @Override
    public <T> CsvParseResult<T> parseRateData(MultipartFile file, Class<T> entityClass) {
        // Implementation will be completed in actual business module
        throw new UnsupportedOperationException("Rate data parsing should be implemented in RateService");
    }

    /**
     * Detect header row index
     */
    private int detectHeaderRow(List<String[]> lines) {
        for (int i = 0; i < lines.size(); i++) {
            String lineStr = String.join(",", lines.get(i)).toLowerCase();

            // Check English headers
            boolean allEnMatch = EN_HEADER_KEYWORDS.stream()
                    .allMatch(k -> lineStr.contains(k.toLowerCase()));
            if (allEnMatch) {
                return i;
            }

            // Check German headers
            boolean allDeMatch = DE_HEADER_KEYWORDS.stream()
                    .allMatch(k -> lineStr.contains(k.toLowerCase()));
            if (allDeMatch) {
                return i;
            }

            // Check French headers
            boolean allFrMatch = FR_HEADER_KEYWORDS.stream()
                    .allMatch(k -> lineStr.contains(k.toLowerCase()));
            if (allFrMatch) {
                return i;
            }

            // Check Italian headers
            boolean allItMatch = IT_HEADER_KEYWORDS.stream()
                    .allMatch(k -> lineStr.contains(k.toLowerCase()));
            if (allItMatch) {
                return i;
            }

            // Check Spanish headers
            boolean allEsMatch = ES_HEADER_KEYWORDS.stream()
                    .allMatch(k -> lineStr.contains(k.toLowerCase()));
            if (allEsMatch) {
                return i;
            }

            // Check Chinese shipping headers
            boolean allCnMatch = CN_SHIPPING_HEADER_KEYWORDS.stream()
                    .allMatch(k -> lineStr.contains(k.toLowerCase()));
            if (allCnMatch) {
                return i;
            }

            // Check Chinese FBA shipment headers
            boolean allFbaMatch = CN_FBA_SHIPMENT_HEADER_KEYWORDS.stream()
                    .allMatch(k -> lineStr.contains(k.toLowerCase()));
            if (allFbaMatch) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Detect header language
     */
    private String detectLanguage(String[] headers) {
        String headerStr = String.join(",", headers).toLowerCase();
        if (headerStr.contains("datum/uhrzeit") || headerStr.contains("bestellnummer")) {
            return "DE";
        }
        if (headerStr.contains("date/heure") || headerStr.contains("ventes de produits")) {
            return "FR";
        }
        if (headerStr.contains("data/ora:") || headerStr.contains("vendite")) {
            return "IT";
        }
        if (headerStr.contains("fecha y hora") || headerStr.contains("ventas de productos")) {
            return "ES";
        }
        if (headerStr.contains("亚马逊订单编号") || headerStr.contains("配送日期")) {
            return "CN";
        }
        return "EN";
    }

    /**
     * Detect site code from CSV content
     */
    private String detectSiteFromHeaders(List<String[]> lines, int headerRowIndex) {
        String[] headers = lines.get(headerRowIndex);

        // Find marketplace column index
        int marketplaceIdx = -1;
        for (int i = 0; i < headers.length; i++) {
            String h = headers[i].toLowerCase().trim();
            if (h.equals("marketplace") || h.equals("vertriebskanal") || h.equals("销售渠道")) {
                marketplaceIdx = i;
                break;
            }
        }

        if (marketplaceIdx < 0) {
            return null;
        }

        // Check first few data rows for marketplace value
        for (int i = headerRowIndex + 1; i < Math.min(headerRowIndex + 10, lines.size()); i++) {
            String[] row = lines.get(i);
            if (marketplaceIdx < row.length) {
                String marketplace = row[marketplaceIdx].toLowerCase().trim();
                Map<String, String> domainMap = marketplaceConfigService.buildDomainToSiteCodeMap();
                for (Map.Entry<String, String> entry : domainMap.entrySet()) {
                    if (marketplace.contains(entry.getKey())) {
                        return entry.getValue();
                    }
                }
            }
        }

        return null;
    }

    /**
     * Simple CSV line parser
     */
    private String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        result.add(current.toString().trim());

        return result.toArray(new String[0]);
    }

    /**
     * Parse date string based on site code
     * Using Hutool DateUtil for better format compatibility
     */
    public LocalDateTime parseDate(String dateStr, String siteCode) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }

        String processed = dateStr.trim();

        // Handle Canadian a.m./p.m. format
        if ("CA".equals(siteCode)) {
            processed = processed
                    .replace("a.m.", "AM")
                    .replace("p.m.", "PM")
                    .replace("A.M.", "AM")
                    .replace("P.M.", "PM");
        }

        // Handle Chinese date format (CN site)
        if ("CN".equals(siteCode)) {
            // Convert Chinese AM/PM markers to 24-hour format indicators
            boolean isPM = processed.contains("下午") || processed.contains("晚上");
            boolean isAM = processed.contains("上午") || processed.contains("早上") || processed.contains("凌晨");

            // Remove Chinese AM/PM markers
            processed = processed
                    .replace("上午", "")
                    .replace("下午", "")
                    .replace("早上", "")
                    .replace("晚上", "")
                    .replace("凌晨", "")
                    .trim();

            // Try to parse with Chinese format
            try {
                // Parse the date string with Chinese characters
                Date date = DateUtil.parse(processed);
                LocalDateTime localDateTime = date.toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();

                // Adjust hour for PM (下午/晚上) if hour is < 12
                if (isPM && localDateTime.getHour() < 12 && localDateTime.getHour() > 0) {
                    localDateTime = localDateTime.plusHours(12);
                }

                return localDateTime;
            } catch (Exception e) {
                log.warn("Failed to parse Chinese date: dateStr={}, processed={}, error={}",
                        dateStr, processed, e.getMessage());
                return null;
            }
        }

        try {
            // 优先使用 DateConverter：按字面日期解析，不做时区转换（避免 UTC 等被转为系统时区）
            LocalDateTime result = DateConverter.parse(processed, siteCode);
            if (result != null) {
                return result;
            }
            // 回退到 Hutool 解析
            Date date = DateUtil.parse(processed);
            return date.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
        } catch (Exception e) {
            log.warn("Failed to parse date: dateStr={}, siteCode={}, error={}",
                    dateStr, siteCode, e.getMessage());
            return null;
        }
    }

    /**
     * Parse decimal based on site code
     * German uses comma as decimal separator
     */
    public BigDecimal parseDecimal(String value, String siteCode) {
        if (value == null || value.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }

        String processed = value.trim();

        // European format (DE/FR/IT/ES): comma as decimal separator, dot as thousand separator
        if ("DE".equals(siteCode) || "FR".equals(siteCode) || "IT".equals(siteCode) || "ES".equals(siteCode)) {
            processed = processed.replace(".", "").replace(",", ".");
        } else {
            // English format: comma as thousand separator
            processed = processed.replace(",", "");
        }

        try {
            return new BigDecimal(processed);
        } catch (NumberFormatException e) {
            log.warn("Failed to parse decimal: value={}, siteCode={}", value, siteCode);
            return BigDecimal.ZERO;
        }
    }
}
