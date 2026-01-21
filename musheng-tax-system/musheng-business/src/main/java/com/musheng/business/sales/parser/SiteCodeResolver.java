package com.musheng.business.sales.parser;

import java.util.HashMap;
import java.util.Map;

/**
 * 站点编码解析器
 * 负责 Marketplace 域名到站点编码的映射
 * 
 * @author BACKEND_AGENT
 * @since 2026-01-21
 */
public class SiteCodeResolver {
    
    /**
     * 域名到站点编码的映射
     */
    private static final Map<String, String> MARKETPLACE_TO_SITE = new HashMap<>();
    
    /**
     * 站点编码到货币的映射
     */
    private static final Map<String, String> SITE_TO_CURRENCY = new HashMap<>();
    
    static {
        // 北美站
        MARKETPLACE_TO_SITE.put("amazon.com", "US");
        MARKETPLACE_TO_SITE.put("www.amazon.com", "US");
        MARKETPLACE_TO_SITE.put("amazon.ca", "CA");
        MARKETPLACE_TO_SITE.put("www.amazon.ca", "CA");
        MARKETPLACE_TO_SITE.put("amazon.com.mx", "MX");
        MARKETPLACE_TO_SITE.put("www.amazon.com.mx", "MX");
        
        // 欧洲站
        MARKETPLACE_TO_SITE.put("amazon.co.uk", "UK");
        MARKETPLACE_TO_SITE.put("www.amazon.co.uk", "UK");
        MARKETPLACE_TO_SITE.put("amazon.de", "DE");
        MARKETPLACE_TO_SITE.put("www.amazon.de", "DE");
        MARKETPLACE_TO_SITE.put("amazon.fr", "FR");
        MARKETPLACE_TO_SITE.put("www.amazon.fr", "FR");
        MARKETPLACE_TO_SITE.put("amazon.it", "IT");
        MARKETPLACE_TO_SITE.put("www.amazon.it", "IT");
        MARKETPLACE_TO_SITE.put("amazon.es", "ES");
        MARKETPLACE_TO_SITE.put("www.amazon.es", "ES");
        MARKETPLACE_TO_SITE.put("amazon.nl", "NL");
        MARKETPLACE_TO_SITE.put("www.amazon.nl", "NL");
        
        // 亚太站
        MARKETPLACE_TO_SITE.put("amazon.co.jp", "JP");
        MARKETPLACE_TO_SITE.put("www.amazon.co.jp", "JP");
        MARKETPLACE_TO_SITE.put("amazon.com.au", "AU");
        MARKETPLACE_TO_SITE.put("www.amazon.com.au", "AU");
        
        // 货币映射
        SITE_TO_CURRENCY.put("US", "USD");
        SITE_TO_CURRENCY.put("CA", "CAD");
        SITE_TO_CURRENCY.put("MX", "MXN");
        SITE_TO_CURRENCY.put("UK", "GBP");
        SITE_TO_CURRENCY.put("DE", "EUR");
        SITE_TO_CURRENCY.put("FR", "EUR");
        SITE_TO_CURRENCY.put("IT", "EUR");
        SITE_TO_CURRENCY.put("ES", "EUR");
        SITE_TO_CURRENCY.put("NL", "EUR");
        SITE_TO_CURRENCY.put("JP", "JPY");
        SITE_TO_CURRENCY.put("AU", "AUD");
    }
    
    /**
     * 根据marketplace域名获取站点编码
     * 
     * @param marketplace marketplace域名（如 amazon.com）
     * @return 站点编码（如 US），如果未找到返回null
     */
    public static String getSiteCode(String marketplace) {
        if (marketplace == null || marketplace.isEmpty()) {
            return null;
        }
        // 移除可能的协议前缀和路径
        String domain = marketplace.toLowerCase().trim();
        domain = domain.replaceAll("^https?://", "");
        domain = domain.replaceAll("/.*$", "");
        
        return MARKETPLACE_TO_SITE.get(domain);
    }
    
    /**
     * 根据站点编码获取默认货币
     * 
     * @param siteCode 站点编码
     * @return 货币代码
     */
    public static String getCurrencyCode(String siteCode) {
        if (siteCode == null) {
            return null;
        }
        return SITE_TO_CURRENCY.get(siteCode.toUpperCase());
    }
    
    /**
     * 检查站点是否使用德语表头
     * 
     * @param siteCode 站点编码
     * @return true-德语表头，false-英语表头
     */
    public static boolean isGermanSite(String siteCode) {
        return "DE".equalsIgnoreCase(siteCode);
    }
    
    /**
     * 检查是否为欧洲站点
     * 
     * @param siteCode 站点编码
     * @return true-欧洲站点
     */
    public static boolean isEuropeanSite(String siteCode) {
        if (siteCode == null) {
            return false;
        }
        String upper = siteCode.toUpperCase();
        return "UK".equals(upper) || "DE".equals(upper) || "FR".equals(upper) 
                || "IT".equals(upper) || "ES".equals(upper) || "NL".equals(upper);
    }
    
    /**
     * 从数据行中检测站点编码
     * 通过查找marketplace域名来确定站点
     * 
     * @param line CSV数据行
     * @return 站点编码，未找到返回null
     */
    public static String detectSiteFromLine(String line) {
        if (line == null || line.isEmpty()) {
            return null;
        }
        
        String lowerLine = line.toLowerCase();
        
        // 遍历所有已知的marketplace域名
        for (String domain : MARKETPLACE_TO_SITE.keySet()) {
            if (lowerLine.contains(domain)) {
                return MARKETPLACE_TO_SITE.get(domain);
            }
        }
        
        return null;
    }
}
