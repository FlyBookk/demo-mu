package com.musheng.business.sales.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.business.sales.dto.SalesQueryRequest;
import com.musheng.business.sales.entity.SalesData;
import com.musheng.business.sales.repository.SalesDataRepository;
import com.musheng.business.sales.service.SalesDataQueryService;
import com.musheng.common.exception.BusinessException;
import com.musheng.common.result.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 销售数据查询服务实现类
 * 
 * 职责：
 * 1. 分页查询销售数据
 * 2. 根据ID获取销售数据
 * 3. 删除销售数据
 * 4. 批量删除销售数据
 * 
 * ⚠️ 核心原则：
 * 1. 禁止修改业务流程
 * 2. 禁止改变输出结果
 * 3. 只是将 Mapper 调用替换为 Repository 调用
 * 
 * @author wanhua
 * 10:30 2026年02月01日
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SalesDataQueryServiceImpl implements SalesDataQueryService {

    private final SalesDataRepository salesDataRepository;

    @Override
    public Page<SalesData> list(SalesQueryRequest request) {
        int page = request.getPage() != null ? request.getPage() : 1;
        int size = request.getSize() != null ? request.getSize() : 20;
        return salesDataRepository.findByQuery(request, page, size);
    }

    @Override
    public SalesData getById(Long id) {
        SalesData entity = salesDataRepository.findById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST, "Sales data not found");
        }
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SalesData entity = salesDataRepository.findById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST, "Sales data not found");
        }

        salesDataRepository.deleteById(id);
        log.info("Deleted sales data: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        salesDataRepository.deleteByIds(ids);
        log.info("Batch deleted sales data: ids={}", ids);
    }
}
