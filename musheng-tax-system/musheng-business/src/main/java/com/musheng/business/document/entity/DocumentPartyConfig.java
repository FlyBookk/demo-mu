package com.musheng.business.document.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * FBA单据交易方配置实体
 *
 * <p>存储各店铺（siteCode）的交易方信息，同时作为生成器参数类型，
 * 替代原有的 DocumentPartyProperties（application.yml 配置方式）。</p>
 *
 * @author wanhua
 * 10:30 2026年03月22日
 */
@Data
@TableName("t_document_party_config")
@Schema(description = "FBA单据交易方配置")
public class DocumentPartyConfig {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键")
    private Long id;

    @Schema(description = "站点代码（US/CA/UK/EU），唯一")
    private String siteCode;

    @Schema(description = "买方中文名（必填）")
    private String buyerName;

    @Schema(description = "买方地址")
    private String buyerAddress;

    @Schema(description = "买方电话")
    private String buyerPhone;

    @Schema(description = "买方英文名")
    private String buyerNameEn;

    @Schema(description = "卖方名称（必填）")
    private String sellerName;

    @Schema(description = "卖方地址")
    private String sellerAddress;

    @Schema(description = "卖方电话")
    private String sellerPhone;

    @Schema(description = "供应商名称（必填）")
    private String supplierName;

    @Schema(description = "客户繁体名（必填）")
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

    @TableLogic
    @Schema(description = "逻辑删除（0=未删除，1=已删除）")
    private Boolean deleted;
}
