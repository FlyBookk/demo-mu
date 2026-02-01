package com.musheng.business.common.strategy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 导入上下文
 * 
 * 封装文件导入过程中需要的上下文信息，包括站点代码、店铺ID、字段映射等。
 * 
 * @author wanhua
 * 10:30 2026年02月01日
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportContext {
    
    /**
     * 站点代码（如 US、UK、DE 等）
     */
    private String siteCode;
    
    /**
     * 店铺ID
     */
    private Long shopId;
    
    /**
     * 字段映射（Excel/CSV 列名 -> 实体字段名）
     */
    private Map<String, String> fieldMapping;
    
    /**
     * 交易类型映射（原始值 -> 标准值）
     */
    private Map<String, String> transactionTypeMapping;
}
