package com.musheng.config.mapping.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.common.annotation.OperationLog;
import com.musheng.common.result.PageResult;
import com.musheng.common.result.Result;
import com.musheng.config.mapping.dto.*;
import com.musheng.config.mapping.entity.FieldMappingTemplate;
import com.musheng.config.mapping.service.FieldMappingTemplateService;
import com.musheng.config.mapping.service.FileParseService;
import com.musheng.config.mapping.service.TargetFieldMetadataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 字段映射模板控制器
 */
@Tag(name = "字段映射模板", description = "字段映射模板管理接口")
@RestController
@RequestMapping("/v1/config/field-mapping-templates")
@RequiredArgsConstructor
public class FieldMappingTemplateController {

    private final FieldMappingTemplateService fieldMappingTemplateService;
    private final TargetFieldMetadataService targetFieldMetadataService;
    private final FileParseService fileParseService;

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

    @Operation(summary = "按类型获取模板", description = "按数据类型、数据源类型和站点获取可用模板列表")
    @GetMapping("/by-type")
    public Result<java.util.List<FieldMappingTemplateOptionDTO>> getByType(
            @Parameter(description = "数据类型(SALES/SHIPPING/ADVERTISING/RATE)", required = true) @RequestParam String dataType,
            @Parameter(description = "数据源类型(ORIGINAL/ERP)") @RequestParam(required = false) String sourceType,
            @Parameter(description = "站点编码") @RequestParam(required = false) String siteCode) {
        java.util.List<FieldMappingTemplateOptionDTO> list = fieldMappingTemplateService.getByType(dataType, sourceType, siteCode);
        return Result.success(list);
    }

    // ==================== 画布相关接口 ====================

    @Operation(summary = "获取目标字段定义", description = "根据数据类型获取目标字段元数据")
    @GetMapping("/target-fields/{dataType}")
    public Result<TargetFieldsResponse> getTargetFields(
            @Parameter(description = "数据类型", required = true) @PathVariable String dataType,
            @Parameter(description = "数据源类型（仅销售数据有效）：ORIGINAL/ERP") @RequestParam(required = false) String sourceType) {
        TargetFieldsResponse response = targetFieldMetadataService.getTargetFields(dataType, sourceType);
        return Result.success(response);
    }

    @Operation(summary = "预览文件", description = "预览文件前N行内容")
    @PostMapping("/preview-file")
    public Result<FilePreviewResponse> previewFile(
            @Parameter(description = "文件", required = true) @RequestParam("file") MultipartFile file,
            @Parameter(description = "预览行数") @RequestParam(defaultValue = "10") Integer previewRows) {
        FilePreviewResponse response = fileParseService.previewFile(file, previewRows);
        return Result.success(response);
    }

    @Operation(summary = "解析文件获取源字段", description = "解析文件指定行作为表头获取源字段")
    @PostMapping("/parse-fields")
    public Result<ParseFieldsResponse> parseFields(
            @Parameter(description = "文件", required = true) @RequestParam("file") MultipartFile file,
            @Parameter(description = "表头行号(从1开始)") @RequestParam(defaultValue = "1") Integer headerRow,
            @Parameter(description = "Sheet名称(Excel可选)") @RequestParam(required = false) String sheetName) {
        ParseFieldsResponse response = fileParseService.parseFields(file, headerRow, sheetName);
        return Result.success(response);
    }

    @Operation(summary = "智能匹配建议", description = "根据源字段智能匹配目标字段")
    @PostMapping("/auto-match")
    public Result<AutoMatchResponse> autoMatch(@Valid @RequestBody AutoMatchRequest request) {
        AutoMatchResponse response = targetFieldMetadataService.autoMatch(request);
        return Result.success(response);
    }
}
