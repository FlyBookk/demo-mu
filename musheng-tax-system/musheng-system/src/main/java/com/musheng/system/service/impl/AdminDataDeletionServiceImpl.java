package com.musheng.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.musheng.business.advertising.mapper.AdvertisingDataMapper;
import com.musheng.business.fbashipment.mapper.FbaShipmentMapper;
import com.musheng.business.sales.entity.SalesData;
import com.musheng.business.sales.mapper.SalesDataMapper;
import com.musheng.business.shipping.mapper.ShippingDataMapper;
import com.musheng.common.exception.BusinessException;
import com.musheng.common.result.ErrorCode;
import com.musheng.system.service.AdminDataDeletionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Admin数据物理删除服务实现类
 * 
 * @author wanhua
 * 10:30 2026年01月29日
 */
@Service
@Slf4j
public class AdminDataDeletionServiceImpl implements AdminDataDeletionService {
    
    @Autowired
    private SalesDataMapper salesDataMapper;
    
    @Autowired
    private ShippingDataMapper shippingDataMapper;
    
    @Autowired
    private FbaShipmentMapper fbaShipmentMapper;
    
    @Autowired
    private AdvertisingDataMapper advertisingDataMapper;

    /**
     * 批量物理删除销售数据
     *
     * @param ids 数据ID列表
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteSalesData(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("ID列表不能为空");
        }
        
        log.info("开始批量物理删除销售数据, ids={}", ids);
        
        try {
            int deletedCount = salesDataMapper.physicalDeleteByIds(ids);
            log.info("批量物理删除销售数据成功, 删除数量={}", deletedCount);
        } catch (Exception e) {
            log.error("批量物理删除销售数据失败, ids={}", ids, e);
            throw new RuntimeException("批量删除失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 批量物理删除配送数据
     *
     * @param ids 数据ID列表
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteShippingData(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("ID列表不能为空");
        }
        
        log.info("开始批量物理删除配送数据, ids={}", ids);
        
        try {
            int deletedCount = shippingDataMapper.physicalDeleteByIds(ids);
            log.info("批量物理删除配送数据成功, 删除数量={}", deletedCount);
        } catch (Exception e) {
            log.error("批量物理删除配送数据失败, ids={}", ids, e);
            throw new RuntimeException("批量删除失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 批量物理删除FBA货件数据
     *
     * @param ids 数据ID列表
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteFbaShipmentData(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("ID列表不能为空");
        }
        
        log.info("开始批量物理删除FBA货件数据, ids={}", ids);
        
        try {
            int deletedCount = fbaShipmentMapper.physicalDeleteByIds(ids);
            log.info("批量物理删除FBA货件数据成功, 删除数量={}", deletedCount);
        } catch (Exception e) {
            log.error("批量物理删除FBA货件数据失败, ids={}", ids, e);
            throw new RuntimeException("批量删除失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 批量物理删除广告数据
     *
     * @param ids 数据ID列表
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteAdvertisingData(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("ID列表不能为空");
        }
        
        log.info("开始批量物理删除广告数据, ids={}", ids);
        
        try {
            int deletedCount = advertisingDataMapper.physicalDeleteByIds(ids);
            log.info("批量物理删除广告数据成功, 删除数量={}", deletedCount);
        } catch (Exception e) {
            log.error("批量物理删除广告数据失败, ids={}", ids, e);
            throw new RuntimeException("批量删除失败: " + e.getMessage(), e);
        }
    }

}
