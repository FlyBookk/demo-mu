package com.musheng.business.fbashipment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.business.fbashipment.entity.FbaShipment;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * FBA货件服务接口
 * 提供货件的导入、查询、删除等业务功能
 */
public interface FbaShipmentService {

    /**
     * 导入Excel文件
     *
     * @param file Excel文件
     * @return 导入结果
     */
    Map<String, Object> importData(MultipartFile file);

    /**
     * 批量导入多个Excel文件（支持幂等性）
     *
     * @param files Excel文件列表
     * @return 批量导入结果
     */
    Map<String, Object> batchImportData(List<MultipartFile> files);

    /**
     * 分页查询货件列表
     *
     * @param shipmentId 货件单号（模糊查询）
     * @param status 货件状态
     * @param shopName 店铺名称
     * @param country 国家
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param page 页码
     * @param size 每页条数
     * @return 分页结果
     */
    Page<FbaShipment> list(String shipmentId, String status, String shopName, String country,
                          String startDate, String endDate, int page, int size);

    /**
     * 根据ID查询货件详情（包含SKU明细）
     *
     * @param id 货件ID
     * @return 货件详情
     */
    FbaShipment getById(Long id);

    /**
     * 删除货件（级联删除明细）
     *
     * @param id 货件ID
     */
    void delete(Long id);

    /**
     * 批量删除货件
     *
     * @param ids 货件ID列表
     */
    void batchDelete(List<Long> ids);

    /**
     * 获取统计汇总
     *
     * @param status 货件状态
     * @param shopName 店铺名称
     * @param country 国家
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 统计数据
     */
    Map<String, Object> getSummary(String status, String shopName, String country, String startDate, String endDate);

    /**
     * 导出数据（CSV格式，与导入文档格式一致：货件+MSKU明细分组）
     *
     * @param status 货件状态
     * @param shopName 店铺名称
     * @param country 国家
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param response HTTP响应
     */
    void exportData(String status, String shopName, String country, String startDate, String endDate,
                   jakarta.servlet.http.HttpServletResponse response);

    /**
     * 获取所有国家列表（去重）
     *
     * @return 国家列表
     */
    List<String> getCountryList();
}
