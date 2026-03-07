package com.musheng.business.document.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.musheng.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 结算导入数据实体
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_settlement_import_data")
@Schema(description = "结算导入数据")
public class SettlementImportData extends BaseEntity {

    @Schema(description = "店铺ID")
    private Long shopId;

    @Schema(description = "导入批次ID")
    private Long importBatchId;

    @Schema(description = "结算周期起始日")
    private LocalDate periodStart;

    @Schema(description = "结算周期结束日")
    private LocalDate periodEnd;

    @Schema(description = "站点代码")
    private String siteCode;

    @Schema(description = "MSKU编码")
    private String msku;

    @Schema(description = "货币代码")
    private String currency;

    @Schema(description = "单价")
    private BigDecimal unitPrice;

    @Schema(description = "销售数量")
    private Integer quantity;

    @Schema(description = "金额")
    private BigDecimal amount;

    @Schema(description = "推导批次标识")
    private String settlementBatchId;

    @Schema(description = "逻辑删除标记（0=正常，1=已删除）")
    @TableLogic
    private Integer delFlag;

    @Schema(description = "采购成本（人民币）")
    private BigDecimal procurementCostCny;

    @Schema(description = "周期平均汇率")
    private BigDecimal averageExchangeRate;
}
