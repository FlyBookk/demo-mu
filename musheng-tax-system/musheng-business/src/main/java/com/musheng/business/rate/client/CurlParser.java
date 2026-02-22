package com.musheng.business.rate.client;

import com.musheng.common.exception.BusinessException;
import com.musheng.common.result.ErrorCode;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 解析 curl 命令，提取 URL、请求方法和请求头
 */
public final class CurlParser {

    private CurlParser() {
    }

    /**
     * 解析 curl 命令
     *
     * @param curlCommand 完整的 curl 命令（如从浏览器 Copy as cURL 复制）
     * @return 解析结果
     */
    public static ParsedCurl parse(String curlCommand) {
        if (curlCommand == null || curlCommand.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "curl 命令不能为空");
        }

        // 合并多行 curl（反斜杠换行）
        String trimmed = curlCommand.replace("\\\n", " ").replace("\\\r\n", " ").trim();
        if (!trimmed.toLowerCase().startsWith("curl")) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "无效的 curl 命令格式");
        }

        String url = extractUrl(trimmed);
        if (url == null || url.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "无法从 curl 中解析 URL");
        }

        // 仅支持 chinamoney 域名
        if (!url.contains("chinamoney.com.cn")) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "仅支持中国外汇交易中心 (chinamoney.com.cn) 的请求");
        }

        String method = extractMethod(trimmed);
        Map<String, String> headers = extractHeaders(trimmed);

        return new ParsedCurl(url, method, headers);
    }

    private static String extractUrl(String curl) {
        // 匹配 curl 'url' 或 curl "url" 或 curl url
        Pattern p = Pattern.compile("curl\\s+(?:'([^']+)'|\"([^\"]+)\"|(\\S+))", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(curl);
        if (m.find()) {
            for (int i = 1; i <= 3; i++) {
                if (m.group(i) != null && m.group(i).startsWith("http")) {
                    return m.group(i);
                }
            }
        }
        return null;
    }

    private static String extractMethod(String curl) {
        Pattern p = Pattern.compile("-X\\s+(?:'([^']+)'|\"([^\"]+)\"|(\\S+))", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(curl);
        if (m.find()) {
            for (int i = 1; i <= 3; i++) {
                if (m.group(i) != null && !m.group(i).isEmpty()) {
                    return m.group(i).toUpperCase();
                }
            }
        }
        return "GET";
    }

    private static Map<String, String> extractHeaders(String curl) {
        Map<String, String> headers = new LinkedHashMap<>();

        // -H 'Name: value' 或 -H "Name: value"
        Pattern hPattern = Pattern.compile("-H\\s+(?:'([^']+)'|\"([^\"]+)\")", Pattern.CASE_INSENSITIVE);
        Matcher hMatcher = hPattern.matcher(curl);
        while (hMatcher.find()) {
            String header = hMatcher.group(1) != null ? hMatcher.group(1) : hMatcher.group(2);
            if (header != null && header.contains(":")) {
                int colon = header.indexOf(':');
                String name = header.substring(0, colon).trim();
                String value = header.substring(colon + 1).trim();
                if (!name.isEmpty()) {
                    headers.put(name, value);
                }
            }
        }

        // -b 'cookie' 或 --cookie 'cookie'
        Pattern bPattern = Pattern.compile("(?:-b|--cookie)\\s+(?:'([^']*)'|\"([^\"]*)\")", Pattern.CASE_INSENSITIVE);
        Matcher bMatcher = bPattern.matcher(curl);
        if (bMatcher.find()) {
            String cookie = bMatcher.group(1) != null ? bMatcher.group(1) : bMatcher.group(2);
            if (cookie != null && !cookie.isBlank()) {
                headers.put("Cookie", cookie);
            }
        }

        return headers;
    }

    public record ParsedCurl(String url, String method, Map<String, String> headers) {
    }
}
