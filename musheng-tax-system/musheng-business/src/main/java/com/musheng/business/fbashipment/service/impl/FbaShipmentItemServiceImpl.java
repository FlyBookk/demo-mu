package com.musheng.business.fbashipment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.business.fbashipment.entity.FbaShipment;
import com.musheng.business.fbashipment.entity.FbaShipmentItem;
import com.musheng.business.fbashipment.mapper.FbaShipmentItemMapper;
import com.musheng.business.fbashipment.mapper.FbaShipmentMapper;
import com.musheng.business.fbashipment.service.FbaShipmentItemService;
import com.musheng.common.context.ShopContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * FBA货件明细服务实现
 * 提供SKU级别的全局查询功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FbaShipmentItemServiceImpl implements FbaShipmentItemService {

    private final FbaShipmentItemMapper fbaShipmentItemMapper;
    private final FbaShipmentMapper fbaShipmentMapper;

    @Override
    public Page<FbaShipmentItem> listItems(String shipmentNo, String sku, String msku,
                                          String shopName, String country,
                                          String startDate, String endDate,
                                          int page, int size) {
        // 店铺数据隔离
        Long shopId = ShopContext.requireShopId();

        // 如果有店铺名称或国家筛选，需要先查询货件主表获取符合条件的货件ID
        List<Long> shipmentIds = null;
        if (StringUtils.hasText(shopName) || StringUtils.hasText(country) ||
            StringUtils.hasText(startDate) || StringUtils.hasText(endDate)) {

            LambdaQueryWrapper<FbaShipment> shipmentWrapper = new LambdaQueryWrapper<>();
            shipmentWrapper.eq(FbaShipment::getShopId, shopId);

            if (StringUtils.hasText(shopName)) {
                shipmentWrapper.like(FbaShipment::getShopName, shopName);
            }
            if (StringUtils.hasText(country)) {
                shipmentWrapper.eq(FbaShipment::getCountry, country);
            }
            if (StringUtils.hasText(startDate)) {
                try {
                    LocalDate start = LocalDate.parse(startDate);
                    shipmentWrapper.ge(FbaShipment::getCreatedDate, start.atStartOfDay());
                } catch (Exception e) {
                    log.warn("开始日期格式错误: {}", startDate);
                }
            }
            if (StringUtils.hasText(endDate)) {
                try {
                    LocalDate end = LocalDate.parse(endDate);
                    shipmentWrapper.le(FbaShipment::getCreatedDate, end.atTime(23, 59, 59));
                } catch (Exception e) {
                    log.warn("结束日期格式错误: {}", endDate);
                }
            }

            shipmentWrapper.select(FbaShipment::getId);
            List<FbaShipment> shipments = fbaShipmentMapper.selectList(shipmentWrapper);
            shipmentIds = shipments.stream().map(FbaShipment::getId).collect(Collectors.toList());

            // 如果没有符合条件的货件，直接返回空结果
            if (shipmentIds.isEmpty()) {
                return new Page<>(page, size);
            }
        }

        // 构建明细查询条件
        LambdaQueryWrapper<FbaShipmentItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FbaShipmentItem::getShopId, shopId);

        // 货件ID筛选
        if (shipmentIds != null && !shipmentIds.isEmpty()) {
            wrapper.in(FbaShipmentItem::getShipmentId, shipmentIds);
        }

        // 货件单号筛选
        if (StringUtils.hasText(shipmentNo)) {
            wrapper.like(FbaShipmentItem::getShipmentNo, shipmentNo);
        }

        // SKU筛选
        if (StringUtils.hasText(sku)) {
            wrapper.like(FbaShipmentItem::getSku, sku);
        }

        // MSKU筛选
        if (StringUtils.hasText(msku)) {
            wrapper.like(FbaShipmentItem::getMsku, msku);
        }

        // 按创建时间倒序
        wrapper.orderByDesc(FbaShipmentItem::getCreateTime);

        return fbaShipmentItemMapper.selectPage(new Page<>(page, size), wrapper);
    }
}
