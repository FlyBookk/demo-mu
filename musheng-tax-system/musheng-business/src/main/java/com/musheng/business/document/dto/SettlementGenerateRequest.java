package com.musheng.business.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 结算单生成请求
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "结算单生成请求")
public class SettlementGenerateRequest {

    @NotNull(message = "结算周期起始日不能为空")
    @Schema(description = "结算周期起始日")
    private LocalDate periodStart;

    @NotNull(message = "结算周期结束日不能为空")
    @Schema(description = "结算周期结束日")
    private LocalDate periodEnd;

    @NotEmpty(message = "货件ID列表不能为空")
    @Schema(description = "选中的FBA货件ID列表，用于限定结算单范围")
    private List<Long> shipmentIds;
}
