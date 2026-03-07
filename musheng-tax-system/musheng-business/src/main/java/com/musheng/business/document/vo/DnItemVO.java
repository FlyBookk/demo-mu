package com.musheng.business.document.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 送货单明细视图
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "送货单明细视图")
public class DnItemVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "行号")
    private Integer lineNo;

    @Schema(description = "MSKU编码")
    private String msku;

    @Schema(description = "数量")
    private Integer quantity;

    @Schema(description = "对应货件编号")
    private String shipmentNo;
}
