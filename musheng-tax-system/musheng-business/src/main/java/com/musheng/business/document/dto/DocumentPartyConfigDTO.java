package com.musheng.business.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * FBA单据交易方配置请求 DTO
 *
 * <p>用于新增和修改交易方配置的请求参数封装，包含必填字段校验。</p>
 *
 * @author wanhua
 * 10:30 2026年03月22日
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "FBA单据交易方配置请求")
public class DocumentPartyConfigDTO {

    @Schema(description = "配置ID（修改时必填）")
    private Long id;

    @NotBlank(message = "站点代码不能为空")
    @Schema(description = "站点代码（US/CA/UK/EU）")
    private String siteCode;

    @NotBlank(message = "买方中文名不能为空")
    @Schema(description = "买方中文名")
    private String buyerName;

    @Schema(description = "买方地址")
    private String buyerAddress;

    @Schema(description = "买方电话")
    private String buyerPhone;

    @Schema(description = "买方英文名")
    private String buyerNameEn;

    @NotBlank(message = "卖方名称不能为空")
    @Schema(description = "卖方名称")
    private String sellerName;

    @Schema(description = "卖方地址")
    private String sellerAddress;

    @Schema(description = "卖方电话")
    private String sellerPhone;

    @NotBlank(message = "供应商名称不能为空")
    @Schema(description = "供应商名称")
    private String supplierName;

    @NotBlank(message = "客户繁体名不能为空")
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
}
