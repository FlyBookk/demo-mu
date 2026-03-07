package com.musheng.business.document.generator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 货件输入数据（纯函数设计，不依赖数据库实体）
 *
 * <p>用于生成器的输入参数，包含货件的基本信息和MSKU明细。
 * 与数据库实体解耦，确保生成器为纯函数。</p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentInput {

    /** 货件编号 */
    private String shipmentNo;

    /** 货件名称 */
    private String shipmentName;

    /** 货件创建时间 */
    private LocalDateTime createTime;

    /** MSKU明细列表 */
    private List<MskuItem> items;

    /** 收货街道地址 */
    private String streetAddress;

    /** 收货城市 */
    private String city;

    /** 收货州/省 */
    private String stateProvince;

    /** 收货邮编 */
    private String postalCode;

    /** 收货国家 */
    private String country;

    /**
     * 获取完整FBA仓库收货地址
     *
     * <p>格式：{街道地址}, {城市}, {州省}, {邮编}, {国家}</p>
     *
     * @return 完整收货地址字符串
     * @author wanhua
     * 10:30 2026年01月29日
     */
    public String getFullAddress() {
        return String.join(", ",
                streetAddress != null ? streetAddress : "",
                city != null ? city : "",
                stateProvince != null ? stateProvince : "",
                postalCode != null ? postalCode : "",
                country != null ? country : "");
    }

    /**
     * MSKU明细项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MskuItem {

        /** MSKU编码 */
        private String msku;

        /** 数量 */
        private Integer quantity;
    }
}
