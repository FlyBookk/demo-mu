package com.musheng.business.advertising.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.musheng.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 广告发票数据实体
 * 映射到 t_advertising_data 表
 *
 * @author 后端研发团队
 * @since 2026-01-20
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_advertising_data")
public class AdvertisingData extends BaseEntity {

    /**
     * 店铺名称
     */
    private String storeName;

    /**
     * 站点编码（从店铺名称推断）
     */
    private String siteCode;

    /**
     * 发票编号（去重关键字段）
     */
    private String invoiceNumber;

    /**
     * 发票状态（PAID_IN_FULL等）
     */
    private String invoiceStatus;

    /**
     * 支付类型（CREDIT_CARD等）
     */
    private String paymentType;

    /**
     * 账单开始日期（核心字段）
     */
    private LocalDate billingStartDate;

    /**
     * 账单结束日期（核心字段）
     */
    private LocalDate billingEndDate;

    /**
     * 发票开具日期
     */
    private LocalDate issueDate;

    /**
     * 付款币种（USD/CAD/GBP/EUR）
     */
    private String currency;

    /**
     * 账单金额（发票总金额）
     */
    private BigDecimal invoiceAmount;

    /**
     * 费用（实际花费）
     */
    private BigDecimal cost;

    /**
     * 其他费分摊
     */
    private BigDecimal otherCost;

    /**
     * 广告活动名称
     */
    private String campaignName;

    /**
     * 活动ID（广告活动唯一标识）
     */
    private String campaignId;

    /**
     * 计价方式（CPC/CPM等）
     */
    private String pricingModel;

    /**
     * 点击次数
     */
    private Integer clicks;

    /**
     * 平均点击单价
     */
    private BigDecimal avgCpc;

    /**
     * 取值来源（业务报告等）
     */
    private String dataSource;

    /**
     * 承担商品（逗号分隔）
     */
    private String productList;

    /**
     * 广告类型（SPONSORED PRODUCTS等）
     */
    private String adType;

    /**
     * 发票附件路径
     */
    private String attachmentPath;

    /**
     * 备注
     */
    private String remark;

    /**
     * 使用的汇率
     */
    private BigDecimal exchangeRate;

    /**
     * 费用金额（人民币）
     */
    private BigDecimal amountCny;

    /**
     * 导入批次ID
     */
    private String importBatchId;

    /**
     * 创建人ID
     */
    private Long createBy;

    /**
     * 更新人ID
     */
    private Long updateBy;
}
