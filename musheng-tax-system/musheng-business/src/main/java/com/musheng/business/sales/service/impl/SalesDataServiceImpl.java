package com.musheng.business.sales.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.business.sales.dto.*;
import com.musheng.business.sales.entity.SalesData;
import com.musheng.business.sales.service.SalesDataExportService;
import com.musheng.business.sales.service.SalesDataImportService;
import com.musheng.business.sales.service.SalesDataQueryService;
import com.musheng.business.sales.service.SalesDataService;
import com.musheng.business.sales.service.SalesDataStatisticsService;
import com.musheng.common.enums.SalesSourceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 销售数据服务实现类 - 门面模式
 * 
 * 职责：
 * 1. 作为对外接口，保持 API 不变
 * 2. 委托给专职 Service 处理
 * 3. 管理事务边界
 * 
 * @author Kiro
 * 10:30 2026年02月01日
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SalesDataServiceImpl implements SalesDataService {
    
    // 专职 Service（门面模式委托）
    private final SalesDataQueryService queryService;
    private final SalesDataStatisticsService statisticsService;
    private final SalesDataExportService exportService;
    private final SalesDataImportService importService;

    @Override
    public Page<SalesData> list(SalesQueryRequest request) {
        return queryService.list(request);
    }

    @Override
    public SalesData getById(Long id) {
        return queryService.getById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importData(String siteCode, MultipartFile file) {
        return importService.importData(siteCode, file);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        queryService.delete(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<Long> ids) {
        queryService.batchDelete(ids);
    }

    @Override
    public Map<String, Object> getSummary(String keyword, String siteCode, String settlementId, String transactionCategory, String startDate, String endDate) {
        return statisticsService.getSummary(keyword, siteCode, settlementId, transactionCategory, startDate, endDate);
    }

    @Override
    public List<Map<String, Object>> getStatByType(String siteCode, String startDate, String endDate) {
        return statisticsService.getStatByType(siteCode, startDate, endDate);
    }

    @Override
    public void exportData(String siteCode, String transactionCategory, String startDate, String endDate,
                           jakarta.servlet.http.HttpServletResponse response) {
        exportService.exportData(siteCode, transactionCategory, startDate, endDate, response);
    }
    
    // ========== 双格式导入相关方法实现（委托给 ImportService）==========
    
    @Override
    public SalesUploadResult uploadFile(MultipartFile file, SalesSourceType sourceType, String siteCode) {
        return importService.uploadFile(file, sourceType, siteCode);
    }
    
    @Override
    public SalesPreviewResult previewImport(SalesPreviewRequest request) {
        return importService.previewImport(request);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SalesImportResult executeImport(SalesImportRequest request) {
        return importService.executeImport(request);
    }
    
    @Override
    public SalesImportProgress getImportProgress(String batchNo) {
        return importService.getImportProgress(batchNo);
    }
}
