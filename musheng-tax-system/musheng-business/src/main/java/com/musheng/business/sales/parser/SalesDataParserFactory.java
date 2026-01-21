package com.musheng.business.sales.parser;

import com.musheng.common.enums.SalesSourceType;
import com.musheng.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 销售数据解析器工厂
 * 根据数据源类型返回对应的解析器实例
 * 
 * @author BACKEND_AGENT
 * @since 2026-01-21
 */
@Slf4j
@Component
public class SalesDataParserFactory {
    
    private final Map<SalesSourceType, SalesDataParser> parserMap = new HashMap<>();
    
    /**
     * 构造函数，自动注入所有解析器实现
     */
    public SalesDataParserFactory(List<SalesDataParser> parsers) {
        for (SalesDataParser parser : parsers) {
            parserMap.put(parser.getSourceType(), parser);
            log.info("注册销售数据解析器: {} -> {}", parser.getSourceType(), parser.getClass().getSimpleName());
        }
    }
    
    /**
     * 根据数据源类型获取解析器
     * 
     * @param sourceType 数据源类型
     * @return 解析器实例
     * @throws BusinessException 如果找不到对应的解析器
     */
    public SalesDataParser getParser(SalesSourceType sourceType) {
        SalesDataParser parser = parserMap.get(sourceType);
        if (parser == null) {
            throw new BusinessException("不支持的数据源类型: " + sourceType);
        }
        return parser;
    }
    
    /**
     * 检查是否支持指定的数据源类型
     * 
     * @param sourceType 数据源类型
     * @return true-支持，false-不支持
     */
    public boolean isSupported(SalesSourceType sourceType) {
        return parserMap.containsKey(sourceType);
    }
    
    /**
     * 获取所有支持的数据源类型
     * 
     * @return 数据源类型列表
     */
    public List<SalesSourceType> getSupportedTypes() {
        return List.copyOf(parserMap.keySet());
    }
}
