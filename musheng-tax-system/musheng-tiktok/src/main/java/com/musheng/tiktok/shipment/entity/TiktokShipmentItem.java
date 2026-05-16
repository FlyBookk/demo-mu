package com.musheng.tiktok.shipment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * TK FBT货件明细实体
 *
 * @author wanhua
 * 19:36 2026年05月14日
 */
@Data
@TableName("t_tiktok_shipment_item")
public class TiktokShipmentItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long shopId;

    /** 站点代码 */
    private String siteCode;

    /** 货件单号 */
    private String shipmentId;

    /** MSKU编码 */
    private String msku;

    /** 申报量 */
    private Integer quantityDeclared;

    /** 签收量 */
    private Integer quantityReceived;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
