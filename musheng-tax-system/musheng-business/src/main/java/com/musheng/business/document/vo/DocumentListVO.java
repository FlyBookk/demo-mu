package com.musheng.business.document.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单据列表通用视图
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "单据列表通用视图")
public class DocumentListVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "单据类型")
    private String documentType;

    @Schema(description = "单据编号")
    private String documentNo;

    @Schema(description = "单据日期")
    private LocalDate documentDate;

    @Schema(description = "买方名称")
    private String buyerName;

    @Schema(description = "卖方名称")
    private String sellerName;

    @Schema(description = "总数量")
    private Integer totalQuantity;

    @Schema(description = "总金额（结算单和INV有值，PO和DN为null）")
    private BigDecimal totalAmount;

    @Schema(description = "站点代码（US/CA/UK/EU）")
    private String siteCode;

    @Schema(description = "导出时间（数据创建时间）")
    private LocalDateTime createTime;
}
