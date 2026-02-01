package com.musheng.business.common.strategy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 导入结果
 * 
 * 封装文件导入操作的结果信息，包括成功数量、失败数量、跳过数量等。
 * 
 * 使用示例：
 * <pre>
 * {@code
 * ImportResult result = ImportResult.builder()
 *         .totalCount(100)
 *         .successCount(95)
 *         .failCount(3)
 *         .skipCount(2)
 *         .errors(Arrays.asList("第5行: 日期格式错误", "第10行: 金额为空"))
 *         .build();
 * }
 * </pre>
 * 
 * @author wanhua
 * 10:30 2026年02月01日
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportResult {
    
    /**
     * 总记录数
     */
    private int totalCount;
    
    /**
     * 成功数量
     */
    private int successCount;
    
    /**
     * 失败数量
     */
    private int failCount;
    
    /**
     * 跳过数量（如重复记录）
     */
    private int skipCount;
    
    /**
     * 错误信息列表
     */
    @Builder.Default
    private List<String> errors = new ArrayList<>();
    
    /**
     * 添加错误信息
     * 
     * @param error 错误信息
     */
    public void addError(String error) {
        if (this.errors == null) {
            this.errors = new ArrayList<>();
        }
        this.errors.add(error);
    }
    
    /**
     * 判断是否有错误
     * 
     * @return 有错误返回 true，否则返回 false
     */
    public boolean hasErrors() {
        return this.errors != null && !this.errors.isEmpty();
    }
    
    /**
     * 转换为 Map 格式（兼容原有返回格式）
     * 
     * @return 包含导入结果的 Map
     */
    public java.util.Map<String, Object> toMap() {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("totalCount", this.totalCount);
        result.put("successCount", this.successCount);
        result.put("failCount", this.failCount);
        result.put("skipCount", this.skipCount);
        if (this.errors != null && !this.errors.isEmpty()) {
            result.put("errors", this.errors);
        }
        return result;
    }
}
