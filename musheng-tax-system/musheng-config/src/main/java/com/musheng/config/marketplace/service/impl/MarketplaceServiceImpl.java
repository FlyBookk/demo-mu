package com.musheng.config.marketplace.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.common.exception.BusinessException;
import com.musheng.common.result.ErrorCode;
import com.musheng.config.marketplace.dto.MarketplaceQueryRequest;
import com.musheng.config.marketplace.dto.MarketplaceRequest;
import com.musheng.config.marketplace.entity.Marketplace;
import com.musheng.config.marketplace.mapper.MarketplaceMapper;
import com.musheng.config.marketplace.service.MarketplaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Marketplace Service Implementation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketplaceServiceImpl implements MarketplaceService {

    private final MarketplaceMapper marketplaceMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Marketplace create(MarketplaceRequest request) {
        Marketplace entity = new Marketplace();
        copyProperties(request, entity);

        if (StpUtil.isLogin()) {
            entity.setCreateBy(StpUtil.getLoginIdAsLong());
        }

        marketplaceMapper.insert(entity);
        log.info("Created marketplace: id={}, siteCode={}", entity.getId(), entity.getSiteCode());

        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Marketplace update(Long id, MarketplaceRequest request) {
        Marketplace entity = marketplaceMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST, "Marketplace not found");
        }

        copyProperties(request, entity);

        if (StpUtil.isLogin()) {
            entity.setUpdateBy(StpUtil.getLoginIdAsLong());
        }

        marketplaceMapper.updateById(entity);
        log.info("Updated marketplace: id={}", id);

        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Marketplace entity = marketplaceMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST, "Marketplace not found");
        }

        marketplaceMapper.deleteById(id);
        log.info("Deleted marketplace: id={}", id);
    }

    @Override
    public Marketplace getById(Long id) {
        Marketplace entity = marketplaceMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST, "Marketplace not found");
        }
        return entity;
    }

    @Override
    public Page<Marketplace> list(MarketplaceQueryRequest queryRequest) {
        LambdaQueryWrapper<Marketplace> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(queryRequest.getSiteCode())) {
            wrapper.like(Marketplace::getSiteCode, queryRequest.getSiteCode());
        }
        if (StringUtils.hasText(queryRequest.getSiteName())) {
            wrapper.like(Marketplace::getSiteName, queryRequest.getSiteName());
        }
        if (queryRequest.getStatus() != null) {
            wrapper.eq(Marketplace::getStatus, queryRequest.getStatus());
        }

        wrapper.orderByDesc(Marketplace::getCreateTime);

        return marketplaceMapper.selectPage(
                new Page<>(queryRequest.getPage(), queryRequest.getSize()),
                wrapper
        );
    }

    @Override
    public java.util.List<Marketplace> getEnabled() {
        LambdaQueryWrapper<Marketplace> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Marketplace::getStatus, 1)
                .orderByAsc(Marketplace::getSiteCode);
        return marketplaceMapper.selectList(wrapper);
    }

    private void copyProperties(MarketplaceRequest request, Marketplace entity) {
        entity.setSiteCode(request.getSiteCode());
        entity.setSiteName(request.getSiteName());
        entity.setMarketplaceId(request.getMarketplaceId());
        entity.setCurrencyCode(request.getCurrencyCode());
        entity.setSellerId(request.getSellerId());
        entity.setHeaderLanguage(request.getHeaderLanguage());
        entity.setDateFormat(request.getDateFormat());
        entity.setNumberFormat(request.getNumberFormat());
        entity.setTimezone(request.getTimezone());
        entity.setStatus(request.getStatus());
    }
}
