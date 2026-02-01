package com.musheng.business.fbashipment.repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.business.fbashipment.entity.FbaShipment;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * FBA货件数据仓储接口
 * 
 * 封装FBA货件的数据访问逻辑，提供统一的数据访问接口。
 * 
 * ⚠️ 注意: 所有方法的返回结果必须与直接使用 Mapper 完全一致
 *
 * @author wanhua
 * 10:30 2026年02月02日
 */
public interface FbaShipmentRepository {

    /**
     * 分页查询FBA货件
     *
     * @param shipmentId 货件编号（模糊查询，可选）
     * @param shopName 店铺名称（模糊查询，可选）
     * @param country 国家/地区（精确匹配，可选）
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @param page 页码（从1开始）
     * @param size 每页条数
     * @return 分页结果
     * @author wanhua
     * 10:30 2026年02月02日
     */
    Page<FbaShipment> findByQuery(String shipmentId, String shopName, String country,
                                   String startDate, String endDate, int page, int size);

    /**
     * 根据ID查询FBA货件
     *
     * @param id 货件ID
     * @return 货件实体，使用Optional包装
     * @author wanhua
     * 10:30 2026年02月02日
     */
    Optional<FbaShipment> findById(Long id);

    /**
     * 根据货件编号查询FBA货件
     *
     * @param shipmentId 货件编号
     * @return 货件实体，使用Optional包装
     * @author wanhua
     * 10:30 2026年02月02日
     */
    Optional<FbaShipment> findByShipmentId(String shipmentId);

    /**
     * 检查指定货件编号是否存在
     *
     * @param shipmentId 货件编号
     * @return 存在返回true，否则返回false
     * @author wanhua
     * 10:30 2026年02月02日
     */
    boolean existsByShipmentId(String shipmentId);

    /**
     * 批量检查货件编号是否存在
     * 
     * 用于导入时的去重检测，返回已存在的货件编号集合
     *
     * @param shipmentIds 货件编号集合
     * @return 已存在的货件编号集合
     * @author wanhua
     * 10:30 2026年02月02日
     */
    Set<String> findExistingShipmentIds(Set<String> shipmentIds);

    /**
     * 保存FBA货件
     *
     * @param shipment 货件实体
     * @author wanhua
     * 10:30 2026年02月02日
     */
    void save(FbaShipment shipment);

    /**
     * 批量保存FBA货件
     *
     * @param shipments 货件列表
     * @author wanhua
     * 10:30 2026年02月02日
     */
    void saveBatch(List<FbaShipment> shipments);

    /**
     * 根据ID删除FBA货件
     *
     * @param id 货件ID
     * @author wanhua
     * 10:30 2026年02月02日
     */
    void deleteById(Long id);

    /**
     * 批量删除FBA货件
     *
     * @param ids 货件ID列表
     * @author wanhua
     * 10:30 2026年02月02日
     */
    void deleteByIds(List<Long> ids);

    /**
     * 查询货件列表（不分页，用于统计和导出）
     *
     * @param shopName 店铺名称（模糊查询，可选）
     * @param country 国家/地区（精确匹配，可选）
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @return 货件列表
     * @author wanhua
     * 10:30 2026年02月02日
     */
    List<FbaShipment> findListByQuery(String shopName, String country,
                                       String startDate, String endDate);

    /**
     * 获取所有国家/地区列表（去重）
     * 
     * 用于前端筛选下拉框
     *
     * @return 国家/地区列表
     * @author wanhua
     * 10:30 2026年02月02日
     */
    List<String> findDistinctCountries();

    /**
     * 获取所有店铺名称列表（去重）
     * 
     * 用于前端筛选下拉框
     *
     * @return 店铺名称列表
     * @author wanhua
     * 10:30 2026年02月02日
     */
    List<String> findDistinctShopNames();

    /**
     * 统计符合条件的货件数量
     *
     * @param shopName 店铺名称（模糊查询，可选）
     * @param country 国家/地区（精确匹配，可选）
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @return 货件数量
     * @author wanhua
     * 10:30 2026年02月02日
     */
    long countByQuery(String shopName, String country, String startDate, String endDate);
}
