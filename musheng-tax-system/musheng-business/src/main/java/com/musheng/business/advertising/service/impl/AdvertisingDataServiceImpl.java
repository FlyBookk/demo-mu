package com.musheng.business.advertising.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.business.advertising.dto.AdvertisingDataImportBatchRequest;
import com.musheng.business.advertising.dto.AdvertisingDataImportRequest;
import com.musheng.business.advertising.dto.AdvertisingDataImportResponse;
import com.musheng.business.advertising.dto.AdvertisingDataRequest;
import com.musheng.business.advertising.entity.AdvertisingData;
import com.musheng.business.advertising.mapper.AdvertisingDataMapper;
import com.musheng.business.advertising.service.AdvertisingDataService;
import com.musheng.common.context.ShopContext;
import com.musheng.common.exception.BusinessException;
import com.musheng.common.result.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.*;

/**
 * Advertising Data Service Implementation
 * P0 Requirement: Advertising data CRUD API
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdvertisingDataServiceImpl implements AdvertisingDataService {

    private final AdvertisingDataMapper advertisingDataMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdvertisingData create(AdvertisingDataRequest request) {
        AdvertisingData entity = new AdvertisingData();
        copyProperties(request, entity);

        // 设置店铺ID
        entity.setShopId(ShopContext.requireShopId());

        // Set created by
        if (StpUtil.isLogin()) {
            entity.setCreateBy(StpUtil.getLoginIdAsLong());
        }

        advertisingDataMapper.insert(entity);
        log.info("Created advertising data: id={}, siteCode={}", entity.getId(), entity.getSiteCode());

        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdvertisingData update(Long id, AdvertisingDataRequest request) {
        AdvertisingData entity = advertisingDataMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST, "Advertising data not found");
        }

        copyProperties(request, entity);

        // Set updated by
        if (StpUtil.isLogin()) {
            entity.setUpdateBy(StpUtil.getLoginIdAsLong());
        }

        advertisingDataMapper.updateById(entity);
        log.info("Updated advertising data: id={}", id);

        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        AdvertisingData entity = advertisingDataMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST, "Advertising data not found");
        }

        advertisingDataMapper.deleteById(id);
        log.info("Deleted advertising data: id={}", id);
    }

    @Override
    public AdvertisingData getById(Long id) {
        AdvertisingData entity = advertisingDataMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST, "Advertising data not found");
        }
        return entity;
    }

    @Override
    public Page<AdvertisingData> list(String siteCode, String yearMonth, int page, int size) {
        LambdaQueryWrapper<AdvertisingData> wrapper = new LambdaQueryWrapper<>();

        // 店铺数据隔离
        Long shopId = ShopContext.requireShopId();
        wrapper.eq(AdvertisingData::getShopId, shopId);

        if (StringUtils.hasText(siteCode)) {
            wrapper.eq(AdvertisingData::getSiteCode, siteCode);
        }

        wrapper.orderByDesc(AdvertisingData::getCreateTime);

        return advertisingDataMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdvertisingDataImportResponse importData(AdvertisingDataImportBatchRequest request) {
        List<AdvertisingDataImportRequest> dataList = request.getData();
        int totalCount = dataList.size();

        // 生成导入批次ID
        String importBatchId = request.getImportBatchId();
        if (!StringUtils.hasText(importBatchId)) {
            importBatchId = "BATCH-" + System.currentTimeMillis();
        }

        // 数据去重：使用LinkedHashMap保证顺序，保留第一条记录
        Map<String, AdvertisingDataImportRequest> uniqueMap = new LinkedHashMap<>();
        List<String> duplicatedInvoices = new ArrayList<>();

        for (AdvertisingDataImportRequest item : dataList) {
            String invoiceNumber = item.getInvoiceNumber();
            if (uniqueMap.containsKey(invoiceNumber)) {
                duplicatedInvoices.add(invoiceNumber);
            } else {
                uniqueMap.put(invoiceNumber, item);
            }
        }

        // 数据校验和导入
        List<AdvertisingDataImportResponse.ImportFailureDetail> failedRecords = new ArrayList<>();
        List<AdvertisingData> entitiesToInsert = new ArrayList<>();
        int index = 0;

        Long currentUserId = null;
        if (StpUtil.isLogin()) {
            currentUserId = StpUtil.getLoginIdAsLong();
        }
        
        // 获取当前店铺ID
        Long shopId = ShopContext.requireShopId();

        for (AdvertisingDataImportRequest item : uniqueMap.values()) {
            try {
                // 数据校验
                validateImportData(item);

                // 转换为实体
                AdvertisingData entity = convertToEntity(item);
                entity.setShopId(shopId);  // 设置店铺ID
                entity.setImportBatchId(importBatchId);
                entity.setCreateBy(currentUserId);

                entitiesToInsert.add(entity);
            } catch (Exception e) {
                log.error("Failed to import advertising data: invoiceNumber={}, error={}",
                        item.getInvoiceNumber(), e.getMessage());

                failedRecords.add(AdvertisingDataImportResponse.ImportFailureDetail.builder()
                        .index(index)
                        .invoiceNumber(item.getInvoiceNumber())
                        .errorMessage(e.getMessage())
                        .build());
            }
            index++;
        }

        // 批量插入
        int importedCount = 0;
        for (AdvertisingData entity : entitiesToInsert) {
            try {
                advertisingDataMapper.insert(entity);
                importedCount++;
            } catch (Exception e) {
                log.error("Failed to insert advertising data: invoiceNumber={}, error={}",
                        entity.getInvoiceNumber(), e.getMessage());

                failedRecords.add(AdvertisingDataImportResponse.ImportFailureDetail.builder()
                        .invoiceNumber(entity.getInvoiceNumber())
                        .errorMessage("数据库插入失败: " + e.getMessage())
                        .build());
            }
        }

        int duplicatedCount = duplicatedInvoices.size();
        int failedCount = failedRecords.size();

        log.info("Import completed: total={}, imported={}, duplicated={}, failed={}",
                totalCount, importedCount, duplicatedCount, failedCount);

        return AdvertisingDataImportResponse.builder()
                .totalCount(totalCount)
                .importedCount(importedCount)
                .duplicatedCount(duplicatedCount)
                .failedCount(failedCount)
                .importBatchId(importBatchId)
                .duplicatedInvoices(duplicatedInvoices)
                .failedRecords(failedRecords)
                .build();
    }

    @Override
    public Page<AdvertisingData> listByConditions(String siteCode, LocalDate billingStartDate,
                                                   LocalDate billingEndDate, String invoiceNumber,
                                                   int page, int size) {
        LambdaQueryWrapper<AdvertisingData> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(siteCode)) {
            wrapper.eq(AdvertisingData::getSiteCode, siteCode);
        }

        if (billingStartDate != null) {
            wrapper.ge(AdvertisingData::getBillingStartDate, billingStartDate);
        }

        if (billingEndDate != null) {
            wrapper.le(AdvertisingData::getBillingEndDate, billingEndDate);
        }

        if (StringUtils.hasText(invoiceNumber)) {
            wrapper.eq(AdvertisingData::getInvoiceNumber, invoiceNumber);
        }

        wrapper.orderByDesc(AdvertisingData::getCreateTime);

        return advertisingDataMapper.selectPage(new Page<>(page, size), wrapper);
    }

    /**
     * Copy request properties to entity
     * 兼容旧的简化版API（yearMonth, invoiceNo, amount, currencyCode）映射到新字段
     */
    private void copyProperties(AdvertisingDataRequest request, AdvertisingData entity) {
        entity.setSiteCode(request.getSiteCode());
        entity.setAttachmentPath(request.getAttachmentPath());
        entity.setRemark(request.getRemark());

        // 根据 siteCode 生成 storeName（必填字段）
        // 格式: "慕声-{站点编码}" 例如: "慕声-CA"
        if (request.getSiteCode() != null) {
            entity.setStoreName("慕声-" + request.getSiteCode());
        }

        // 映射旧字段到新字段
        // yearMonth (YYYY-MM) -> 转换为账单周期（当月第一天到最后一天）
        if (request.getYearMonth() != null) {
            LocalDate startDate = LocalDate.parse(request.getYearMonth() + "-01");
            LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
            entity.setBillingStartDate(startDate);
            entity.setBillingEndDate(endDate);
        }

        // invoiceNo -> invoiceNumber (如果用户未提供，自动生成)
        if (request.getInvoiceNo() != null && !request.getInvoiceNo().isBlank()) {
            entity.setInvoiceNumber(request.getInvoiceNo());
        } else {
            // 自动生成发票编号: INV-{站点}-{年月}-{时间戳后6位}
            String timestamp = String.valueOf(System.currentTimeMillis());
            String shortTimestamp = timestamp.substring(timestamp.length() - 6);
            entity.setInvoiceNumber("INV-" + request.getSiteCode() + "-" +
                                   request.getYearMonth() + "-" + shortTimestamp);
        }

        // amount -> cost (同时设置invoiceAmount)
        if (request.getAmount() != null) {
            entity.setCost(request.getAmount());
            entity.setInvoiceAmount(request.getAmount());
        }

        // currencyCode -> currency
        if (request.getCurrencyCode() != null) {
            entity.setCurrency(request.getCurrencyCode());
        }

        // 设置默认的发票状态
        if (entity.getInvoiceStatus() == null) {
            entity.setInvoiceStatus("PAID_IN_FULL");
        }

        // 设置开具日期为当前日期
        if (entity.getIssueDate() == null) {
            entity.setIssueDate(LocalDate.now());
        }
    }

    /**
     * Validate import data
     */
    private void validateImportData(AdvertisingDataImportRequest data) {
        // 校验账单周期：开始日期 <= 结束日期
        if (data.getBillingStartDate().isAfter(data.getBillingEndDate())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账单开始日期不能晚于结束日期");
        }

        // 校验账单周期合理性：不超过31天（警告级别，仅记录日志）
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(
                data.getBillingStartDate(), data.getBillingEndDate());
        if (daysBetween > 31) {
            log.warn("Billing period exceeds 31 days: invoiceNumber={}, days={}",
                    data.getInvoiceNumber(), daysBetween);
        }

        // 校验币种
        Set<String> validCurrencies = Set.of("USD", "CAD", "GBP", "EUR");
        if (!validCurrencies.contains(data.getCurrency())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "不支持的币种: " + data.getCurrency());
        }
    }

    /**
     * Convert import request to entity
     */
    private AdvertisingData convertToEntity(AdvertisingDataImportRequest request) {
        AdvertisingData entity = new AdvertisingData();

        entity.setStoreName(request.getStoreName());
        entity.setSiteCode(request.getSiteCode());
        entity.setInvoiceNumber(request.getInvoiceNumber());
        entity.setInvoiceStatus(request.getInvoiceStatus());
        entity.setPaymentType(request.getPaymentType());
        entity.setBillingStartDate(request.getBillingStartDate());
        entity.setBillingEndDate(request.getBillingEndDate());
        entity.setIssueDate(request.getIssueDate());
        entity.setCurrency(request.getCurrency());
        entity.setInvoiceAmount(request.getInvoiceAmount());
        entity.setCost(request.getCost());
        entity.setOtherCost(request.getOtherCost());
        entity.setCampaignName(request.getCampaignName());
        entity.setCampaignId(request.getCampaignId());
        entity.setPricingModel(request.getPricingModel());
        entity.setClicks(request.getClicks());
        entity.setAvgCpc(request.getAvgCpc());
        entity.setDataSource(request.getDataSource());
        entity.setProductList(request.getProductList());
        entity.setAdType(request.getAdType());
        entity.setAttachmentPath(request.getAttachmentPath());
        entity.setRemark(request.getRemark());
        entity.setExchangeRate(request.getExchangeRate());

        // 如果提供了汇率，自动计算人民币金额
        if (request.getExchangeRate() != null && request.getCost() != null) {
            entity.setAmountCny(request.getCost().multiply(request.getExchangeRate()));
        }

        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(java.util.List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "ID列表不能为空");
        }

        log.info("批量删除广告数据: count={}", ids.size());
        advertisingDataMapper.deleteBatchIds(ids);
    }
}
