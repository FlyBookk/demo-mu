package com.musheng.business.sales.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.business.common.utils.QueryWrapperUtils;
import com.musheng.business.sales.dto.SalesQueryRequest;
import com.musheng.business.sales.entity.SalesData;
import com.musheng.business.sales.mapper.SalesDataMapper;
import com.musheng.business.sales.repository.SalesDataRepository;
import com.musheng.common.context.ShopContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 销售数据仓储实现类
 * 
 * 封装销售数据的数据访问逻辑，保持与原有 Service 中的数据访问逻辑完全一致。
 * 
 * ⚠️ 核心原则：
 * 1. 禁止修改业务流程
 * 2. 禁止改变输出结果
 * 3. 只是将数据访问逻辑从 Service 移动到 Repository
 *
 * @author wanhua
 * 10:30 2026年02月01日
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class SalesDataRepositoryImpl implements SalesDataRepository {

    private final SalesDataMapper salesDataMapper;

    @Override
    public Page<SalesData> findByQuery(SalesQueryRequest query, int page, int size) {
        LambdaQueryWrapper<SalesData> wrapper = buildQueryWrapper(query);
        wrapper.orderByDesc(SalesData::getTransactionDate);
        return salesDataMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public SalesData findById(Long id) {
        return salesDataMapper.selectById(id);
    }

    @Override
    public boolean existsByOrderIdAndCategory(String orderId, String category) {
        // ⚠️ 逻辑与原 SalesDataImportServiceImpl.isDuplicate() 方法完全一致
        LambdaQueryWrapper<SalesData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SalesData::getOrderId, orderId)
                .eq(SalesData::getTransactionCategory, category);
        return salesDataMapper.selectCount(wrapper) > 0;
    }

    @Override
    public void save(SalesData salesData) {
        salesDataMapper.insert(salesData);
    }

    @Override
    public void saveBatch(List<SalesData> salesDataList) {
        if (salesDataList == null || salesDataList.isEmpty()) {
            return;
        }
        // 使用 MyBatis-Plus 的批量插入
        for (SalesData salesData : salesDataList) {
            salesDataMapper.insert(salesData);
        }
    }

    @Override
    public void deleteById(Long id) {
        salesDataMapper.deleteById(id);
        log.info("Deleted sales data: id={}", id);
    }

    @Override
    public void deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        salesDataMapper.deleteBatchIds(ids);
        log.info("Batch deleted sales data: ids={}", ids);
    }

    @Override
    public List<SalesData> findListByQuery(SalesQueryRequest query) {
        LambdaQueryWrapper<SalesData> wrapper = buildQueryWrapper(query);
        wrapper.orderByDesc(SalesData::getTransactionDate);
        return salesDataMapper.selectList(wrapper);
    }

    /**
     * 构建查询条件
     * 
     * ⚠️ 逻辑与原 SalesDataQueryServiceImpl.list() 方法中的查询条件构建完全一致
     *
     * @param query 查询请求
     * @return 查询包装器
     */
    private LambdaQueryWrapper<SalesData> buildQueryWrapper(SalesQueryRequest query) {
        LambdaQueryWrapper<SalesData> wrapper = new LambdaQueryWrapper<>();

        // 店铺数据隔离 - 必须按当前店铺过滤
        Long shopId = ShopContext.requireShopId();
        QueryWrapperUtils.applyShopIdFilter(wrapper, SalesData::getShopId, shopId);

        // 数据来源过滤
        QueryWrapperUtils.applyEqFilter(wrapper, SalesData::getSourceType, query.getSourceType());

        // 站点过滤
        QueryWrapperUtils.applyEqFilter(wrapper, SalesData::getSiteCode, query.getSiteCode());

        // 结算ID过滤
        QueryWrapperUtils.applyEqFilter(wrapper, SalesData::getSettlementId, query.getSettlementId());

        // 交易分类过滤
        QueryWrapperUtils.applyEqFilter(wrapper, SalesData::getTransactionCategory, query.getTransactionCategory());

        // 交易类型过滤
        QueryWrapperUtils.applyEqFilter(wrapper, SalesData::getTransactionType, query.getTransactionType());

        // 关键字搜索：订单号、SKU
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w
                    .like(SalesData::getOrderId, query.getKeyword())
                    .or().like(SalesData::getSku, query.getKeyword())
            );
        }

        // 日期范围过滤
        // ⚠️ 保持与原有逻辑一致：使用字符串拼接方式
        if (StringUtils.hasText(query.getStartDate())) {
            wrapper.ge(SalesData::getTransactionDate, query.getStartDate() + " 00:00:00");
        }
        if (StringUtils.hasText(query.getEndDate())) {
            wrapper.le(SalesData::getTransactionDate, query.getEndDate() + " 23:59:59");
        }

        return wrapper;
    }
}
