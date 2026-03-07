package com.musheng.business.document.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 送货单详情视图
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "送货单详情视图")
public class DnVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "单据编号")
    private String documentNo;

    @Schema(description = "送货日期")
    private LocalDate dnDate;

    @Schema(description = "供应商名称")
    private String supplierName;

    @Schema(description = "客户名称")
    private String customerName;

    @Schema(description = "总数量")
    private Integer totalQuantity;

    @Schema(description = "DN周期起始日")
    private LocalDate periodStart;

    @Schema(description = "DN周期结束日")
    private LocalDate periodEnd;

    @Schema(description = "DN明细列表")
    private List<DnItemVO> items;
}
