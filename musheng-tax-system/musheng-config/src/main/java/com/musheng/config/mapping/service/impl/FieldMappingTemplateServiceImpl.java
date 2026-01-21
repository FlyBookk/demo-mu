package com.musheng.config.mapping.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.common.exception.BusinessException;
import com.musheng.common.result.ErrorCode;
import com.musheng.config.mapping.dto.FieldMappingTemplateOptionDTO;
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

import java.util.List;

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
        copy.setSourceType(source.getSourceType());
        copy.setMappingConfig(source.getMappingConfig());
        copy.setSourceFields(source.getSourceFields());
        copy.setHeaderRow(source.getHeaderRow());
        copy.setDefaultValues(source.getDefaultValues());
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

    @Override
    public List<FieldMappingTemplateOptionDTO> getByType(String dataType, String sourceType, String siteCode) {
        LambdaQueryWrapper<FieldMappingTemplate> wrapper = new LambdaQueryWrapper<>();
        
        // 必须匹配数据类型
        if (StringUtils.hasText(dataType)) {
            wrapper.eq(FieldMappingTemplate::getDataType, dataType);
        }
        
        // 匹配数据源类型
        if (StringUtils.hasText(sourceType)) {
            wrapper.eq(FieldMappingTemplate::getSourceType, sourceType);
        }
        
        // 匹配站点：站点相同或者站点为空（通用模板）
        if (StringUtils.hasText(siteCode)) {
            wrapper.and(w -> w.eq(FieldMappingTemplate::getSiteCode, siteCode)
                    .or().isNull(FieldMappingTemplate::getSiteCode)
                    .or().eq(FieldMappingTemplate::getSiteCode, ""));
        }
        
        wrapper.orderByDesc(FieldMappingTemplate::getIsDefault)
                .orderByAsc(FieldMappingTemplate::getTemplateName);
        
        java.util.List<FieldMappingTemplate> templates = fieldMappingTemplateMapper.selectList(wrapper);
        
        // 转换为 DTO
        java.util.List<FieldMappingTemplateOptionDTO> result = new java.util.ArrayList<>();
        for (FieldMappingTemplate template : templates) {
            FieldMappingTemplateOptionDTO dto = FieldMappingTemplateOptionDTO.builder()
                    .id(template.getId())
                    .templateName(template.getTemplateName())
                    .siteCode(template.getSiteCode())
                    .dataType(template.getDataType())
                    .sourceType(template.getSourceType())
                    .isDefault(template.getIsDefault())
                    .mappingCount(countMappings(template.getMappingConfig()))
                    .build();
            result.add(dto);
        }
        
        return result;
    }

    /**
     * 统计映射配置中的字段数量
     */
    private Integer countMappings(String mappingConfig) {
        if (!StringUtils.hasText(mappingConfig)) {
            return 0;
        }
        try {
            // 简单统计 JSON 数组中的元素数量
            // mappingConfig 格式: [{"source": "xxx", "target": "yyy"}, ...]
            int count = 0;
            int depth = 0;
            for (char c : mappingConfig.toCharArray()) {
                if (c == '{') depth++;
                if (c == '}') {
                    depth--;
                    if (depth == 0) count++;
                }
            }
            return count;
        } catch (Exception e) {
            return 0;
        }
    }

    private void copyProperties(FieldMappingTemplateRequest request, FieldMappingTemplate entity) {
        entity.setTemplateName(request.getTemplateName());
        entity.setSiteCode(request.getSiteCode());
        entity.setDataType(request.getDataType());
        entity.setSourceType(request.getSourceType());
        entity.setMappingConfig(request.getMappingConfig());
        entity.setSourceFields(request.getSourceFields());
        entity.setHeaderRow(request.getHeaderRow());
        entity.setDefaultValues(request.getDefaultValues());
        entity.setIsDefault(request.getIsDefault());
    }
}
