package com.musheng.business.advertising.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.musheng.common.annotation.FieldMapping;
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
     * 店铺ID（用于数据隔离）
     */
    @FieldMapping(label = "店铺ID", ignore = true)
    private Long shopId;

    /**
     * 店铺名称
     */
    @FieldMapping(label = "店铺名称", description = "店铺名称", required = true, maxLength = 100, order = 1)
    private String storeName;

    /**
     * 站点编码（从店铺名称推断）
     */
    @FieldMapping(label = "站点编码", ignore = true)
    private String siteCode;

    /**
     * 发票编号（去重关键字段）
     */
    @FieldMapping(label = "发票编号", description = "发票编号，去重关键字段", required = true, maxLength = 100, order = 2)
    private String invoiceNumber;

    /**
     * 发票状态（PAID_IN_FULL等）
     */
    @FieldMapping(label = "发票状态", description = "PAID_IN_FULL等", maxLength = 50, order = 3)
    private String invoiceStatus;

    /**
     * 支付类型（CREDIT_CARD等）
     */
    @FieldMapping(label = "支付类型", description = "CREDIT_CARD等", maxLength = 50, order = 4)
    private String paymentType;

    /**
     * 账单开始日期（核心字段）
     */
    @FieldMapping(label = "账单开始日期", description = "账单周期开始日期", required = true, order = 5)
    private LocalDate billingStartDate;

    /**
     * 账单结束日期（核心字段）
     */
    @FieldMapping(label = "账单结束日期", description = "账单周期结束日期", required = true, order = 6)
    private LocalDate billingEndDate;

    /**
     * 发票开具日期
     */
    @FieldMapping(label = "发票开具日期", description = "发票开具日期", order = 7)
    private LocalDate issueDate;

    /**
     * 付款币种（USD/CAD/GBP/EUR）
     */
    @FieldMapping(label = "货币编码", description = "付款币种", required = true, maxLength = 10, order = 8)
    private String currency;

    /**
     * 账单金额（发票总金额）
     */
    @FieldMapping(label = "发票金额", description = "发票总金额", precision = 2, order = 10)
    private BigDecimal invoiceAmount;

    /**
     * 费用（实际花费）
     */
    @FieldMapping(label = "费用", description = "实际花费", required = true, precision = 2, order = 11)
    private BigDecimal cost;

    /**
     * 其他费分摊
     */
    @FieldMapping(label = "其他费用", description = "其他费分摊", precision = 2, order = 12)
    private BigDecimal otherCost;

    /**
     * 广告活动名称
     */
    @FieldMapping(label = "广告活动名称", description = "广告活动名称", maxLength = 200, order = 13)
    private String campaignName;

    /**
     * 活动ID（广告活动唯一标识）
     */
    @FieldMapping(label = "广告活动ID", description = "广告活动唯一标识", maxLength = 100, order = 14)
    private String campaignId;

    /**
     * 计价方式（CPC/CPM等）
     */
    @FieldMapping(label = "计价方式", description = "CPC/CPM等", maxLength = 20, order = 15)
    private String pricingModel;

    /**
     * 点击次数
     */
    @FieldMapping(label = "点击次数", description = "广告点击次数", order = 16)
    private Integer clicks;

    /**
     * 平均点击单价
     */
    @FieldMapping(label = "平均点击单价", description = "CPC单价", precision = 4, order = 17)
    private BigDecimal avgCpc;

    /**
     * 取值来源（业务报告等）
     */
    @FieldMapping(label = "数据来源", description = "业务报告等", maxLength = 50, order = 18)
    private String dataSource;

    /**
     * 承担商品（逗号分隔）
     */
    @FieldMapping(label = "承担商品", description = "逗号分隔的商品列表", maxLength = 1000, order = 19)
    private String productList;

    /**
     * 广告类型（SPONSORED PRODUCTS等）
     */
    @FieldMapping(label = "广告类型", description = "SPONSORED PRODUCTS等", maxLength = 50, order = 20)
    private String adType;

    /**
     * 发票附件路径
     */
    @FieldMapping(label = "附件路径", description = "发票附件路径", maxLength = 500, order = 21)
    private String attachmentPath;

    /**
     * 备注
     */
    @FieldMapping(label = "备注", description = "备注信息", maxLength = 500, order = 22)
    private String remark;

    /**
     * 使用的汇率
     */
    @FieldMapping(label = "汇率", ignore = true)
    private BigDecimal exchangeRate;

    /**
     * 汇率取值日期（用于查询汇率的日期）
     */
    @FieldMapping(label = "汇率取值日期", ignore = true)
    private LocalDate exchangeRateDate;

    /**
     * 费用金额（人民币）
     */
    @FieldMapping(label = "人民币金额", ignore = true)
    private BigDecimal amountCny;

    /**
     * 导入批次ID
     */
    @FieldMapping(label = "导入批次ID", ignore = true)
    private String importBatchId;

    /**
     * 创建人ID
     */
    @FieldMapping(label = "创建人", ignore = true)
    private Long createBy;

    /**
     * 更新人ID
     */
    @FieldMapping(label = "更新人", ignore = true)
    private Long updateBy;
}
