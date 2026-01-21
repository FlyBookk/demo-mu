package com.musheng.config.mapping.service;

import com.musheng.config.mapping.dto.AutoMatchRequest;
import com.musheng.config.mapping.dto.AutoMatchResponse;
import com.musheng.config.mapping.dto.TargetFieldsResponse;

/**
 * 目标字段元数据服务接口
 */
public interface TargetFieldMetadataService {

    /**
     * 获取目标字段定义
     *
     * @param dataType   数据类型
     * @param sourceType 数据源类型
     * @return 目标字段响应
     */
    TargetFieldsResponse getTargetFields(String dataType, String sourceType);

    /**
     * 智能匹配
     *
     * @param request 匹配请求
     * @return 匹配响应
     */
    AutoMatchResponse autoMatch(AutoMatchRequest request);
}
