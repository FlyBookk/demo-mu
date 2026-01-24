package com.musheng.config.shop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.musheng.common.exception.BusinessException;
import com.musheng.config.shop.dto.ShopQueryRequest;
import com.musheng.config.shop.dto.ShopRequest;
import com.musheng.config.shop.entity.Shop;
import com.musheng.config.shop.mapper.ShopMapper;
import com.musheng.config.shop.service.ShopService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 店铺服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements ShopService {

    @Override
    public IPage<Shop> queryPage(ShopQueryRequest request) {
        LambdaQueryWrapper<Shop> wrapper = new LambdaQueryWrapper<>();
        
        // 店铺编码模糊查询
        wrapper.like(StringUtils.hasText(request.getShopCode()), 
                Shop::getShopCode, request.getShopCode());
        
        // 店铺名称模糊查询
        wrapper.like(StringUtils.hasText(request.getShopName()), 
                Shop::getShopName, request.getShopName());
        
        // 状态精确匹配
        wrapper.eq(request.getStatus() != null, 
                Shop::getStatus, request.getStatus());
        
        // 按创建时间倒序
        wrapper.orderByDesc(Shop::getCreateTime);
        
        return page(new Page<>(request.getPage(), request.getSize()), wrapper);
    }

    @Override
    public List<Shop> listEnabled() {
        return lambdaQuery()
                .eq(Shop::getStatus, 1)
                .orderByAsc(Shop::getShopCode)
                .list();
    }

    @Override
    public Long createShop(ShopRequest request) {
        // 检查编码唯一性
        Shop existing = getByCode(request.getShopCode());
        if (existing != null) {
            throw new BusinessException("店铺编码已存在: " + request.getShopCode());
        }
        
        Shop shop = new Shop();
        BeanUtils.copyProperties(request, shop);
        save(shop);
        
        log.info("创建店铺成功: {} - {}", shop.getId(), shop.getShopName());
        return shop.getId();
    }

    @Override
    public void updateShop(Long id, ShopRequest request) {
        Shop existing = getById(id);
        if (existing == null) {
            throw new BusinessException("店铺不存在: " + id);
        }
        
        // 如果修改了编码，检查唯一性
        if (!existing.getShopCode().equals(request.getShopCode())) {
            Shop byCode = getByCode(request.getShopCode());
            if (byCode != null) {
                throw new BusinessException("店铺编码已存在: " + request.getShopCode());
            }
        }
        
        BeanUtils.copyProperties(request, existing);
        existing.setId(id);
        updateById(existing);
        
        log.info("更新店铺成功: {} - {}", id, request.getShopName());
    }

    @Override
    public void deleteShop(Long id) {
        Shop shop = getById(id);
        if (shop == null) {
            throw new BusinessException("店铺不存在: " + id);
        }
        
        // TODO: 检查是否有关联的业务数据，如果有则不允许删除
        // 可以在后续阶段添加关联检查逻辑
        
        removeById(id);
        log.info("删除店铺成功: {} - {}", id, shop.getShopName());
    }

    @Override
    public Shop getByCode(String shopCode) {
        if (!StringUtils.hasText(shopCode)) {
            return null;
        }
        return lambdaQuery()
                .eq(Shop::getShopCode, shopCode)
                .one();
    }
}
