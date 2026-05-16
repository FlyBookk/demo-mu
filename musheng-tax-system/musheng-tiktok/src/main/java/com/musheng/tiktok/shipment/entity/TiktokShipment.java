package com.musheng.tiktok.shipment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * TK FBT货件汇总实体
 *
 * @author wanhua
 * 19:36 2026年05月14日
 */
@Data
@TableName("t_tiktok_shipment")
public class TiktokShipment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long shopId;

    /** 站点代码 */
    private String siteCode;

    /** 货件单号（IBRxxx） */
    private String shipmentId;

    /** 货件名称（Plan ID） */
    private String shipmentName;

    /** 状态 */
    private String status;

    /** Reference ID */
    private String referenceId;

    /** 收件人 */
    private String shipTo;

    /** 物流中心编码 */
    private String warehouseCode;

    /** 收件邮编 */
    private String postalCode;

    /** 收件国家 */
    private String country;

    /** 收件州/省 */
    private String state;

    /** 收件城市 */
    private String city;

    /** 收件街道地址 */
    private String streetAddress;

    /** SKU种类数 */
    private Integer totalSkus;

    /** 总数量 */
    private Integer totalQuantity;

    /** 创建时间（源数据） */
    private LocalDateTime creationTime;

    /** 更新时间（源数据） */
    private LocalDateTime updateTimeSource;

    /** 导入批次ID */
    private Long importBatchId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
