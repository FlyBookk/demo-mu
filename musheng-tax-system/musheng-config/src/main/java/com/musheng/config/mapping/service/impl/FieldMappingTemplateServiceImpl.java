package com.musheng.config.mapping.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.common.exception.BusinessException;
import com.musheng.common.result.ErrorCode;
import com.musheng.config.mapping.dto.FieldMappingTemplateQueryRequest;
import com.musheng.config.mapping.dto.FieldMappingTemplateRequest;
import com.musheng.config.mapping.entity.FieldMappingTemplate;
import com.musheng.config.mapping.mapper.FieldMappingTemplateMapper;
import com.musheng.config.mapping.service.FieldMappingTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Field Mapping Template Service Implementation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FieldMappingTemplateServiceImpl implements FieldMappingTemplateService {

    private final FieldMappingTemplateMapper fieldMappingTemplateMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FieldMappingTemplate create(FieldMappingTemplateRequest request) {
        FieldMappingTemplate entity = new FieldMappingTemplate();
        copyProperties(request, entity);

        if (StpUtil.isLogin()) {
            entity.setCreateBy(StpUtil.getLoginIdAsLong());
        }

        fieldMappingTemplateMapper.insert(entity);
        log.info("Created field mapping template: id={}, name={}", entity.getId(), entity.getTemplateName());

        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FieldMappingTemplate update(Long id, FieldMappingTemplateRequest request) {
        FieldMappingTemplate entity = fieldMappingTemplateMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST, "Field mapping template not found");
        }

        copyProperties(request, entity);

        if (StpUtil.isLogin()) {
            entity.setUpdateBy(StpUtil.getLoginIdAsLong());
        }

        fieldMappingTemplateMapper.updateById(entity);
        log.info("Updated field mapping template: id={}", id);

        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        FieldMappingTemplate entity = fieldMappingTemplateMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST, "Field mapping template not found");
        }

        fieldMappingTemplateMapper.deleteById(id);
        log.info("Deleted field mapping template: id={}", id);
    }

    @Override
    public FieldMappingTemplate getById(Long id) {
        FieldMappingTemplate entity = fieldMappingTemplateMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST, "Field mapping template not found");
        }
        return entity;
    }

    @Override
    public Page<FieldMappingTemplate> list(FieldMappingTemplateQueryRequest queryRequest) {
        LambdaQueryWrapper<FieldMappingTemplate> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(queryRequest.getTemplateName())) {
            wrapper.like(FieldMappingTemplate::getTemplateName, queryRequest.getTemplateName());
        }
        if (StringUtils.hasText(queryRequest.getSiteCode())) {
            wrapper.eq(FieldMappingTemplate::getSiteCode, queryRequest.getSiteCode());
        }
        if (StringUtils.hasText(queryRequest.getDataType())) {
            wrapper.eq(FieldMappingTemplate::getDataType, queryRequest.getDataType());
        }
        if (queryRequest.getIsDefault() != null) {
            wrapper.eq(FieldMappingTemplate::getIsDefault, queryRequest.getIsDefault());
        }

        wrapper.orderByDesc(FieldMappingTemplate::getCreateTime);

        return fieldMappingTemplateMapper.selectPage(
                new Page<>(queryRequest.getPage(), queryRequest.getSize()),
                wrapper
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FieldMappingTemplate copy(Long id, String newName) {
        FieldMappingTemplate source = fieldMappingTemplateMapper.selectById(id);
        if (source == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST, "Field mapping template not found");
        }

        FieldMappingTemplate copy = new FieldMappingTemplate();
        copy.setTemplateName(newName);
        copy.setSiteCode(source.getSiteCode());
        copy.setDataType(source.getDataType());
        copy.setMappingConfig(source.getMappingConfig());
        copy.setIsDefault(false); // Copied template is not default

        if (StpUtil.isLogin()) {
            copy.setCreateBy(StpUtil.getLoginIdAsLong());
        }

        fieldMappingTemplateMapper.insert(copy);
        log.info("Copied field mapping template: sourceId={}, newId={}, newName={}", id, copy.getId(), newName);

        return copy;
    }

    @Override
    public java.util.List<FieldMappingTemplate> getEnabled(String dataType) {
        LambdaQueryWrapper<FieldMappingTemplate> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(dataType)) {
            wrapper.eq(FieldMappingTemplate::getDataType, dataType);
        }
        wrapper.orderByDesc(FieldMappingTemplate::getIsDefault)
                .orderByAsc(FieldMappingTemplate::getTemplateName);
        return fieldMappingTemplateMapper.selectList(wrapper);
    }

    private void copyProperties(FieldMappingTemplateRequest request, FieldMappingTemplate entity) {
        entity.setTemplateName(request.getTemplateName());
        entity.setSiteCode(request.getSiteCode());
        entity.setDataType(request.getDataType());
        entity.setMappingConfig(request.getMappingConfig());
        entity.setIsDefault(request.getIsDefault());
    }
}
