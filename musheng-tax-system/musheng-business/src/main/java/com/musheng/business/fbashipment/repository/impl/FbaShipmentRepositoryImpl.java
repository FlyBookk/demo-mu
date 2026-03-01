package com.musheng.business.fbashipment.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.business.common.utils.DateParseUtils;
import com.musheng.business.fbashipment.entity.FbaShipment;
import com.musheng.business.fbashipment.mapper.FbaShipmentMapper;
import com.musheng.business.fbashipment.repository.FbaShipmentRepository;
import com.musheng.common.context.ShopContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * FBA货件数据仓储实现类
 * 
 * 封装FBA货件的数据访问逻辑，保持与原有 FbaShipmentServiceImpl 中的数据访问逻辑完全一致。
 * 
 * ⚠️ 核心原则：
 * 1. 禁止修改业务流程
 * 2. 禁止改变输出结果
 * 3. 只是将数据访问逻辑从 Service 移动到 Repository
 *
 * @author wanhua
 * 10:30 2026年02月02日
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class FbaShipmentRepositoryImpl implements FbaShipmentRepository {

    private final FbaShipmentMapper fbaShipmentMapper;

    @Override
    public Page<FbaShipment> findByQuery(String shipmentId, String status, String shopName, String country,
                                          String startDate, String endDate, int page, int size) {
        LambdaQueryWrapper<FbaShipment> wrapper = new LambdaQueryWrapper<>();

        // 店铺数据隔离
        Long shopId = ShopContext.requireShopId();
        wrapper.eq(FbaShipment::getShopId, shopId);

        // 条件筛选 - 货件编号模糊查询
        if (StringUtils.hasText(shipmentId)) {
            wrapper.like(FbaShipment::getShipmentId, shipmentId);
        }
        // 货件状态
        if (StringUtils.hasText(status)) {
            wrapper.eq(FbaShipment::getStatus, status);
        }
        // 店铺名称模糊查询
        if (StringUtils.hasText(shopName)) {
            wrapper.like(FbaShipment::getShopName, shopName);
        }
        // 国家精确匹配
        if (StringUtils.hasText(country)) {
            wrapper.eq(FbaShipment::getCountry, country);
        }
        
        // 使用 DateParseUtils 解析日期 - 与原有逻辑一致
        LocalDateTime startDateTime = DateParseUtils.parseStartDate(startDate);
        LocalDateTime endDateTime = DateParseUtils.parseEndDate(endDate);
        if (startDateTime != null) {
            wrapper.ge(FbaShipment::getCreatedDate, startDateTime);
        }
        if (endDateTime != null) {
            wrapper.le(FbaShipment::getCreatedDate, endDateTime);
        }

        // 按创建时间倒序排序
        wrapper.orderByDesc(FbaShipment::getCreatedDate);

        return fbaShipmentMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public Optional<FbaShipment> findById(Long id) {
        return Optional.ofNullable(fbaShipmentMapper.selectById(id));
    }

    @Override
    public Optional<FbaShipment> findByShipmentId(String shipmentId) {
        if (!StringUtils.hasText(shipmentId)) {
            return Optional.empty();
        }
        
        Long shopId = ShopContext.requireShopId();
        LambdaQueryWrapper<FbaShipment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FbaShipment::getShopId, shopId)
                .eq(FbaShipment::getShipmentId, shipmentId);
        
        return Optional.ofNullable(fbaShipmentMapper.selectOne(wrapper));
    }

    @Override
    public boolean existsByShipmentId(String shipmentId) {
        if (!StringUtils.hasText(shipmentId)) {
            return false;
        }
        
        Long shopId = ShopContext.requireShopId();
        LambdaQueryWrapper<FbaShipment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FbaShipment::getShopId, shopId)
                .eq(FbaShipment::getShipmentId, shipmentId);
        
        return fbaShipmentMapper.selectCount(wrapper) > 0;
    }

    @Override
    public Set<String> findExistingShipmentIds(Set<String> shipmentIds) {
        // ⚠️ 逻辑与原 FbaShipmentServiceImpl.batchCheckDuplicates() 方法完全一致
        if (shipmentIds == null || shipmentIds.isEmpty()) {
            return Collections.emptySet();
        }

        Long shopId = ShopContext.requireShopId();
        
        // 批量查询已存在的货件
        LambdaQueryWrapper<FbaShipment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FbaShipment::getShopId, shopId)
                .in(FbaShipment::getShipmentId, shipmentIds)
                .select(FbaShipment::getShipmentId);

        List<FbaShipment> existing = fbaShipmentMapper.selectList(wrapper);

        Set<String> existingIds = new HashSet<>();
        for (FbaShipment shipment : existing) {
            existingIds.add(shipment.getShipmentId());
        }

        return existingIds;
    }

    @Override
    public void save(FbaShipment shipment) {
        fbaShipmentMapper.insert(shipment);
        log.debug("保存FBA货件: shipmentId={}", shipment.getShipmentId());
    }

    @Override
    public void saveBatch(List<FbaShipment> shipments) {
        if (shipments == null || shipments.isEmpty()) {
            return;
        }
        // 使用循环插入，保持与原有逻辑一致
        for (FbaShipment shipment : shipments) {
            fbaShipmentMapper.insert(shipment);
        }
        log.info("批量保存FBA货件: count={}", shipments.size());
    }

    @Override
    public void deleteById(Long id) {
        fbaShipmentMapper.deleteById(id);
        log.info("删除FBA货件: id={}", id);
    }

    @Override
    public void deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        fbaShipmentMapper.deleteBatchIds(ids);
        log.info("批量删除FBA货件: ids={}", ids);
    }

    @Override
    public List<FbaShipment> findListByQuery(String status, String shopName, String country,
                                              String startDate, String endDate) {
        LambdaQueryWrapper<FbaShipment> wrapper = new LambdaQueryWrapper<>();

        // 店铺数据隔离
        Long shopId = ShopContext.requireShopId();
        wrapper.eq(FbaShipment::getShopId, shopId);

        // 条件筛选
        if (StringUtils.hasText(status)) {
            wrapper.eq(FbaShipment::getStatus, status);
        }
        if (StringUtils.hasText(shopName)) {
            wrapper.like(FbaShipment::getShopName, shopName);
        }
        if (StringUtils.hasText(country)) {
            wrapper.eq(FbaShipment::getCountry, country);
        }
        
        // 使用 DateParseUtils 解析日期
        LocalDateTime startDateTime = DateParseUtils.parseStartDate(startDate);
        LocalDateTime endDateTime = DateParseUtils.parseEndDate(endDate);
        if (startDateTime != null) {
            wrapper.ge(FbaShipment::getCreatedDate, startDateTime);
        }
        if (endDateTime != null) {
            wrapper.le(FbaShipment::getCreatedDate, endDateTime);
        }

        return fbaShipmentMapper.selectList(wrapper);
    }

    @Override
    public List<String> findDistinctCountries() {
        // ⚠️ 逻辑与原 FbaShipmentServiceImpl.getCountryList() 方法完全一致
        Long shopId = ShopContext.requireShopId();

        LambdaQueryWrapper<FbaShipment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FbaShipment::getShopId, shopId)
                .select(FbaShipment::getCountry)
                .groupBy(FbaShipment::getCountry)
                .orderByAsc(FbaShipment::getCountry);

        List<FbaShipment> list = fbaShipmentMapper.selectList(wrapper);
        return list.stream()
                .map(FbaShipment::getCountry)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public List<String> findDistinctShopNames() {
        // ⚠️ 逻辑与原 FbaShipmentServiceImpl.getShopNameList() 方法完全一致
        Long shopId = ShopContext.requireShopId();

        LambdaQueryWrapper<FbaShipment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FbaShipment::getShopId, shopId)
                .select(FbaShipment::getShopName)
                .groupBy(FbaShipment::getShopName)
                .orderByAsc(FbaShipment::getShopName);

        List<FbaShipment> list = fbaShipmentMapper.selectList(wrapper);
        return list.stream()
                .map(FbaShipment::getShopName)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public long countByQuery(String status, String shopName, String country, String startDate, String endDate) {
        LambdaQueryWrapper<FbaShipment> wrapper = new LambdaQueryWrapper<>();

        // 店铺数据隔离
        Long shopId = ShopContext.requireShopId();
        wrapper.eq(FbaShipment::getShopId, shopId);

        // 条件筛选
        if (StringUtils.hasText(status)) {
            wrapper.eq(FbaShipment::getStatus, status);
        }
        if (StringUtils.hasText(shopName)) {
            wrapper.like(FbaShipment::getShopName, shopName);
        }
        if (StringUtils.hasText(country)) {
            wrapper.eq(FbaShipment::getCountry, country);
        }
        
        // 使用 DateParseUtils 解析日期
        LocalDateTime startDateTime = DateParseUtils.parseStartDate(startDate);
        LocalDateTime endDateTime = DateParseUtils.parseEndDate(endDate);
        if (startDateTime != null) {
            wrapper.ge(FbaShipment::getCreatedDate, startDateTime);
        }
        if (endDateTime != null) {
            wrapper.le(FbaShipment::getCreatedDate, endDateTime);
        }

        return fbaShipmentMapper.selectCount(wrapper);
    }
}
