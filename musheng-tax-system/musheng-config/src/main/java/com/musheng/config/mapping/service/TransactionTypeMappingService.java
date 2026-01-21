package com.musheng.config.mapping.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.config.mapping.dto.TransactionTypeMappingQueryRequest;
import com.musheng.config.mapping.dto.TransactionTypeMappingRequest;
import com.musheng.config.mapping.entity.TransactionTypeMapping;

/**
 * 交易类型映射服务接口
 */
public interface TransactionTypeMappingService {

    /**
     * 创建交易类型映射
     *
     * @param request 请求数据
     * @return 创建的实体
     */
    TransactionTypeMapping create(TransactionTypeMappingRequest request);

    /**
     * 更新交易类型映射
     *
     * @param id      实体ID
     * @param request 请求数据
     * @return 更新后的实体
     */
    TransactionTypeMapping update(Long id, TransactionTypeMappingRequest request);

    /**
     * 删除交易类型映射
     *
     * @param id 实体ID
     */
    void delete(Long id);

    /**
     * 根据ID获取交易类型映射
     *
     * @param id 实体ID
     * @return 实体
     */
    TransactionTypeMapping getById(Long id);

    /**
     * 分页查询交易类型映射
     *
     * @param queryRequest 查询参数
     * @return 分页结果
     */
    Page<TransactionTypeMapping> list(TransactionTypeMappingQueryRequest queryRequest);
}
