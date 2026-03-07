package com.musheng.business.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

/**
 * MSKU列表查询请求
 *
 * @author wanhua
 * 14:00 2026年03月07日
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "MSKU列表查询请求")
public class MskuQueryRequest {

    @Schema(description = "站点代码")
    private String siteCode;

    @Schema(description = "MSKU编码（模糊搜索）")
    private String msku;

    @Schema(description = "结算周期起始日")
    private LocalDate periodStart;

    @Schema(description = "结算周期结束日")
    private LocalDate periodEnd;

    @Schema(description = "页码", example = "1")
    private Integer pageNum;

    @Schema(description = "每页条数", example = "20")
    private Integer pageSize;
}
