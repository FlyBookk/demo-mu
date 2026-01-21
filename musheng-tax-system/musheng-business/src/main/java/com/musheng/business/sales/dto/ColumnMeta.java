package com.musheng.business.sales.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 列元数据
 * 
 * @author BACKEND_AGENT
 * @since 2026-01-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "列元数据")
public class ColumnMeta {
    
    @Schema(description = "字段名", example = "orderId")
    private String field;
    
    @Schema(description = "显示名称", example = "订单ID")
    private String label;
    
    @Schema(description = "字段类型", example = "string")
    private String type;
    
    @Schema(description = "对应的源字段", example = "order id")
    private String sourceField;
    
    @Schema(description = "是否已映射", example = "true")
    private Boolean mapped;
}
