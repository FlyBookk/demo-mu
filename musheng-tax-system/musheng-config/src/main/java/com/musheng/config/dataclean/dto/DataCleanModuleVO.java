package com.musheng.config.dataclean.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据清理模块VO
 *
 * @author wanhua
 * 12:40 2026年03月08日
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "数据清理模块信息")
public class DataCleanModuleVO {

    @Schema(description = "模块编码")
    private String moduleCode;

    @Schema(description = "模块名称")
    private String moduleName;

    @Schema(description = "模块描述")
    private String description;

    @Schema(description = "当前店铺数据量")
    private Long dataCount;
}
