package com.musheng.business.advertising.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.business.advertising.dto.AdvertisingDataImportBatchRequest;
import com.musheng.business.advertising.dto.AdvertisingDataImportResponse;
import com.musheng.business.advertising.dto.AdvertisingDataRequest;
import com.musheng.business.advertising.entity.AdvertisingData;

import java.time.LocalDate;

/**
 * 广告数据服务接口
 * P0需求: 广告数据增删改查接口
 */
public interface AdvertisingDataService {

    /**
     * 创建广告数据
     *
     * @param request 请求数据
     * @return 创建的实体
     */
    AdvertisingData create(AdvertisingDataRequest request);

    /**
     * 更新广告数据
     *
     * @param id      实体ID
     * @param request 请求数据
     * @return 更新后的实体
     */
    AdvertisingData update(Long id, AdvertisingDataRequest request);

    /**
     * 删除广告数据
     *
     * @param id 实体ID
     */
    void delete(Long id);

    /**
     * 根据ID获取广告数据
     *
     * @param id 实体ID
     * @return 实体
     */
    AdvertisingData getById(Long id);

    /**
     * 分页查询广告数据
     *
     * @param siteCode  站点编码(可选)
     * @param yearMonth 年月(可选)
     * @param page      页码
     * @param size      每页条数
     * @return 分页结果
     */
    Page<AdvertisingData> list(String siteCode, String yearMonth, int page, int size);

    /**
     * 批量导入广告数据
     * 实现发票编号去重（保留第一条）、时间字段校验等逻辑
     *
     * @param request 批量导入请求
     * @return 导入结果统计
     */
    AdvertisingDataImportResponse importData(AdvertisingDataImportBatchRequest request);

    /**
     * 按条件查询广告数据（支持账单周期查询）
     *
     * @param siteCode          站点编码(可选)
     * @param billingStartDate  账单开始日期(可选)
     * @param billingEndDate    账单结束日期(可选)
     * @param invoiceNumber     发票编号(可选)
     * @param page              页码
     * @param size              每页条数
     * @return 分页结果
     */
    Page<AdvertisingData> listByConditions(String siteCode, LocalDate billingStartDate,
                                            LocalDate billingEndDate, String invoiceNumber,
                                            int page, int size);
}
