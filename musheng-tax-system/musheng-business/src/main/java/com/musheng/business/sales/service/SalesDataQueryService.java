package com.musheng.business.sales.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.business.sales.dto.SalesQueryRequest;
import com.musheng.business.sales.entity.SalesData;

import java.util.List;

/**
 * 销售数据查询服务接口
 * 
 * 职责：
 * 1. 分页查询销售数据
 * 2. 根据ID获取销售数据
 * 3. 删除销售数据
 * 4. 批量删除销售数据
 * 
 * @author wanhua
 * 10:30 2026年02月01日
 */
public interface SalesDataQueryService {

    /**
     * 分页查询销售数据
     *
     * @param request 查询请求
     * @return 分页结果
     */
    Page<SalesData> list(SalesQueryRequest request);

    /**
     * 根据ID获取销售数据
     *
     * @param id 实体ID
     * @return 实体
     */
    SalesData getById(Long id);

    /**
     * 删除销售数据
     *
     * @param id 实体ID
     */
    void delete(Long id);

    /**
     * 批量删除销售数据
     *
     * @param ids 实体ID列表
     */
    void batchDelete(List<Long> ids);
}
