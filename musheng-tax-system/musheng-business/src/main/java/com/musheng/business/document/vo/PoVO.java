package com.musheng.business.document.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * PO采购订单详情视图
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "PO采购订单详情视图")
public class PoVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "单据编号")
    private String documentNo;

    @Schema(description = "PO日期")
    private LocalDate poDate;

    @Schema(description = "买方名称")
    private String buyerName;

    @Schema(description = "买方地址")
    private String buyerAddress;

    @Schema(description = "卖方名称")
    private String sellerName;

    @Schema(description = "总数量")
    private Integer totalQuantity;

    @Schema(description = "包含货件数")
    private Integer shipmentCount;

    @Schema(description = "PO明细列表")
    private List<PoItemVO> items;
}
