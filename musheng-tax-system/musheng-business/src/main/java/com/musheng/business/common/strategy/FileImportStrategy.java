package com.musheng.business.common.strategy;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 文件导入策略接口
 * 
 * 用于策略模式重构文件导入逻辑，消除 CSV 和 Excel 导入的重复代码。
 * 
 * ⚠️ 重要：实现类必须保证与原有导入逻辑完全一致，
 * 以确保重构不改变任何业务输出。
 * 
 * 使用示例：
 * <pre>
 * {@code
 * @Autowired
 * private List<FileImportStrategy<ExchangeRate>> importStrategies;
 * 
 * public Map<String, Object> importData(MultipartFile file) {
 *     String fileName = file.getOriginalFilename();
 *     FileImportStrategy<ExchangeRate> strategy = importStrategies.stream()
 *             .filter(s -> s.supports(fileName))
 *             .findFirst()
 *             .orElseThrow(() -> new BusinessException("不支持的文件格式"));
 *     return strategy.importAndSave(file, context);
 * }
 * }
 * </pre>
 * 
 * @param <T> 导入的实体类型
 * @author wanhua
 * 10:30 2026年02月01日
 */
public interface FileImportStrategy<T> {
    
    /**
     * 判断是否支持该文件类型
     * 
     * 根据文件名（主要是扩展名）判断当前策略是否能处理该文件。
     * 
     * @param fileName 文件名（包含扩展名）
     * @return 支持返回 true，否则返回 false
     */
    boolean supports(String fileName);
    
    /**
     * 解析文件内容
     * 
     * 将上传的文件解析为实体列表，不执行数据库操作。
     * 
     * @param file 上传的文件
     * @param context 导入上下文，包含站点代码、店铺ID等信息
     * @return 解析后的实体列表
     * @throws IOException 文件读取异常
     */
    List<T> parse(MultipartFile file, ImportContext context) throws IOException;
    
    /**
     * 执行导入并保存
     * 
     * 完整的导入流程：解析文件 -> 数据校验 -> 去重检查 -> 保存到数据库。
     * 
     * @param file 上传的文件
     * @param context 导入上下文，包含站点代码、店铺ID等信息
     * @return 导入结果，包含成功数量、失败数量、跳过数量等信息
     */
    Map<String, Object> importAndSave(MultipartFile file, ImportContext context);
}
