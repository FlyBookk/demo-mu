package com.musheng.config.currency.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.common.exception.BusinessException;
import com.musheng.common.result.ErrorCode;
import com.musheng.config.currency.dto.CurrencyQueryRequest;
import com.musheng.config.currency.dto.CurrencyRequest;
import com.musheng.config.currency.entity.Currency;
import com.musheng.config.currency.mapper.CurrencyMapper;
import com.musheng.config.currency.service.CurrencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Currency Service Implementation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CurrencyServiceImpl implements CurrencyService {

    private final CurrencyMapper currencyMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Currency create(CurrencyRequest request) {
        Currency entity = new Currency();
        copyProperties(request, entity);

        if (StpUtil.isLogin()) {
            entity.setCreateBy(StpUtil.getLoginIdAsLong());
        }

        currencyMapper.insert(entity);
        log.info("Created currency: id={}, code={}", entity.getId(), entity.getCurrencyCode());

        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Currency update(Long id, CurrencyRequest request) {
        Currency entity = currencyMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST, "Currency not found");
        }

        copyProperties(request, entity);

        if (StpUtil.isLogin()) {
            entity.setUpdateBy(StpUtil.getLoginIdAsLong());
        }

        currencyMapper.updateById(entity);
        log.info("Updated currency: id={}", id);

        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Currency entity = currencyMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST, "Currency not found");
        }

        currencyMapper.deleteById(id);
        log.info("Deleted currency: id={}", id);
    }

    @Override
    public Currency getById(Long id) {
        Currency entity = currencyMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST, "Currency not found");
        }
        return entity;
    }

    @Override
    public Page<Currency> list(CurrencyQueryRequest queryRequest) {
        LambdaQueryWrapper<Currency> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(queryRequest.getCurrencyCode())) {
            wrapper.like(Currency::getCurrencyCode, queryRequest.getCurrencyCode());
        }
        if (StringUtils.hasText(queryRequest.getCurrencyName())) {
            wrapper.like(Currency::getCurrencyName, queryRequest.getCurrencyName());
        }
        if (queryRequest.getStatus() != null) {
            wrapper.eq(Currency::getStatus, queryRequest.getStatus());
        }

        wrapper.orderByDesc(Currency::getCreateTime);

        return currencyMapper.selectPage(
                new Page<>(queryRequest.getPage(), queryRequest.getSize()),
                wrapper
        );
    }

    @Override
    public java.util.List<Currency> getEnabled() {
        LambdaQueryWrapper<Currency> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Currency::getStatus, 1)
                .orderByAsc(Currency::getCurrencyCode);
        return currencyMapper.selectList(wrapper);
    }

    private void copyProperties(CurrencyRequest request, Currency entity) {
        entity.setCurrencyCode(request.getCurrencyCode());
        entity.setCurrencyName(request.getCurrencyName());
        entity.setCurrencySymbol(request.getCurrencySymbol());
        entity.setDecimalPlaces(request.getDecimalPlaces());
        entity.setStatus(request.getStatus());
    }
}
