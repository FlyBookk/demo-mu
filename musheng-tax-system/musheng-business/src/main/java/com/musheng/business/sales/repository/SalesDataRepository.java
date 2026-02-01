package com.musheng.business.sales.repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.business.sales.dto.SalesQueryRequest;
import com.musheng.business.sales.entity.SalesData;

import java.util.List;

/**
 * 销售数据仓储接口
 * 
 * 封装销售数据的数据访问逻辑，提供统一的数据访问接口。
 * 
 * ⚠️ 注意: 所有方法的返回结果必须与直接使用 Mapper 完全一致
 *
 * @author wanhua
 * 10:30 2026年02月01日
 */
public interface SalesDataRepository {

    /**
     * 分页查询销售数据
     *
     * @param query 查询条件
     * @param page 页码（从1开始）
     * @param size 每页条数
     * @return 分页结果
     * @author wanhua
     * 10:30 2026年02月02日
     */
    Page<SalesData> findByQuery(SalesQueryRequest query, int page, int size);

    /**
     * 根据ID查询销售数据
     *
     * @param id 销售数据ID
     * @return 销售数据实体，不存在返回null
     * @author wanhua
     * 10:30 2026年02月02日
     */
    SalesData findById(Long id);

    /**
     * 检查是否存在重复数据（根据订单号和交易分类）
     *
     * @param orderId 订单号
     * @param category 交易分类
     * @return 存在返回true，否则返回false
     * @author wanhua
     * 10:30 2026年02月02日
     */
    boolean existsByOrderIdAndCategory(String orderId, String category);

    /**
     * 保存销售数据
     *
     * @param salesData 销售数据实体
     * @author wanhua
     * 10:30 2026年02月02日
     */
    void save(SalesData salesData);

    /**
     * 批量保存销售数据
     *
     * @param salesDataList 销售数据列表
     * @author wanhua
     * 10:30 2026年02月02日
     */
    void saveBatch(List<SalesData> salesDataList);

    /**
     * 根据ID删除销售数据
     *
     * @param id 销售数据ID
     * @author wanhua
     * 10:30 2026年02月02日
     */
    void deleteById(Long id);

    /**
     * 批量删除销售数据
     *
     * @param ids 销售数据ID列表
     * @author wanhua
     * 10:30 2026年02月02日
     */
    void deleteByIds(List<Long> ids);

    /**
     * 查询销售数据列表（不分页，用于统计等场景）
     *
     * @param query 查询条件
     * @return 销售数据列表
     * @author wanhua
     * 10:30 2026年02月02日
     */
    List<SalesData> findListByQuery(SalesQueryRequest query);
}
