package com.musheng.business.document.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * PO采购订单明细视图
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "PO采购订单明细视图")
public class PoItemVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "FBA货件编号")
    private String shipmentNo;

    @Schema(description = "MSKU编码")
    private String msku;

    @Schema(description = "数量")
    private Integer quantity;

    @Schema(description = "FBA仓库地址")
    private String fbaAddress;

    @Schema(description = "排序序号")
    private Integer sortOrder;
}
