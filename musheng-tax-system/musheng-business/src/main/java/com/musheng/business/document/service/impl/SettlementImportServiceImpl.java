package com.musheng.business.document.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.musheng.business.document.dto.SettlementImportRequest;
import com.musheng.business.document.dto.SettlementImportRequest.SettlementImportItem;
import com.musheng.business.document.entity.SettlementImportData;
import com.musheng.business.document.mapper.SettlementImportDataMapper;
import com.musheng.business.document.service.SettlementImportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.util.List;

/**
 * 结算数据导入服务实现类
 *
 * <p>负责解析导入请求中的结算数据，转换为实体并持久化到数据库。
 * 支持按结算周期查询和删除导入数据（用于重新导入）。</p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@Service
@Slf4j
public class SettlementImportServiceImpl implements SettlementImportService {

    @Autowired
    private SettlementImportDataMapper settlementImportDataMapper;

    /**
     * 导入结算数据
     *
     * <p>流程：生成批次ID → 遍历导入项 → 转换为实体 → 逐条持久化。</p>
     *
     * @param request 导入请求（包含周期、站点、MSKU、数量、单价等）
     * @return 导入的数据条数
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importSettlementData(SettlementImportRequest request) {
        if (CollectionUtils.isEmpty(request.getItems())) {
            log.info("导入数据列表为空，跳过导入");
            return 0;
        }

        // 生成导入批次ID
        long importBatchId = System.currentTimeMillis();
        log.info("开始导入结算数据，批次ID: {}, 周期: {} ~ {}, 数据条数: {}",
                importBatchId, request.getPeriodStart(), request.getPeriodEnd(),
                request.getItems().size());

        int count = 0;
        for (SettlementImportItem item : request.getItems()) {
            SettlementImportData entity = new SettlementImportData();
            entity.setShopId(request.getShopId());
            entity.setImportBatchId(importBatchId);
            entity.setPeriodStart(request.getPeriodStart());
            entity.setPeriodEnd(request.getPeriodEnd());
            entity.setSiteCode(item.getSiteCode());
            entity.setMsku(item.getMsku());
            entity.setCurrency(item.getCurrency());
            entity.setUnitPrice(item.getUnitPrice());
            entity.setQuantity(item.getQuantity());
            entity.setAmount(item.getAmount());

            settlementImportDataMapper.insert(entity);
            count++;
        }

        log.info("结算数据导入完成，批次ID: {}, 成功导入 {} 条", importBatchId, count);
        return count;
    }

    /**
     * 根据结算周期查询导入数据
     *
     * @param periodStart 周期起始日
     * @param periodEnd 周期结束日
     * @return 导入数据列表
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Override
    public List<SettlementImportData> queryByPeriod(LocalDate periodStart, LocalDate periodEnd) {
        log.info("查询结算导入数据，周期: {} ~ {}", periodStart, periodEnd);

        LambdaQueryWrapper<SettlementImportData> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SettlementImportData::getPeriodStart, periodStart)
                .eq(SettlementImportData::getPeriodEnd, periodEnd);

        List<SettlementImportData> result = settlementImportDataMapper.selectList(queryWrapper);
        log.info("查询到 {} 条结算导入数据", result.size());
        return result;
    }

    /**
     * 删除指定周期的导入数据（用于重新导入）
     *
     * @param periodStart 周期起始日
     * @param periodEnd 周期结束日
     * @return 删除的数据条数
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteByPeriod(LocalDate periodStart, LocalDate periodEnd) {
        log.info("删除结算导入数据，周期: {} ~ {}", periodStart, periodEnd);

        LambdaQueryWrapper<SettlementImportData> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SettlementImportData::getPeriodStart, periodStart)
                .eq(SettlementImportData::getPeriodEnd, periodEnd);

        int deleted = settlementImportDataMapper.delete(queryWrapper);
        log.info("删除结算导入数据完成，共删除 {} 条", deleted);
        return deleted;
    }
}
