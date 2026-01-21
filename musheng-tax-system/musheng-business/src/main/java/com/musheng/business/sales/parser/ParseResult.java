package com.musheng.business.sales.parser;

import com.musheng.business.sales.entity.SalesData;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 解析结果
 * 
 * @author BACKEND_AGENT
 * @since 2026-01-21
 */
@Data
@Builder
public class ParseResult {
    
    /**
     * 是否成功
     */
    private Boolean success;
    
    /**
     * 总行数
     */
    private Integer totalRows;
    
    /**
     * 成功解析的数据行数
     */
    private Integer successRows;
    
    /**
     * 解析失败的行数
     */
    private Integer failRows;
    
    /**
     * 跳过的行数（重复数据）
     */
    private Integer skipRows;
    
    /**
     * 解析后的销售数据列表
     */
    private List<SalesData> dataList;
    
    /**
     * 预览模式的原始数据（Map格式，用于前端展示）
     */
    private List<Map<String, Object>> previewData;
    
    /**
     * 解析错误列表
     */
    private List<ParseError> errors;
    
    /**
     * 警告信息
     */
    private List<String> warnings;
    
    /**
     * 检测到的站点编码列表（多站点文件）
     */
    private List<String> detectedSiteCodes;
    
    /**
     * 解析错误详情
     */
    @Data
    @Builder
    public static class ParseError {
        /**
         * 行号
         */
        private Integer row;
        
        /**
         * 字段名
         */
        private String field;
        
        /**
         * 错误消息
         */
        private String message;
        
        /**
         * 原始值
         */
        private String originalValue;
    }
    
    /**
     * 创建成功的解析结果
     */
    public static ParseResult success(List<SalesData> dataList, int totalRows) {
        return ParseResult.builder()
                .success(true)
                .totalRows(totalRows)
                .successRows(dataList.size())
                .failRows(0)
                .skipRows(0)
                .dataList(dataList)
                .errors(new ArrayList<>())
                .warnings(new ArrayList<>())
                .build();
    }
    
    /**
     * 创建失败的解析结果
     */
    public static ParseResult fail(String message) {
        List<ParseError> errors = new ArrayList<>();
        errors.add(ParseError.builder()
                .row(0)
                .message(message)
                .build());
        
        return ParseResult.builder()
                .success(false)
                .totalRows(0)
                .successRows(0)
                .failRows(0)
                .skipRows(0)
                .dataList(new ArrayList<>())
                .errors(errors)
                .warnings(new ArrayList<>())
                .build();
    }
    
    /**
     * 添加错误
     */
    public void addError(int row, String field, String message, String originalValue) {
        if (this.errors == null) {
            this.errors = new ArrayList<>();
        }
        this.errors.add(ParseError.builder()
                .row(row)
                .field(field)
                .message(message)
                .originalValue(originalValue)
                .build());
    }
    
    /**
     * 添加警告
     */
    public void addWarning(String warning) {
        if (this.warnings == null) {
            this.warnings = new ArrayList<>();
        }
        this.warnings.add(warning);
    }
}
