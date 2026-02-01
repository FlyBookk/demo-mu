package com.musheng.business.sales.service;

import com.musheng.business.sales.dto.*;
import com.musheng.common.enums.SalesSourceType;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 销售数据导入服务接口
 * 
 * 职责：
 * 1. 处理销售数据文件上传
 * 2. 解析和预览导入数据
 * 3. 执行数据导入
 * 4. 跟踪导入进度
 * 
 * @author wanhua
 * 10:30 2026年02月01日
 */
public interface SalesDataImportService {

    /**
     * 从文件导入销售数据（传统单步导入）
     *
     * @param siteCode 站点编码
     * @param file     导入文件
     * @return 导入结果(包含成功/失败数量)
     */
    Map<String, Object> importData(String siteCode, MultipartFile file);
    
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
