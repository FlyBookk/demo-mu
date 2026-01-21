package com.musheng.business.common.service.csv;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * CSV解析结果
 *
 * @param <T> 数据实体类型
 */
@Data
@Builder
@Schema(description = "CSV解析结果")
public class CsvParseResult<T> {

    @Schema(description = "是否解析成功")
    private Boolean success;

    @Schema(description = "总记录数")
    private Integer totalCount;

    @Schema(description = "成功条数")
    private Integer successCount;

    @Schema(description = "失败条数")
    private Integer failCount;

    @Schema(description = "检测到的站点编码")
    private String siteCode;

    @Schema(description = "解析后的数据列表")
    private List<T> dataList;

    @Schema(description = "错误列表")
    private List<CsvParseError> errors;

    @Schema(description = "错误信息(严重错误时)")
    private String errorMessage;

    /**
     * 创建成功结果
     */
    public static <T> CsvParseResult<T> success(List<T> dataList, String siteCode) {
        return CsvParseResult.<T>builder()
                .success(true)
                .totalCount(dataList.size())
                .successCount(dataList.size())
                .failCount(0)
                .siteCode(siteCode)
                .dataList(dataList)
                .build();
    }

    /**
     * 创建部分成功结果
     */
    public static <T> CsvParseResult<T> partial(List<T> dataList, List<CsvParseError> errors, String siteCode) {
        return CsvParseResult.<T>builder()
                .success(true)
                .totalCount(dataList.size() + errors.size())
                .successCount(dataList.size())
                .failCount(errors.size())
                .siteCode(siteCode)
                .dataList(dataList)
                .errors(errors)
                .build();
    }

    /**
     * 创建失败结果
     */
    public static <T> CsvParseResult<T> failed(String errorMessage) {
        return CsvParseResult.<T>builder()
                .success(false)
                .totalCount(0)
                .successCount(0)
                .failCount(0)
                .errorMessage(errorMessage)
                .build();
    }
}
