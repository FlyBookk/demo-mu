package com.musheng.business.advertising.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.business.advertising.dto.AdvertisingDataImportBatchRequest;
import com.musheng.business.advertising.dto.AdvertisingDataImportResponse;
import com.musheng.business.advertising.entity.AdvertisingBill;
import com.musheng.business.advertising.entity.AdvertisingBillItem;

import java.time.LocalDate;
import java.util.List;

/**
 * 广告发票服务接口（主表+明细，无去重）
 */
public interface AdvertisingBillService {

    /**
     * 批量导入（按行入库，无去重；同发票多行→1个bill+N个item）
     */
    AdvertisingDataImportResponse importData(AdvertisingDataImportBatchRequest request);

    /**
     * 分页查询发票主表
     */
    Page<AdvertisingBill> list(String siteCode, LocalDate billingStartDate, LocalDate billingEndDate,
                               String invoiceNumber, int page, int size);

    /**
     * 根据ID获取发票详情（含明细）
     */
    AdvertisingBill getById(Long id);

    /**
     * 删除发票（级联删除明细）
     */
    void delete(Long id);

    /**
     * 批量删除
     */
    void batchDelete(List<Long> ids);

    /**
     * 分页查询明细（全局视图，支持按发票编号、活动ID筛选）
     */
    Page<AdvertisingBillItem> listItems(String invoiceNumber, String campaignId, String campaignName,
                                       int page, int size);

    /**
     * 下载导入模板（Excel，含表头与示例行）
     */
    void downloadTemplate(jakarta.servlet.http.HttpServletResponse response);

    /**
     * 汇总统计（按当前筛选条件）
     */
    com.musheng.business.advertising.dto.AdvertisingSummaryDTO getSummary(
            String siteCode, java.time.LocalDate billingStartDate, java.time.LocalDate billingEndDate,
            String invoiceNumber);
}
