package com.musheng.business.fbashipment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.business.fbashipment.entity.FbaShipmentItem;

/**
 * FBA货件明细服务接口
 * 提供SKU级别的全局查询功能
 */
public interface FbaShipmentItemService {

    /**
     * 分页查询SKU明细列表（全局视图）
     *
     * @param shipmentNo 货件单号（模糊查询）
     * @param sku 内部SKU（模糊查询）
     * @param msku 亚马逊MSKU（模糊查询）
     * @param shopName 店铺名称
     * @param country 国家
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param page 页码
     * @param size 每页条数
     * @return 分页结果
     */
    Page<FbaShipmentItem> listItems(String shipmentNo, String sku, String msku,
                                    String shopName, String country,
                                    String startDate, String endDate,
                                    int page, int size);
}
