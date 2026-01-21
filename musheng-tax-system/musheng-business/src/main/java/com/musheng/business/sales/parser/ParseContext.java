package com.musheng.business.sales.parser;

import com.musheng.common.enums.SalesSourceType;
import lombok.Builder;
import lombok.Data;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * 解析上下文
 * 包含解析所需的所有配置信息
 * 
 * @author BACKEND_AGENT
 * @since 2026-01-21
 */
@Data
@Builder
public class ParseContext {
    
    /**
     * 数据源类型
     */
    private SalesSourceType sourceType;
    
    /**
     * 站点编码
     */
    private String siteCode;
    
    /**
     * 模板ID
     */
    private Long templateId;
    
    /**
     * 文件路径
     */
    private Path filePath;
    
    /**
     * 文件编码
     */
    private String encoding;
    
    /**
     * 表头行号（从1开始）
     */
    private Integer headerRow;
    
    /**
     * 源字段列表
     */
    private List<String> sourceFields;
    
    /**
     * 字段映射配置
     * key: 目标字段名
     * value: 源字段名
     */
    private Map<String, String> fieldMapping;
    
    /**
     * 交易类型映射配置（用于确定transactionCategory）
     * key: 原始交易类型
     * value: 标准分类(income/refund/fee/adjustment/other)
     */
    private Map<String, String> transactionTypeMapping;
    
    /**
     * ERP交易类型到金额字段的映射（仅ERP数据使用）
     * key: ERP交易类型（如Principal、Commission）
     * value: 目标金额字段名（如productSales、sellingFees）
     */
    private Map<String, String> erpAggregateMapping;
    
    /**
     * 数据所属季度
     */
    private String quarter;
    
    /**
     * 是否跳过重复数据
     */
    private Boolean skipDuplicate;
    
    /**
     * 是否覆盖重复数据
     */
    private Boolean overwriteDuplicate;
    
    /**
     * 是否预览模式（只解析前N行）
     */
    private Boolean previewMode;
    
    /**
     * 预览行数（预览模式下有效）
     */
    private Integer previewRows;
    
    /**
     * 导入批次ID
     */
    private Long importBatchId;
}
