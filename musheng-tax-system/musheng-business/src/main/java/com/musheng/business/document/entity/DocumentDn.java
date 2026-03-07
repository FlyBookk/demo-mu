package com.musheng.business.document.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.musheng.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 送货单主表实体
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_document_dn")
@Schema(description = "送货单主表")
public class DocumentDn extends BaseEntity {

    @Schema(description = "店铺ID")
    private Long shopId;

    @Schema(description = "单据编号")
    private String documentNo;

    @Schema(description = "送货日期")
    private LocalDate dnDate;

    @Schema(description = "供应商名称")
    private String supplierName;

    @Schema(description = "客户名称（繁体）")
    private String customerName;

    @Schema(description = "总数量")
    private Integer totalQuantity;

    @Schema(description = "DN周期起始日")
    private LocalDate periodStart;

    @Schema(description = "DN周期结束日")
    private LocalDate periodEnd;
}
