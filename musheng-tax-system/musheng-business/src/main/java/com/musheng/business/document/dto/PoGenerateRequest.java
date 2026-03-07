package com.musheng.business.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * PO采购订单生成请求
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "PO采购订单生成请求")
public class PoGenerateRequest {

    @NotEmpty(message = "货件ID列表不能为空")
    @Schema(description = "FBA货件ID列表")
    private List<Long> shipmentIds;
}
