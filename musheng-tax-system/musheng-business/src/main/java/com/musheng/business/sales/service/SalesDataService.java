package com.musheng.business.sales.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.business.sales.entity.SalesData;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 销售数据服务接口
 */
public interface SalesDataService {

    /**
     * 分页查询销售数据
     *
     * @param siteCode            站点编码(可选)
     * @param transactionCategory 交易分类(可选)
     * @param transactionType     交易类型(可选)
     * @param orderId             订单号(可选)
     * @param page                页码
     * @param size                每页条数
     * @return 分页结果
     */
    Page<SalesData> list(String siteCode, String transactionCategory, String transactionType, String orderId, int page, int size);

    /**
     * 根据ID获取销售数据
     *
     * @param id 实体ID
     * @return 实体
     */
    SalesData getById(Long id);

    /**
     * 从文件导入销售数据
     *
     * @param siteCode 站点编码
     * @param file     导入文件
     * @return 导入结果(包含成功/失败数量)
     */
    Map<String, Object> importData(String siteCode, MultipartFile file);

    /**
     * 删除销售数据
     *
     * @param id 实体ID
     */
    void delete(Long id);

    /**
     * 批量删除销售数据
     *
     * @param ids 实体ID列表
     */
    void batchDelete(List<Long> ids);

    /**
     * 获取销售数据统计汇总
     *
     * @param siteCode            站点编码(可选)
     * @param transactionCategory 交易分类(可选)
     * @param startDate           开始日期(可选)
     * @param endDate             结束日期(可选)
     * @return 汇总数据
     */
    Map<String, Object> getSummary(String siteCode, String transactionCategory, String startDate, String endDate);

    /**
     * 按交易类型分组统计
     *
     * @param siteCode  站点编码(可选)
     * @param startDate 开始日期(可选)
     * @param endDate   结束日期(可选)
     * @return 统计列表
     */
    List<Map<String, Object>> getStatByType(String siteCode, String startDate, String endDate);

    /**
     * 导出销售数据到Excel
     *
     * @param siteCode            站点编码(可选)
     * @param transactionCategory 交易分类(可选)
     * @param startDate           开始日期(可选)
     * @param endDate             结束日期(可选)
     * @param response            HTTP响应
     */
    void exportData(String siteCode, String transactionCategory, String startDate, String endDate, HttpServletResponse response);
}
