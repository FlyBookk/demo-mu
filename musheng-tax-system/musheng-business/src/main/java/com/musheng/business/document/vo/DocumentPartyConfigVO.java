package com.musheng.business.document.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * FBA单据交易方配置响应 VO
 *
 * @author wanhua
 * 10:30 2026年03月22日
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "FBA单据交易方配置响应")
public class DocumentPartyConfigVO {

    @Schema(description = "配置ID")
    private Long id;

    @Schema(description = "站点代码（US/CA/UK/EU）")
    private String siteCode;

    @Schema(description = "买方中文名")
    private String buyerName;

    @Schema(description = "买方地址")
    private String buyerAddress;

    @Schema(description = "买方电话")
    private String buyerPhone;

    @Schema(description = "买方英文名")
    private String buyerNameEn;

    @Schema(description = "卖方名称")
    private String sellerName;

    @Schema(description = "卖方地址")
    private String sellerAddress;

    @Schema(description = "卖方电话")
    private String sellerPhone;

    @Schema(description = "供应商名称")
    private String supplierName;

    @Schema(description = "客户繁体名")
    private String customerNameTc;

    @Schema(description = "银行账户名")
    private String bankAccountName;

    @Schema(description = "银行账号")
    private String bankAccountNumber;

    @Schema(description = "银行名称")
    private String bankName;

    @Schema(description = "银行地址")
    private String bankAddress;

    @Schema(description = "SWIFT代码")
    private String swiftCode;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
