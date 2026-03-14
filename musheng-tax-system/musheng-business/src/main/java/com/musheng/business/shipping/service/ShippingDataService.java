package com.musheng.business.shipping.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.business.shipping.entity.ShippingData;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 配送数据服务接口
 */
public interface ShippingDataService {

    /**
     * 分页查询配送数据
     *
     * @param siteCode       站点编码(可选)
     * @param trackingNumber 物流单号(可选)
     * @param orderId        订单号(可选)
     * @param startDate      开始日期(发货日期，可选)
     * @param endDate        结束日期(发货日期，可选)
     * @param page           页码
     * @param size           每页条数
     * @return 分页结果
     */
    Page<ShippingData> list(String siteCode, String trackingNumber, String orderId, String startDate, String endDate, Integer isOwnSite, int page, int size);

    /**
     * 根据ID获取配送数据
     *
     * @param id 实体ID
     * @return 实体
     */
    ShippingData getById(Long id);

    /**
     * 从文件导入配送数据（自动识别销售渠道并分配站点）
     *
     * @param file 导入文件
     * @return 导入结果(包含成功/失败数量)
     */
    Map<String, Object> importData(MultipartFile file);

    /**
     * 批量导入配送数据（支持多选文件）
     *
     * @param files 导入文件列表
     * @return 批量导入结果
     */
    Map<String, Object> batchImportData(List<MultipartFile> files);

    /**
     * 删除配送数据
     *
     * @param id 实体ID
     */
    void delete(Long id);

    /**
     * 批量删除配送数据
     *
     * @param ids 实体ID列表
     */
    void batchDelete(List<Long> ids);

    /**
     * 获取配送数据统计汇总
     *
     * @param siteCode  站点编码(可选)
     * @param startDate 开始日期(可选)
     * @param endDate   结束日期(可选)
     * @return 汇总数据
     */
    Map<String, Object> getSummary(String siteCode, String startDate, String endDate);

    /**
     * 导出配送数据到Excel
     *
     * @param siteCode  站点编码(可选)
     * @param startDate 开始日期(可选)
     * @param endDate   结束日期(可选)
     * @param response  HTTP响应
     */
    void exportData(String siteCode, String startDate, String endDate, HttpServletResponse response);
}
