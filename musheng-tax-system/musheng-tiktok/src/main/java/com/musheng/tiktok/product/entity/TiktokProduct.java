package com.musheng.tiktok.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * TK商品库实体（SKU对照表）
 *
 * @author wanhua
 * 19:15 2026年05月14日
 */
@Data
@TableName("t_tiktok_product")
public class TiktokProduct {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 店铺ID */
    private Long shopId;

    /** 站点代码 */
    private String siteCode;

    /** TK商品ID */
    private String productId;

    /** TK SKU ID（结算单关联键） */
    private String skuId;

    /** MSKU编码（seller_sku） */
    private String msku;

    /** 商品名称 */
    private String productName;

    /** 类目 */
    private String category;

    /** 变体选项 */
    private String variationValue;

    /** 零售价（本地币种） */
    private BigDecimal price;

    /** 状态（1启用/0禁用） */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
