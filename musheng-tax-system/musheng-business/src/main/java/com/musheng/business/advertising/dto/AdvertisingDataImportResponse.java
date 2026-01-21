package com.musheng.business.advertising.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 广告数据导入响应DTO
 * 返回导入结果统计信息
 *
 * @author 后端研发团队
 * @since 2026-01-20
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "广告数据导入响应")
public class AdvertisingDataImportResponse {

    @Schema(description = "提交的总记录数", example = "32")
    private Integer totalCount;

    @Schema(description = "成功导入的记录数", example = "3")
    private Integer importedCount;

    @Schema(description = "去重的记录数", example = "29")
    private Integer duplicatedCount;

    @Schema(description = "失败的记录数", example = "0")
    private Integer failedCount;

    @Schema(description = "导入批次ID")
    private String importBatchId;

    @Schema(description = "去重的发票编号列表")
    private List<String> duplicatedInvoices;

    @Schema(description = "失败的记录详情")
    private List<ImportFailureDetail> failedRecords;

    /**
     * 导入失败详情
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "导入失败详情")
    public static class ImportFailureDetail {

        @Schema(description = "记录索引（从0开始）")
        private Integer index;

        @Schema(description = "发票编号")
        private String invoiceNumber;

        @Schema(description = "失败原因")
        private String errorMessage;
    }
}
