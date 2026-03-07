package com.musheng.business.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 单据交易方信息配置
 *
 * <p>从 application.yml 的 app.document.party 节点读取卖方/买方/银行信息，
 * 避免将固定数据硬编码在生成器中。</p>
 *
 * @author wanhua
 * 10:30 2026年03月07日
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.document.party")
public class DocumentPartyProperties {

    /** 卖方名称 */
    private String sellerName;

    /** 卖方地址 */
    private String sellerAddress;

    /** 卖方电话 */
    private String sellerPhone;

    /** 买方名称 */
    private String buyerName;

    /** 买方地址 */
    private String buyerAddress;

    /** 买方电话 */
    private String buyerPhone;

    /** 银行账户名 */
    private String bankAccountName;

    /** 银行账号 */
    private String bankAccountNumber;

    /** 银行名称 */
    private String bankName;

    /** 银行地址 */
    private String bankAddress;

    /** SWIFT代码 */
    private String swiftCode;
}
