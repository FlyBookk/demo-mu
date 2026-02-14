package com.musheng.system.service;

import java.util.List;

/**
 * Admin数据物理删除服务接口
 * 
 * @author wanhua
 * 10:30 2026年01月29日
 */
public interface AdminDataDeletionService {
    
    /**
     * 批量物理删除销售数据
     * 
     * @param ids 数据ID列表
     * @author wanhua
     * 10:30 2026年01月29日
     */
    void batchDeleteSalesData(List<Long> ids);
    
    /**
     * 批量物理删除配送数据
     * 
     * @param ids 数据ID列表
     * @author wanhua
     * 10:30 2026年01月29日
     */
    void batchDeleteShippingData(List<Long> ids);
    
    /**
     * 批量物理删除FBA货件数据
     * 
     * @param ids 数据ID列表
     * @author wanhua
     * 10:30 2026年01月29日
     */
    void batchDeleteFbaShipmentData(List<Long> ids);
    
    /**
     * 批量物理删除广告数据
     * 
     * @param ids 数据ID列表
     * @author wanhua
     * 10:30 2026年01月29日
     */
    void batchDeleteAdvertisingData(List<Long> ids);
}
