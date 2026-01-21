package com.musheng.common.result;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分页响应结果
 *
 * @param <T> 数据类型
 */
@Data
@Schema(description = "分页响应结果")
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 数据记录列表
     */
    @Schema(description = "数据记录列表")
    private List<T> records;

    /**
     * 总记录数
     */
    @Schema(description = "总记录数", example = "100")
    private long total;

    /**
     * 当前页码 (从1开始)
     */
    @Schema(description = "当前页码", example = "1")
    private int page;

    /**
     * 每页条数
     */
    @Schema(description = "每页条数", example = "10")
    private int size;

    /**
     * 总页数
     */
    @Schema(description = "总页数", example = "10")
    private int pages;

    /**
     * 创建分页结果
     */
    public static <T> PageResult<T> of(List<T> records, long total, int page, int size) {
        PageResult<T> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(total);
        result.setPage(page);
        result.setSize(size);
        result.setPages((int) Math.ceil((double) total / size));
        return result;
    }

    /**
     * 是否有下一页
     */
    public boolean hasNext() {
        return page < pages;
    }

    /**
     * 是否有上一页
     */
    public boolean hasPrevious() {
        return page > 1;
    }
}
