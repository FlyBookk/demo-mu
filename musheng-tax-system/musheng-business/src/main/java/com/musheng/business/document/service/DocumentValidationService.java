package com.musheng.business.document.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 数据校验服务接口
 *
 * <p>提供结算单与INV一致性校验、INV日期校验、结算单序号与站点映射校验、全量校验。</p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
public interface DocumentValidationService {

    /**
     * 校验结算单与INV的数据一致性
     *
     * <p>比较MSKU列表、数量、单价、金额是否完全一致。</p>
     *
     * @param settlementId 结算单ID
     * @param invId INV发票ID
     * @return 差异描述列表，空列表表示一致
     * @author wanhua
     * 10:30 2026年01月29日
     */
    List<String> validateSettlementInvConsistency(Long settlementId, Long invId);

    /**
     * 校验INV日期是否为结算日+1工作日
     *
     * @param invId INV发票ID
     * @return 日期校验通过返回 true，否则返回 false
     * @author wanhua
     * 10:30 2026年01月29日
     */
    boolean validateInvDate(Long invId);

    /**
     * 校验结算单序号与站点映射
     *
     * <p>验证 001→USD, 002→CAD, 003→GBP, 004→EUR 映射关系。</p>
     *
     * @param settlementId 结算单ID
     * @return 映射校验通过返回 true，否则返回 false
     * @author wanhua
     * 10:30 2026年01月29日
     */
    boolean validateSettlementSiteMapping(Long settlementId);

    /**
     * 全量校验（一致性+日期+站点）
     *
     * <p>按 periodStart 和 periodEnd 查询所有结算单，
     * 对每份结算单执行一致性校验、日期校验、站点映射校验。</p>
     *
     * @param periodStart 结算周期起始日
     * @param periodEnd 结算周期结束日
     * @return Map&lt;校验类型, 错误列表&gt;
     * @author wanhua
     * 10:30 2026年01月29日
     */
    Map<String, List<String>> validateAll(LocalDate periodStart, LocalDate periodEnd);
}
