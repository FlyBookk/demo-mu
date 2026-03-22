package com.musheng.business.document.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.musheng.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * PO采购订单主表实体
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_document_po")
@Schema(description = "PO采购订单主表")
public class DocumentPo extends BaseEntity {

    @Schema(description = "店铺ID")
    private Long shopId;

    @Schema(description = "单据编号（如20250902001）")
    private String documentNo;

    @Schema(description = "站点代码（US/CA/UK/EU）")
    private String siteCode;

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
}
