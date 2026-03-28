package com.musheng.business.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 单据查询请求
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "单据查询请求")
public class DocumentQueryRequest {

    @Schema(description = "单据类型（PO/DN/SETTLEMENT/INV）")
    private String documentType;

    @Schema(description = "单据编号")
    private String documentNo;

    @Schema(description = "开始日期")
    private LocalDate startDate;

    @Schema(description = "结束日期")
    private LocalDate endDate;

    @Schema(description = "站点代码（US/CA/UK/EU）")
    private String siteCode;

    @Schema(description = "导出时间起始（创建时间范围）")
    private LocalDateTime createTimeStart;

    @Schema(description = "导出时间结束（创建时间范围）")
    private LocalDateTime createTimeEnd;

    @Builder.Default
    @Schema(description = "页码", example = "1")
    private Integer pageNum = 1;

    @Builder.Default
    @Schema(description = "每页条数", example = "10")
    private Integer pageSize = 10;
}
