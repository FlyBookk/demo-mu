package com.musheng.business.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 单据导出请求
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "单据导出请求")
public class DocumentExportRequest {

    @Schema(description = "单据类型（PO/DN/SETTLEMENT/INV）")
    private String documentType;

    @Schema(description = "单据ID")
    private Long documentId;

    @Schema(description = "结算周期起始日（批量导出用）")
    private LocalDate periodStart;

    @Schema(description = "结算周期结束日（批量导出用）")
    private LocalDate periodEnd;
}
