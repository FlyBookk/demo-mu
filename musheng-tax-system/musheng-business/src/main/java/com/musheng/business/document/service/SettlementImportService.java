package com.musheng.business.document.service;

import com.musheng.business.document.dto.SettlementImportRequest;
import com.musheng.business.document.entity.SettlementImportData;

import java.time.LocalDate;
import java.util.List;

/**
 * 结算数据导入服务接口
 *
 * <p>负责解析导入数据并存储到 t_settlement_import_data 表，
 * 支持按结算周期查询和删除导入数据。</p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
public interface SettlementImportService {

    /**
     * 导入结算数据
     *
     * @param request 导入请求（包含周期、站点、MSKU、数量、单价等）
     * @return 导入的数据条数
     * @author wanhua
     * 10:30 2026年01月29日
     */
    int importSettlementData(SettlementImportRequest request);

    /**
     * 根据结算周期查询导入数据
     *
     * @param periodStart 周期起始日
     * @param periodEnd 周期结束日
     * @return 导入数据列表
     * @author wanhua
     * 10:30 2026年01月29日
     */
    List<SettlementImportData> queryByPeriod(LocalDate periodStart, LocalDate periodEnd);

    /**
     * 删除指定周期的导入数据（用于重新导入）
     *
     * @param periodStart 周期起始日
     * @param periodEnd 周期结束日
     * @return 删除的数据条数
     * @author wanhua
     * 10:30 2026年01月29日
     */
    int deleteByPeriod(LocalDate periodStart, LocalDate periodEnd);
}
