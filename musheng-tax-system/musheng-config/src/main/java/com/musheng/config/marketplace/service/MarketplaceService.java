package com.musheng.config.marketplace.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.config.marketplace.dto.MarketplaceQueryRequest;
import com.musheng.config.marketplace.dto.MarketplaceRequest;
import com.musheng.config.marketplace.entity.Marketplace;

/**
 * 站点服务接口
 */
public interface MarketplaceService {

    /**
     * 创建站点
     *
     * @param request 请求数据
     * @return 创建的实体
     */
    Marketplace create(MarketplaceRequest request);

    /**
     * 更新站点
     *
     * @param id      实体ID
     * @param request 请求数据
     * @return 更新后的实体
     */
    Marketplace update(Long id, MarketplaceRequest request);

    /**
     * 删除站点
     *
     * @param id 实体ID
     */
    void delete(Long id);

    /**
     * 根据ID获取站点
     *
     * @param id 实体ID
     * @return 实体
     */
    Marketplace getById(Long id);

    /**
     * 分页查询站点
     *
     * @param queryRequest 查询参数
     * @return 分页结果
     */
    Page<Marketplace> list(MarketplaceQueryRequest queryRequest);

    /**
     * 获取所有启用的站点
     *
     * @return 启用的站点列表
     */
    java.util.List<Marketplace> getEnabled();
}
