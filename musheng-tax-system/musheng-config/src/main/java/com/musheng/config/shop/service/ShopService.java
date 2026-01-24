package com.musheng.config.shop.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.musheng.config.shop.dto.ShopQueryRequest;
import com.musheng.config.shop.dto.ShopRequest;
import com.musheng.config.shop.entity.Shop;

import java.util.List;

/**
 * 店铺服务接口
 */
public interface ShopService extends IService<Shop> {

    /**
     * 分页查询店铺
     *
     * @param request 查询条件
     * @return 分页结果
     */
    IPage<Shop> queryPage(ShopQueryRequest request);

    /**
     * 查询所有启用的店铺（下拉选项）
     *
     * @return 店铺列表
     */
    List<Shop> listEnabled();

    /**
     * 新增店铺
     *
     * @param request 店铺信息
     * @return 店铺ID
     */
    Long createShop(ShopRequest request);

    /**
     * 更新店铺
     *
     * @param id      店铺ID
     * @param request 店铺信息
     */
    void updateShop(Long id, ShopRequest request);

    /**
     * 删除店铺
     *
     * @param id 店铺ID
     */
    void deleteShop(Long id);

    /**
     * 根据编码查询店铺
     *
     * @param shopCode 店铺编码
     * @return 店铺信息
     */
    Shop getByCode(String shopCode);
}
