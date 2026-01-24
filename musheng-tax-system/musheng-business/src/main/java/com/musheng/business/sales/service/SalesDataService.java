package com.musheng.business.sales.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.business.sales.dto.*;
import com.musheng.business.sales.entity.SalesData;
import com.musheng.common.enums.SalesSourceType;
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
     * @param request 查询请求
     * @return 分页结果
     */
    Page<SalesData> list(SalesQueryRequest request);

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
    Map<String, Object> getSummary(String keyword, String siteCode, String transactionCategory, String startDate, String endDate);

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
    
    // ========== 双格式导入相关方法 ==========
    
    /**
     * 上传销售数据文件
     * 解析文件表头，返回源字段列表和样例数据
     *
     * @param file       上传的文件
     * @param sourceType 数据源类型
     * @param siteCode   站点编码（ERP需要预选，ORIGINAL可自动识别）
     * @return 上传结果
     */
    SalesUploadResult uploadFile(MultipartFile file, SalesSourceType sourceType, String siteCode);
    
    /**
     * 预览导入数据
     * 根据选择的模板解析数据，返回前N行预览
     *
     * @param request 预览请求
     * @return 预览结果
     */
    SalesPreviewResult previewImport(SalesPreviewRequest request);
    
    /**
     * 执行导入
     * 根据模板配置解析并导入数据
     *
     * @param request 导入请求
     * @return 导入结果
     */
    SalesImportResult executeImport(SalesImportRequest request);
    
    /**
     * 获取导入进度
     *
     * @param batchNo 批次号
     * @return 导入进度
     */
    SalesImportProgress getImportProgress(String batchNo);
}
