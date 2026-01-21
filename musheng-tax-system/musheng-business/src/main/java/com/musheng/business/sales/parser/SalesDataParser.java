package com.musheng.business.sales.parser;

import com.musheng.common.enums.SalesSourceType;

/**
 * 销售数据解析器接口
 * 不同数据源类型实现不同的解析逻辑
 * 
 * @author BACKEND_AGENT
 * @since 2026-01-21
 */
public interface SalesDataParser {
    
    /**
     * 获取支持的数据源类型
     * 
     * @return 数据源类型
     */
    SalesSourceType getSourceType();
    
    /**
     * 解析销售数据（从上下文中的文件路径读取）
     * 
     * @param context 解析上下文
     * @return 解析结果
     */
    ParseResult parse(ParseContext context);
    
    /**
     * 解析销售数据（从字符串内容解析）
     * 
     * @param content  文件内容
     * @param context  解析上下文
     * @param maxRows  最大解析行数（用于预览）
     * @return 解析结果
     */
    ParseResult parse(String content, ParseContext context, int maxRows);
    
    /**
     * 预览解析（只解析前N行）
     * 
     * @param context 解析上下文
     * @return 解析结果（包含预览数据）
     */
    default ParseResult preview(ParseContext context) {
        context.setPreviewMode(true);
        if (context.getPreviewRows() == null) {
            context.setPreviewRows(10);
        }
        return parse(context);
    }
    
    /**
     * 检测文件中的站点编码
     * 
     * @param context 解析上下文（需要包含文件路径）
     * @return 站点编码列表
     */
    java.util.List<String> detectSiteCodes(ParseContext context);
    
    /**
     * 验证文件格式是否符合预期
     * 
     * @param context 解析上下文
     * @return true-格式正确，false-格式错误
     */
    boolean validateFormat(ParseContext context);
}
