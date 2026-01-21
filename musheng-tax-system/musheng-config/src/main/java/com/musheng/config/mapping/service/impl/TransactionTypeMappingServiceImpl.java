package com.musheng.config.mapping.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.common.exception.BusinessException;
import com.musheng.common.result.ErrorCode;
import com.musheng.config.mapping.dto.TransactionTypeMappingQueryRequest;
import com.musheng.config.mapping.dto.TransactionTypeMappingRequest;
import com.musheng.config.mapping.entity.TransactionTypeMapping;
import com.musheng.config.mapping.mapper.TransactionTypeMappingMapper;
import com.musheng.config.mapping.service.TransactionTypeMappingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Transaction Type Mapping Service Implementation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionTypeMappingServiceImpl implements TransactionTypeMappingService {

    private final TransactionTypeMappingMapper transactionTypeMappingMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TransactionTypeMapping create(TransactionTypeMappingRequest request) {
        TransactionTypeMapping entity = new TransactionTypeMapping();
        copyProperties(request, entity);

        if (StpUtil.isLogin()) {
            entity.setCreateBy(StpUtil.getLoginIdAsLong());
        }

        transactionTypeMappingMapper.insert(entity);
        log.info("Created transaction type mapping: id={}, originalType={}, mappedType={}",
                entity.getId(), entity.getOriginalType(), entity.getMappedType());

        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TransactionTypeMapping update(Long id, TransactionTypeMappingRequest request) {
        TransactionTypeMapping entity = transactionTypeMappingMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST, "Transaction type mapping not found");
        }

        copyProperties(request, entity);

        if (StpUtil.isLogin()) {
            entity.setUpdateBy(StpUtil.getLoginIdAsLong());
        }

        transactionTypeMappingMapper.updateById(entity);
        log.info("Updated transaction type mapping: id={}", id);

        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        TransactionTypeMapping entity = transactionTypeMappingMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST, "Transaction type mapping not found");
        }

        transactionTypeMappingMapper.deleteById(id);
        log.info("Deleted transaction type mapping: id={}", id);
    }

    @Override
    public TransactionTypeMapping getById(Long id) {
        TransactionTypeMapping entity = transactionTypeMappingMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST, "Transaction type mapping not found");
        }
        return entity;
    }

    @Override
    public Page<TransactionTypeMapping> list(TransactionTypeMappingQueryRequest queryRequest) {
        LambdaQueryWrapper<TransactionTypeMapping> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(queryRequest.getSiteCode())) {
            wrapper.eq(TransactionTypeMapping::getSiteCode, queryRequest.getSiteCode());
        }
        if (StringUtils.hasText(queryRequest.getOriginalType())) {
            wrapper.like(TransactionTypeMapping::getOriginalType, queryRequest.getOriginalType());
        }
        if (StringUtils.hasText(queryRequest.getMappedType())) {
            wrapper.like(TransactionTypeMapping::getMappedType, queryRequest.getMappedType());
        }
        if (StringUtils.hasText(queryRequest.getStandardCategory())) {
            wrapper.eq(TransactionTypeMapping::getStandardCategory, queryRequest.getStandardCategory());
        }
        if (queryRequest.getStatus() != null) {
            wrapper.eq(TransactionTypeMapping::getStatus, queryRequest.getStatus());
        }

        wrapper.orderByDesc(TransactionTypeMapping::getCreateTime);

        return transactionTypeMappingMapper.selectPage(
                new Page<>(queryRequest.getPage(), queryRequest.getSize()),
                wrapper
        );
    }

    private void copyProperties(TransactionTypeMappingRequest request, TransactionTypeMapping entity) {
        entity.setSiteCode(request.getSiteCode());
        entity.setOriginalType(request.getOriginalType());
        entity.setMappedType(request.getMappedType());
        entity.setStandardCategory(request.getStandardCategory());
        entity.setCategoryDesc(request.getCategoryDesc());
        entity.setStatus(request.getStatus());
    }
}
