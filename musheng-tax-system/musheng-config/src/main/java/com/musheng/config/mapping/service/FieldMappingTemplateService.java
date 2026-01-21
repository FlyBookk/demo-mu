package com.musheng.config.mapping.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.config.mapping.dto.FieldMappingTemplateQueryRequest;
import com.musheng.config.mapping.dto.FieldMappingTemplateRequest;
import com.musheng.config.mapping.entity.FieldMappingTemplate;

/**
 * 字段映射模板服务接口
 */
public interface FieldMappingTemplateService {

    /**
     * 创建字段映射模板
     *
     * @param request 请求数据
     * @return 创建的实体
     */
    FieldMappingTemplate create(FieldMappingTemplateRequest request);

    /**
     * 更新字段映射模板
     *
     * @param id      实体ID
     * @param request 请求数据
     * @return 更新后的实体
     */
    FieldMappingTemplate update(Long id, FieldMappingTemplateRequest request);

    /**
     * 删除字段映射模板
     *
     * @param id 实体ID
     */
    void delete(Long id);

    /**
     * 根据ID获取字段映射模板
     *
     * @param id 实体ID
     * @return 实体
     */
    FieldMappingTemplate getById(Long id);

    /**
     * 分页查询字段映射模板
     *
     * @param queryRequest 查询参数
     * @return 分页结果
     */
    Page<FieldMappingTemplate> list(FieldMappingTemplateQueryRequest queryRequest);

    /**
     * 复制字段映射模板
     *
     * @param id      源模板ID
     * @param newName 新模板名称
     * @return 复制后的实体
     */
    FieldMappingTemplate copy(Long id, String newName);

    /**
     * 按数据类型获取启用的模板
     *
     * @param dataType 数据类型(可选)
     * @return 启用的模板列表
     */
    java.util.List<FieldMappingTemplate> getEnabled(String dataType);
}
