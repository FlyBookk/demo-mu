package com.musheng.business.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 交易方配置复制请求 DTO
 *
 * @author wanhua
 * 10:30 2026年03月22日
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "交易方配置复制请求")
public class DocumentPartyConfigCopyDTO {

    @NotNull(message = "来源配置ID不能为空")
    @Schema(description = "来源配置ID")
    private Long sourceId;

    @NotBlank(message = "目标站点代码不能为空")
    @Schema(description = "目标站点代码（US/CA/UK/EU等）")
    private String targetSiteCode;
}
