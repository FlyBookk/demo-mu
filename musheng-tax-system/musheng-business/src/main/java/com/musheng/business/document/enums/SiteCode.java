package com.musheng.business.document.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 站点/货币枚举 — FBA单据生成专用
 *
 * <p>定义四个站点的序号、货币代码和MSKU前缀映射关系。
 * 序号001-004分别对应USD/CAD/GBP/EUR四个站点，
 * 用于结算单和INV的站点拆分与编号生成。</p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@Getter
@AllArgsConstructor
public enum SiteCode {

    /** 美国站 */
    US("001", "USD", "MSUS-", "美国站"),

    /** 加拿大站 */
    CA("002", "CAD", "MSCA-", "加拿大站"),

    /** 英国站 */
    UK("003", "GBP", "MSUK-", "英国站"),

    /** 欧盟站 */
    EU("004", "EUR", "MSEU-", "欧盟站");

    /** 站点序号（001-004） */
    private final String sequence;

    /** 货币代码 */
    private final String currency;

    /** MSKU前缀 */
    private final String mskuPrefix;

    /** 站点描述 */
    private final String description;

    /**
     * 根据序号查找站点
     *
     * @param sequence 站点序号（如 "001"）
     * @return 匹配的站点，未找到返回 null
     */
    public static SiteCode fromSequence(String sequence) {
        for (SiteCode site : values()) {
            if (site.getSequence().equals(sequence)) {
                return site;
            }
        }
        return null;
    }

    /**
     * 根据货币代码查找站点
     *
     * @param currency 货币代码（如 "USD"）
     * @return 匹配的站点，未找到返回 null
     */
    public static SiteCode fromCurrency(String currency) {
        for (SiteCode site : values()) {
            if (site.getCurrency().equalsIgnoreCase(currency)) {
                return site;
            }
        }
        return null;
    }

    /**
     * 根据站点代码名称查找站点（如 "US", "CA", "UK", "DE"/"EU"）
     *
     * @param code 站点代码名称
     * @return 匹配的站点，未找到返回 null
     * @author wanhua
     * 17:30 2026年03月07日
     */
    public static SiteCode fromSiteCodeName(String code) {
        if (code == null) {
            return null;
        }
        for (SiteCode site : values()) {
            if (site.name().equalsIgnoreCase(code)) {
                return site;
            }
        }
        // DE 映射到 EU
        if ("DE".equalsIgnoreCase(code)) {
            return EU;
        }
        return null;
    }

    /**
     * 根据MSKU前缀匹配站点
     *
     * <p>判断给定的MSKU是否以某个站点前缀开头，返回匹配的站点。</p>
     *
     * @param msku MSKU编码（如 "MSUS-ABC123"）
     * @return 匹配的站点，未找到返回 null
     */
    public static SiteCode fromMskuPrefix(String msku) {
        if (msku == null) {
            return null;
        }
        for (SiteCode site : values()) {
            if (msku.startsWith(site.getMskuPrefix())) {
                return site;
            }
        }
        return null;
    }
}
