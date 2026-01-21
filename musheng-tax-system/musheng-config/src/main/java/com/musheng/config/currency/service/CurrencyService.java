package com.musheng.config.currency.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.config.currency.dto.CurrencyQueryRequest;
import com.musheng.config.currency.dto.CurrencyRequest;
import com.musheng.config.currency.entity.Currency;

/**
 * 货币服务接口
 */
public interface CurrencyService {

    /**
     * 创建货币
     *
     * @param request 请求数据
     * @return 创建的实体
     */
    Currency create(CurrencyRequest request);

    /**
     * 更新货币
     *
     * @param id      实体ID
     * @param request 请求数据
     * @return 更新后的实体
     */
    Currency update(Long id, CurrencyRequest request);

    /**
     * 删除货币
     *
     * @param id 实体ID
     */
    void delete(Long id);

    /**
     * 根据ID获取货币
     *
     * @param id 实体ID
     * @return 实体
     */
    Currency getById(Long id);

    /**
     * 分页查询货币
     *
     * @param queryRequest 查询参数
     * @return 分页结果
     */
    Page<Currency> list(CurrencyQueryRequest queryRequest);

    /**
     * 获取所有启用的货币
     *
     * @return 启用的货币列表
     */
    java.util.List<Currency> getEnabled();
}
