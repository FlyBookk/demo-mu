package com.musheng.config.importrecord.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.common.result.PageResult;
import com.musheng.common.result.Result;
import com.musheng.config.importrecord.dto.ImportRecordQueryRequest;
import com.musheng.config.importrecord.entity.ImportRecord;
import com.musheng.config.importrecord.service.ImportRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 导入记录控制器
 */
@Tag(name = "导入记录", description = "导入记录管理接口")
@RestController
@RequestMapping("/v1/config/import-records")
@RequiredArgsConstructor
public class ImportRecordController {

    private final ImportRecordService importRecordService;

    @Operation(summary = "导入记录详情", description = "根据ID获取导入记录")
    @GetMapping("/{id}")
    public Result<ImportRecord> getById(@Parameter(description = "导入记录ID") @PathVariable Long id) {
        ImportRecord data = importRecordService.getById(id);
        return Result.success(data);
    }

    @Operation(summary = "删除导入记录", description = "删除导入记录")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "导入记录ID") @PathVariable Long id) {
        importRecordService.delete(id);
        return Result.success();
    }

    @Operation(summary = "导入记录列表", description = "分页查询导入记录")
    @GetMapping
    public Result<PageResult<ImportRecord>> list(
            @Parameter(description = "批次号") @RequestParam(required = false) String batchNo,
            @Parameter(description = "数据类型") @RequestParam(required = false) String dataType,
            @Parameter(description = "文件名") @RequestParam(required = false) String fileName,
            @Parameter(description = "导入状态") @RequestParam(required = false) String importStatus,
            @Parameter(description = "页码(从1开始)") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size) {

        ImportRecordQueryRequest queryRequest = new ImportRecordQueryRequest();
        queryRequest.setBatchNo(batchNo);
        queryRequest.setDataType(dataType);
        queryRequest.setFileName(fileName);
        queryRequest.setImportStatus(importStatus);
        queryRequest.setPage(page);
        queryRequest.setSize(size);

        Page<ImportRecord> pageResult = importRecordService.list(queryRequest);
        PageResult<ImportRecord> result = PageResult.of(
                pageResult.getRecords(),
                pageResult.getTotal(),
                (int) pageResult.getCurrent(),
                (int) pageResult.getSize()
        );
        return Result.success(result);
    }
}
