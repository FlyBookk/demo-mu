package com.musheng.business.fbashipment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.business.fbashipment.entity.FbaShipmentDetail;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * FBA货件明细服务接口
 */
public interface FbaShipmentDetailService {

    /**
     * 分页查询FBA货件明细
     *
     * @param status           货件状态(可选)
     * @param shipmentId       货件编号(可选)
     * @param receivingAddress 收货地址(可选)
     * @param page             页码
     * @param size             每页条数
     * @return 分页结果
     */
    Page<FbaShipmentDetail> list(String status, String shipmentId, String receivingAddress, int page, int size);

    /**
     * 根据ID获取FBA货件明细
     *
     * @param id 实体ID
     * @return 实体
     */
    FbaShipmentDetail getById(Long id);

    /**
     * 从文件导入FBA货件明细（自动识别GBK编码）
     *
     * @param file 导入文件
     * @return 导入结果(包含成功/失败/重复数量)
     */
    Map<String, Object> importData(MultipartFile file);

    /**
     * 新增FBA货件明细
     *
     * @param fbaShipmentDetail 实体
     * @return 实体ID
     */
    Long add(FbaShipmentDetail fbaShipmentDetail);

    /**
     * 更新FBA货件明细
     *
     * @param fbaShipmentDetail 实体
     */
    void update(FbaShipmentDetail fbaShipmentDetail);

    /**
     * 删除FBA货件明细
     *
     * @param id 实体ID
     */
    void delete(Long id);

    /**
     * 批量删除FBA货件明细
     *
     * @param ids 实体ID列表
     */
    void batchDelete(List<Long> ids);

    /**
     * 获取FBA货件明细统计汇总
     *
     * @param status           货件状态(可选)
     * @param receivingAddress 收货地址(可选)
     * @param startDate        开始日期(可选)
     * @param endDate          结束日期(可选)
     * @return 汇总数据
     */
    Map<String, Object> getSummary(String status, String receivingAddress, String startDate, String endDate);

    /**
     * 导出FBA货件明细到Excel
     *
     * @param status           货件状态(可选)
     * @param receivingAddress 收货地址(可选)
     * @param startDate        开始日期(可选)
     * @param endDate          结束日期(可选)
     * @param response         HTTP响应
     */
    void exportData(String status, String receivingAddress, String startDate, String endDate, HttpServletResponse response);
}
