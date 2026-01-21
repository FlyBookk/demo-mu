package com.musheng.config.marketplace.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.musheng.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 站点实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_marketplace")
public class Marketplace extends BaseEntity {

    /**
     * 站点编码(US/CA/UK/DE)
     */
    private String siteCode;

    /**
     * 站点名称
     */
    private String siteName;

    /**
     * Marketplace标识(amazon.com等)
     */
    private String marketplaceId;

    /**
     * 关联货币编码
     */
    private String currencyCode;

    /**
     * 卖家ID
     */
    private String sellerId;

    /**
     * 表头语言(EN/DE)
     */
    private String headerLanguage;

    /**
     * 日期解析格式
     */
    private String dateFormat;

    /**
     * 数字格式(.或,)
     */
    private String numberFormat;

    /**
     * 时区
     */
    private String timezone;

    /**
     * 状态(1启用, 0禁用)
     */
    private Integer status;
}
