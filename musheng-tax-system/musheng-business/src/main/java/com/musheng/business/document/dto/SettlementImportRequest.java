package com.musheng.business.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 结算数据导入请求
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "结算数据导入请求")
public class SettlementImportRequest {

    @NotNull(message = "店铺ID不能为空")
    @Schema(description = "店铺ID")
    private Long shopId;

    @NotNull(message = "结算周期起始日不能为空")
    @Schema(description = "结算周期起始日")
    private LocalDate periodStart;

    @NotNull(message = "结算周期结束日不能为空")
    @Schema(description = "结算周期结束日")
    private LocalDate periodEnd;

    @NotEmpty(message = "导入数据列表不能为空")
    @Schema(description = "导入数据列表")
    private List<SettlementImportItem> items;

    /**
     * 结算导入明细项
     *
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "结算导入明细项")
    public static class SettlementImportItem {

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
    }
}
