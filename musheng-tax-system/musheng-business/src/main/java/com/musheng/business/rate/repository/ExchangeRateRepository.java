package com.musheng.business.rate.repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.business.rate.entity.ExchangeRate;

import java.time.LocalDate;
import java.util.List;

/**
 * 汇率数据仓储接口
 * 
 * 封装汇率数据的数据访问逻辑，提供统一的数据访问接口。
 * 
 * ⚠️ 注意: 所有方法的返回结果必须与直接使用 Mapper 完全一致
 *
 * @author wanhua
 * 10:30 2026年02月01日
 */
public interface ExchangeRateRepository {

    /**
     * 分页查询汇率数据
     *
     * @param currencyCode 货币代码（可选）
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @param source 数据来源（可选）
     * @param page 页码（从1开始）
     * @param size 每页条数
     * @return 分页结果
     * @author wanhua
     * 10:30 2026年02月02日
     */
    Page<ExchangeRate> findByQuery(String currencyCode, LocalDate startDate, 
                                    LocalDate endDate, String source, int page, int size);

    /**
     * 根据ID查询汇率数据
     *
     * @param id 汇率数据ID
     * @return 汇率数据实体，不存在返回null
     * @author wanhua
     * 10:30 2026年02月02日
     */
    ExchangeRate findById(Long id);

    /**
     * 根据货币代码和日期查询汇率
     *
     * @param currencyCode 货币代码
     * @param date 汇率日期
     * @return 汇率数据实体，不存在返回null
     * @author wanhua
     * 10:30 2026年02月02日
     */
    ExchangeRate findByCurrencyAndDate(String currencyCode, LocalDate date);

    /**
     * 检查指定货币和日期的汇率是否存在
     *
     * @param currencyCode 货币代码
     * @param date 汇率日期
     * @return 存在返回true，否则返回false
     * @author wanhua
     * 10:30 2026年02月02日
     */
    boolean existsByCurrencyAndDate(String currencyCode, LocalDate date);

    /**
     * 检查指定货币和日期的汇率是否存在（排除指定ID）
     *
     * @param currencyCode 货币代码
     * @param date 汇率日期
     * @param excludeId 排除的ID
     * @return 存在返回true，否则返回false
     * @author wanhua
     * 10:30 2026年02月02日
     */
    boolean existsByCurrencyAndDateExcludeId(String currencyCode, LocalDate date, Long excludeId);

    /**
     * 查询指定日期及之后最早有汇率的记录
     * 用于回退兜底：当指定日期之前无汇率数据时，取之后最近的汇率
     *
     * @param currencyCode 货币代码
     * @param date 起始日期（含）
     * @return 汇率数据实体，不存在返回null
     */
    ExchangeRate findEarliestOnOrAfter(String currencyCode, LocalDate date);

    /**
     * 查询指定日期及之前最近的汇率（含当天）
     * 用于节假日前移：遇假期/周末无汇率时，取前一个有汇率的日期
     *
     * @param currencyCode 货币代码
     * @param date 截止日期（含）
     * @return 汇率数据实体，不存在返回null
     * @author wanhua
     * 10:30 2026年03月14日
     */
    ExchangeRate findLatestBefore(String currencyCode, LocalDate date);

    /**
     * 查询指定货币的最新汇率
     *
     * @param currencyCode 货币代码
     * @return 汇率数据实体，不存在返回null
     * @author wanhua
     * 10:30 2026年02月02日
     */
    ExchangeRate findLatestByCurrency(String currencyCode);

    /**
     * 保存汇率数据
     *
     * @param rate 汇率数据实体
     * @author wanhua
     * 10:30 2026年02月02日
     */
    void save(ExchangeRate rate);

    /**
     * 批量保存汇率数据
     *
     * @param rates 汇率数据列表
     * @author wanhua
     * 10:30 2026年02月02日
     */
    void saveBatch(List<ExchangeRate> rates);

    /**
     * 根据ID删除汇率数据
     *
     * @param id 汇率数据ID
     * @author wanhua
     * 10:30 2026年02月02日
     */
    void deleteById(Long id);

    /**
     * 批量删除汇率数据
     *
     * @param ids 汇率数据ID列表
     * @author wanhua
     * 10:30 2026年02月02日
     */
    void deleteByIds(List<Long> ids);

    /**
     * 统计指定货币和日期的汇率数量
     *
     * @param currencyCode 货币代码
     * @param date 汇率日期
     * @return 数量
     * @author wanhua
     * 10:30 2026年02月02日
     */
    long countByCurrencyAndDate(String currencyCode, LocalDate date);
}
