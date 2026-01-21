package com.musheng.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Locale;

/**
 * 站点编码枚举 - 亚马逊站点
 */
@Getter
@AllArgsConstructor
public enum SiteCode {

    US("US", "美国站", "USD", "amazon.com", "EN", "MMM d, yyyy h:mm:ss a z", ".", Locale.US),
    CA("CA", "加拿大站", "CAD", "amazon.ca", "EN", "MMM d, yyyy h:mm:ss a z", ".", Locale.CANADA),
    UK("UK", "英国站", "GBP", "amazon.co.uk", "EN", "dd MMM yyyy HH:mm:ss z", ".", Locale.UK),
    DE("DE", "德国站", "EUR", "amazon.de", "DE", "dd.MM.yyyy HH:mm:ss z", ",", Locale.GERMANY);

    private final String code;
    private final String name;
    private final String currencyCode;
    private final String marketplaceId;
    private final String headerLanguage;
    private final String dateFormat;
    private final String decimalSeparator;
    private final Locale locale;

    public static SiteCode fromCode(String code) {
        for (SiteCode site : values()) {
            if (site.getCode().equalsIgnoreCase(code)) {
                return site;
            }
        }
        return null;
    }

    public static SiteCode fromMarketplaceId(String marketplaceId) {
        for (SiteCode site : values()) {
            if (site.getMarketplaceId().equalsIgnoreCase(marketplaceId)) {
                return site;
            }
        }
        return null;
    }

    /**
     * 检查该站点是否使用德语表头
     */
    public boolean isGermanHeader() {
        return "DE".equals(this.headerLanguage);
    }

    /**
     * 检查该站点是否使用逗号作为小数分隔符
     */
    public boolean usesCommaDecimal() {
        return ",".equals(this.decimalSeparator);
    }
}
