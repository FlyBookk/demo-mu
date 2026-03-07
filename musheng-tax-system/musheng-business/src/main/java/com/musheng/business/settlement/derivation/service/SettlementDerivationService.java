package com.musheng.business.settlement.derivation.service;

import com.musheng.business.settlement.derivation.dto.DerivationConfirmRequest;
import com.musheng.business.settlement.derivation.dto.DerivationRequest;
import com.musheng.business.settlement.derivation.vo.DerivationConfirmResultVO;
import com.musheng.business.settlement.derivation.vo.DerivationResultVO;

/**
 * 结算数据推导服务接口（按季度）
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
public interface SettlementDerivationService {

    /**
     * 执行推导计算（按季度范围）
     *
     * @param request 推导请求，包含开始/结束季度和各站点采购成本
     * @return 推导结果
     * @author wanhua
     * 10:30 2026年01月29日
     */
    DerivationResultVO derive(DerivationRequest request);

    /**
     * 确认写入推导结果（按季度范围）
     *
     * @param request 确认写入请求
     * @return 写入结果
     * @author wanhua
     * 10:30 2026年01月29日
     */
    DerivationConfirmResultVO confirm(DerivationConfirmRequest request);

    /**
     * 检查指定季度范围是否已有推导数据
     *
     * @param startQuarter 开始季度，如 2025-Q3
     * @param endQuarter 结束季度，如 2025-Q3
     * @return 存在返回 true，否则返回 false
     * @author wanhua
     * 10:30 2026年01月29日
     */
    boolean checkExistingData(String startQuarter, String endQuarter);
}
