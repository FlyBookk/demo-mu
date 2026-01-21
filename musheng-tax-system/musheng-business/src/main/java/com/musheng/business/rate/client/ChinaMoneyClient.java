package com.musheng.business.rate.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.musheng.business.rate.config.ChinaMoneyConfig;
import com.musheng.common.exception.BusinessException;
import com.musheng.common.result.ErrorCode;
import com.musheng.config.currency.entity.Currency;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 中国外汇交易中心API客户端
 * 用于从外汇交易中心同步汇率数据
 */
@Slf4j
@Component
public class ChinaMoneyClient {

    private static final String BASE_URL = "https://www.chinamoney.com.cn/ags/ms/cm-u-bk-ccpr/CcprHisNew";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ChinaMoneyConfig config;

    public ChinaMoneyClient(ChinaMoneyConfig config) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.config = config;
    }

    /**
     * 获取汇率数据
     *
     * @param startDate   开始日期
     * @param endDate     结束日期
     * @param currencies  货币对象列表
     * @return 汇率数据列表
     */
    public List<RateData> fetchRates(LocalDate startDate, LocalDate endDate, List<Currency> currencies) {
        try {
            // 构建货币对参数 (例如: USD/CNY,EUR/CNY,CNY/MOP)
            String currencyPairs = buildCurrencyPairs(currencies);

            // 构建请求URL
            String url = buildUrl(startDate, endDate, currencyPairs);

            log.info("Fetching rates from China Money: startDate={}, endDate={}, currencies={}",
                    startDate, endDate, currencies.stream().map(Currency::getCurrencyCode).toList());

            // 设置请求头 - 模拟完整的浏览器请求
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json, text/javascript, */*; q=0.01");
            headers.set("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
            headers.set("Accept-Encoding", "gzip, deflate, br");
            headers.set("Cache-Control", "no-cache");
            headers.set("Connection", "keep-alive");
            headers.set("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            headers.set("Origin", "https://www.chinamoney.com.cn");
            headers.set("Pragma", "no-cache");
            headers.set("Referer", "https://www.chinamoney.com.cn/chinese/bkccpr/");
            headers.set("Sec-Fetch-Dest", "empty");
            headers.set("Sec-Fetch-Mode", "cors");
            headers.set("Sec-Fetch-Site", "same-origin");
            headers.set("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            headers.set("X-Requested-With", "XMLHttpRequest");
            headers.set("sec-ch-ua", "\"Not_A Brand\";v=\"8\", \"Chromium\";v=\"120\", \"Google Chrome\";v=\"120\"");
            headers.set("sec-ch-ua-mobile", "?0");
            headers.set("sec-ch-ua-platform", "\"macOS\"");

            // 如果配置了 Cookie，则添加到请求头
            if (config.isEnableCookie() && config.getCookie() != null && !config.getCookie().isEmpty()) {
                headers.set("Cookie", config.getCookie());
                log.debug("Using configured cookie for request");
            }

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // 发送POST请求
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode() != HttpStatus.OK) {
                throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR,
                        "Failed to fetch rates from China Money: " + response.getStatusCode());
            }

            // 解析响应
            return parseResponse(response.getBody());

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching rates from China Money", e);
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR,
                    "Failed to fetch rates: " + e.getMessage());
        }
    }

    /**
     * 构建货币对参数
     * 根据货币对方向将 Currency 对象转换为货币对字符串
     * DIRECT: [USD, EUR, GBP] -> "USD/CNY,EUR/CNY,GBP/CNY"
     * REVERSE: [MOP, MYR] -> "CNY/MOP,CNY/MYR"
     */
    private String buildCurrencyPairs(List<Currency> currencies) {
        List<String> pairs = new ArrayList<>();
        for (Currency currency : currencies) {
            String code = currency.getCurrencyCode();
            String direction = currency.getPairDirection();

            // 跳过 CNY 本身
            if ("CNY".equals(code)) {
                continue;
            }

            // 根据货币对方向构建货币对
            // 如果 direction 为 null 或空，默认使用 DIRECT
            if ("REVERSE".equals(direction)) {
                // CNY/XXX 格式
                pairs.add("CNY/" + code);
            } else {
                // DIRECT 或默认使用 XXX/CNY 格式
                // 特殊处理日元(100JPY/CNY)
                if ("JPY".equals(code)) {
                    pairs.add("100JPY/CNY");
                } else {
                    pairs.add(code + "/CNY");
                }
            }
        }
        return String.join(",", pairs);
    }

    /**
     * 构建请求URL
     */
    private String buildUrl(LocalDate startDate, LocalDate endDate, String currencyPairs) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        long timestamp = System.currentTimeMillis();
        return String.format("%s?startDate=%s&endDate=%s&currency=%s&pageNum=1&pageSize=1000&t=%d",
                BASE_URL,
                startDate.format(formatter),
                endDate.format(formatter),
                currencyPairs,
                timestamp);
    }

    /**
     * 解析API响应
     */
    private List<RateData> parseResponse(String responseBody) {
        List<RateData> rates = new ArrayList<>();

        try {
            JsonNode rootNode = objectMapper.readTree(responseBody);

            // 检查响应状态
            if (!rootNode.has("data")) {
                log.warn("No data field in response: {}", responseBody.substring(0, Math.min(200, responseBody.length())));
                return rates;
            }

            JsonNode dataNode = rootNode.get("data");
            if (!dataNode.has("records") || !dataNode.get("records").isArray()) {
                log.warn("No records in response data");
                return rates;
            }

            JsonNode recordsNode = dataNode.get("records");

            for (JsonNode record : recordsNode) {
                try {
                    String dateStr = record.has("date") ? record.get("date").asText() : null;
                    String currencyPair = record.has("vrtEfcBuyPrcCny") ? record.get("vrtEfcBuyPrcCny").asText() : null;
                    String priceStr = record.has("prcCny") ? record.get("prcCny").asText() : null;

                    if (dateStr == null || currencyPair == null || priceStr == null) {
                        continue;
                    }

                    // 解析货币编码 (USD/CNY -> USD, 100JPY/CNY -> JPY)
                    String currencyCode = parseCurrencyCode(currencyPair);

                    // 解析日期 (yyyy-MM-dd)
                    LocalDate rateDate = LocalDate.parse(dateStr);

                    // 解析汇率值
                    String cleanPrice = priceStr.replace(",", "");
                    double rateValue = Double.parseDouble(cleanPrice);

                    rates.add(new RateData(rateDate, currencyCode, rateValue));

                } catch (Exception e) {
                    log.warn("Failed to parse rate record: {}", record, e);
                }
            }

        } catch (Exception e) {
            log.error("Failed to parse response from China Money", e);
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR,
                    "Failed to parse rate data: " + e.getMessage());
        }

        return rates;
    }

    /**
     * 解析货币编码
     * USD/CNY -> USD
     * 100JPY/CNY -> JPY
     * CNY/MOP -> MOP
     */
    private String parseCurrencyCode(String currencyPair) {
        if (currencyPair.contains("/")) {
            String[] parts = currencyPair.split("/");

            // 判断是哪种格式
            if (parts[0].contains("CNY")) {
                // CNY/XXX 格式，返回后面的货币
                return parts[1];
            } else {
                // XXX/CNY 格式，返回前面的货币
                String code = parts[0];
                // 移除数字前缀 (100JPY -> JPY)
                return code.replaceAll("^\\d+", "");
            }
        }
        return currencyPair;
    }

    /**
     * 汇率数据记录
     */
    public record RateData(LocalDate date, String currencyCode, double rate) {}
}
