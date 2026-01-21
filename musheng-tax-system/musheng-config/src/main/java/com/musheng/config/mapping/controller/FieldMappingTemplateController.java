package com.musheng.config.mapping.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.common.annotation.OperationLog;
import com.musheng.common.result.PageResult;
import com.musheng.common.result.Result;
import com.musheng.config.mapping.dto.FieldMappingTemplateQueryRequest;
import com.musheng.config.mapping.dto.FieldMappingTemplateRequest;
import com.musheng.config.mapping.entity.FieldMappingTemplate;
import com.musheng.config.mapping.service.FieldMappingTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 字段映射模板控制器
 */
@Tag(name = "字段映射模板", description = "字段映射模板管理接口")
@RestController
@RequestMapping("/v1/config/field-mapping-templates")
@RequiredArgsConstructor
public class FieldMappingTemplateController {

    private final FieldMappingTemplateService fieldMappingTemplateService;

    @OperationLog(module = "字段映射模板", operation = "创建模板")
    @Operation(summary = "创建模板", description = "创建字段映射模板")
    @PostMapping
    public Result<FieldMappingTemplate> create(@Valid @RequestBody FieldMappingTemplateRequest request) {
        FieldMappingTemplate data = fieldMappingTemplateService.create(request);
        return Result.success(data);
    }

    @OperationLog(module = "字段映射模板", operation = "更新模板")
    @Operation(summary = "更新模板", description = "更新字段映射模板")
    @PutMapping("/{id}")
    public Result<FieldMappingTemplate> update(
            @Parameter(description = "模板ID") @PathVariable Long id,
            @Valid @RequestBody FieldMappingTemplateRequest request) {
        FieldMappingTemplate data = fieldMappingTemplateService.update(id, request);
        return Result.success(data);
    }

    @OperationLog(module = "字段映射模板", operation = "删除模板")
    @Operation(summary = "删除模板", description = "删除字段映射模板")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "模板ID") @PathVariable Long id) {
        fieldMappingTemplateService.delete(id);
        return Result.success();
    }

    @Operation(summary = "模板详情", description = "根据ID获取字段映射模板")
    @GetMapping("/{id}")
    public Result<FieldMappingTemplate> getById(@Parameter(description = "模板ID") @PathVariable Long id) {
        FieldMappingTemplate data = fieldMappingTemplateService.getById(id);
        return Result.success(data);
    }

    @Operation(summary = "模板列表", description = "分页查询字段映射模板")
    @GetMapping
    public Result<PageResult<FieldMappingTemplate>> list(
            @Parameter(description = "模板名称") @RequestParam(required = false) String templateName,
            @Parameter(description = "站点编码") @RequestParam(required = false) String siteCode,
            @Parameter(description = "数据类型") @RequestParam(required = false) String dataType,
            @Parameter(description = "是否默认") @RequestParam(required = false) Boolean isDefault,
            @Parameter(description = "页码(从1开始)") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size) {

        FieldMappingTemplateQueryRequest queryRequest = new FieldMappingTemplateQueryRequest();
        queryRequest.setTemplateName(templateName);
        queryRequest.setSiteCode(siteCode);
        queryRequest.setDataType(dataType);
        queryRequest.setIsDefault(isDefault);
        queryRequest.setPage(page);
        queryRequest.setSize(size);

        Page<FieldMappingTemplate> pageResult = fieldMappingTemplateService.list(queryRequest);
        PageResult<FieldMappingTemplate> result = PageResult.of(
                pageResult.getRecords(),
                pageResult.getTotal(),
                (int) pageResult.getCurrent(),
                (int) pageResult.getSize()
        );
        return Result.success(result);
    }

    @OperationLog(module = "字段映射模板", operation = "复制模板")
    @Operation(summary = "复制模板", description = "复制字段映射模板并设置新名称")
    @PostMapping("/{id}/copy")
    public Result<FieldMappingTemplate> copy(
            @Parameter(description = "源模板ID") @PathVariable Long id,
            @Parameter(description = "新模板名称") @RequestParam String newName) {
        FieldMappingTemplate data = fieldMappingTemplateService.copy(id, newName);
        return Result.success(data);
    }

    @Operation(summary = "启用的模板", description = "按数据类型获取启用的模板(下拉选择用)")
    @GetMapping("/enabled")
    public Result<java.util.List<FieldMappingTemplate>> getEnabled(
            @Parameter(description = "数据类型(SALES/SHIPPING/ADVERTISING/RATE)") @RequestParam(required = false) String dataType) {
        java.util.List<FieldMappingTemplate> list = fieldMappingTemplateService.getEnabled(dataType);
        return Result.success(list);
    }
}
