package com.musheng.business.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * DN送货单生成请求
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DN送货单生成请求")
public class DnGenerateRequest {

    @NotBlank(message = "站点代码不能为空")
    @Schema(description = "站点代码（US/UK/CA/EU），以前端用户选择为准")
    private String siteCode;

    @NotNull(message = "锚点日期不能为空")
    @Schema(description = "锚点日期")
    private LocalDate anchorDate;

    @Schema(description = "FBA货件ID列表（与poId二选一，传poId时可不传）")
    private List<Long> shipmentIds;

    @Schema(description = "关联PO主键（传入时自动从PO明细提取货件，shipmentIds可为空）")
    private Long poId;
}
