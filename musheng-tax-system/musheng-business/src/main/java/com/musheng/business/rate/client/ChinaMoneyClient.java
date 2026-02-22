package com.musheng.business.rate.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.musheng.common.exception.BusinessException;
import com.musheng.common.result.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 中国货币网 curl 解析客户端
 * 仅支持通过粘贴 curl 命令同步汇率（用户从浏览器 F12 → Network → Copy as cURL）
 */
@Slf4j
@Component
public class ChinaMoneyClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public ChinaMoneyClient() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 执行 curl 命令获取汇率数据（用户从浏览器复制的完整 curl）
     * 支持新返回格式：data.head + records[].date/values
     * 自动翻页：根据 data.pageTotal 循环请求所有页
     *
     * @param curlCommand 完整的 curl 命令
     * @return 汇率数据列表
     */
    public List<RateData> executeCurlAndParse(String curlCommand) {
        CurlParser.ParsedCurl parsed = CurlParser.parse(curlCommand);

        HttpHeaders headers = new HttpHeaders();
        parsed.headers().forEach(headers::set);

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        HttpMethod method = "POST".equalsIgnoreCase(parsed.method()) ? HttpMethod.POST : HttpMethod.GET;

        List<RateData> allRates = new ArrayList<>();
        int pageNum = 1;
        int pageTotal = 1;

        do {
            String url = buildUrlWithPageNum(parsed.url(), pageNum);

            ResponseEntity<String> response = restTemplate.exchange(url, method, entity, String.class);

            if (response.getStatusCode() != HttpStatus.OK) {
                throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR,
                        "请求失败: " + response.getStatusCode());
            }

            CurlParseResult parseResult = parseCurlResponseWithMeta(response.getBody());
            allRates.addAll(parseResult.rates());

            if (pageNum == 1) {
                pageTotal = parseResult.pageTotal();
                log.info("Curl sync pagination: total={} records, pageTotal={} pages",
                        parseResult.total(), pageTotal);
            }

            pageNum++;
        } while (pageNum <= pageTotal);

        return allRates;
    }

    /**
     * 将 URL 中的 pageNum 参数替换为指定页码
     */
    private String buildUrlWithPageNum(String url, int pageNum) {
        if (url.contains("pageNum=")) {
            return url.replaceFirst("pageNum=\\d+", "pageNum=" + pageNum);
        }
        String separator = url.contains("?") ? "&" : "?";
        return url + separator + "pageNum=" + pageNum;
    }

    /**
     * 解析 curl 响应，返回汇率数据及分页元信息
     */
    private CurlParseResult parseCurlResponseWithMeta(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode dataNode = root.has("data") ? root.get("data") : root;

            int total = dataNode.has("total") ? dataNode.get("total").asInt() : 0;
            int pageTotal = dataNode.has("pageTotal") ? dataNode.get("pageTotal").asInt() : 1;

            List<RateData> rates = parseCurlRecords(root, dataNode);
            return new CurlParseResult(rates, total, pageTotal);
        } catch (Exception e) {
            log.error("Failed to parse curl response", e);
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR,
                    "解析响应失败: " + e.getMessage());
        }
    }

    /**
     * 解析 records 和 head 为 RateData 列表
     */
    private List<RateData> parseCurlRecords(JsonNode root, JsonNode dataNode) {
        List<RateData> rates = new ArrayList<>();

        JsonNode recordsNode = root.has("records") ? root.get("records") : null;
        if (recordsNode == null && dataNode.has("records")) {
            recordsNode = dataNode.get("records");
        }
        if (recordsNode == null || !recordsNode.isArray()) {
            return rates;
        }

        JsonNode headNode = dataNode.has("head") ? dataNode.get("head") : null;
        if (headNode == null || !headNode.isArray()) {
            return rates;
        }

        List<String> currencyPairs = new ArrayList<>();
        for (JsonNode h : headNode) {
            currencyPairs.add(h.asText());
        }

        for (JsonNode record : recordsNode) {
            String dateStr = record.has("date") ? record.get("date").asText() : null;
            JsonNode valuesNode = record.has("values") ? record.get("values") : null;
            if (dateStr == null || valuesNode == null || !valuesNode.isArray()) {
                continue;
            }

            LocalDate rateDate = LocalDate.parse(dateStr);

            for (int i = 0; i < Math.min(currencyPairs.size(), valuesNode.size()); i++) {
                String pair = currencyPairs.get(i);
                String valueStr = valuesNode.get(i).asText().replace(",", "");
                double rawRate = Double.parseDouble(valueStr);

                String currencyCode = parseCurrencyCode(pair);
                double rate = convertRate(pair, rawRate);

                rates.add(new RateData(rateDate, currencyCode, rate));
            }
        }

        return rates;
    }

    private record CurlParseResult(List<RateData> rates, int total, int pageTotal) {}

    /**
     * 解析货币编码
     * USD/CNY -> USD
     * 100JPY/CNY -> JPY
     * CNY/MOP -> MOP
     */
    private String parseCurrencyCode(String currencyPair) {
        if (currencyPair.contains("/")) {
            String[] parts = currencyPair.split("/");
            if (parts[0].contains("CNY")) {
                return parts[1];
            } else {
                String code = parts[0];
                return code.replaceAll("^\\d+", "");
            }
        }
        return currencyPair;
    }

    /**
     * 根据货币对格式转换汇率
     * XXX/CNY: 直接使用
     * CNY/XXX: 取倒数
     * 100JPY/CNY: 直接使用（存储为 100 日元对应人民币）
     */
    private double convertRate(String currencyPair, double rawRate) {
        if (currencyPair.contains("/")) {
            String[] parts = currencyPair.split("/");
            if (parts[0].contains("CNY")) {
                return rawRate > 0 ? 1.0 / rawRate : 0;
            }
        }
        return rawRate;
    }

    /**
     * 汇率数据记录
     */
    public record RateData(LocalDate date, String currencyCode, double rate) {}
}
