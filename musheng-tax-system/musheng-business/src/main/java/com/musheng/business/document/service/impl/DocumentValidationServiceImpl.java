package com.musheng.business.document.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.musheng.business.document.entity.*;
import com.musheng.business.document.enums.SiteCode;
import com.musheng.business.document.mapper.*;
import com.musheng.business.document.service.DocumentValidationService;
import com.musheng.business.document.utils.WorkingDayCalculator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据校验服务实现类
 *
 * <p>实现结算单与INV一致性校验、INV日期校验、结算单序号与站点映射校验、全量校验。</p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@Service
@Slf4j
public class DocumentValidationServiceImpl implements DocumentValidationService {

    @Autowired
    private DocumentSettlementMapper documentSettlementMapper;
    @Autowired
    private DocumentSettlementItemMapper documentSettlementItemMapper;
    @Autowired
    private DocumentInvMapper documentInvMapper;
    @Autowired
    private DocumentInvItemMapper documentInvItemMapper;

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
    @Override
    public List<String> validateSettlementInvConsistency(Long settlementId, Long invId) {
        log.info("校验结算单与INV一致性, settlementId={}, invId={}", settlementId, invId);
        List<String> errors = new ArrayList<>();

        // 查询结算单明细
        LambdaQueryWrapper<DocumentSettlementItem> sQuery = new LambdaQueryWrapper<DocumentSettlementItem>()
                .eq(DocumentSettlementItem::getSettlementId, settlementId)
                .orderByAsc(DocumentSettlementItem::getMsku);
        List<DocumentSettlementItem> settlementItems = documentSettlementItemMapper.selectList(sQuery);

        // 查询INV明细
        LambdaQueryWrapper<DocumentInvItem> invQuery = new LambdaQueryWrapper<DocumentInvItem>()
                .eq(DocumentInvItem::getInvId, invId)
                .orderByAsc(DocumentInvItem::getMsku);
        List<DocumentInvItem> invItems = documentInvItemMapper.selectList(invQuery);

        // 构建结算单MSKU映射
        Map<String, DocumentSettlementItem> settlementMap = settlementItems.stream()
                .collect(Collectors.toMap(DocumentSettlementItem::getMsku, item -> item, (a, b) -> a));

        // 构建INV MSKU映射
        Map<String, DocumentInvItem> invMap = invItems.stream()
                .collect(Collectors.toMap(DocumentInvItem::getMsku, item -> item, (a, b) -> a));

        // 检查结算单中有但INV中没有的MSKU
        Set<String> settlementMskus = settlementMap.keySet();
        Set<String> invMskus = invMap.keySet();

        Set<String> onlyInSettlement = new HashSet<>(settlementMskus);
        onlyInSettlement.removeAll(invMskus);
        for (String msku : onlyInSettlement) {
            errors.add("MSKU " + msku + " 存在于结算单但不存在于INV");
        }

        Set<String> onlyInInv = new HashSet<>(invMskus);
        onlyInInv.removeAll(settlementMskus);
        for (String msku : onlyInInv) {
            errors.add("MSKU " + msku + " 存在于INV但不存在于结算单");
        }

        // 比较共有MSKU的数量、单价、金额
        Set<String> commonMskus = new HashSet<>(settlementMskus);
        commonMskus.retainAll(invMskus);
        for (String msku : commonMskus) {
            DocumentSettlementItem sItem = settlementMap.get(msku);
            DocumentInvItem iItem = invMap.get(msku);

            if (!Objects.equals(sItem.getQuantity(), iItem.getQuantity())) {
                errors.add("MSKU " + msku + " 数量不一致: 结算单=" + sItem.getQuantity() + ", INV=" + iItem.getQuantity());
            }
            if (sItem.getUnitPrice().compareTo(iItem.getUnitPrice()) != 0) {
                errors.add("MSKU " + msku + " 单价不一致: 结算单=" + sItem.getUnitPrice() + ", INV=" + iItem.getUnitPrice());
            }
            if (sItem.getAmount().compareTo(iItem.getAmount()) != 0) {
                errors.add("MSKU " + msku + " 金额不一致: 结算单=" + sItem.getAmount() + ", INV=" + iItem.getAmount());
            }
        }

        if (errors.isEmpty()) {
            log.info("结算单与INV数据一致, settlementId={}, invId={}", settlementId, invId);
        } else {
            log.warn("结算单与INV数据存在差异, settlementId={}, invId={}, 差异数={}", settlementId, invId, errors.size());
        }

        return errors;
    }

    /**
     * 校验INV日期是否为结算日+1工作日
     *
     * @param invId INV发票ID
     * @return 日期校验通过返回 true，否则返回 false
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Override
    public boolean validateInvDate(Long invId) {
        log.info("校验INV日期, invId={}", invId);

        DocumentInv inv = documentInvMapper.selectById(invId);
        if (inv == null) {
            log.warn("INV不存在, invId={}", invId);
            return false;
        }

        DocumentSettlement settlement = documentSettlementMapper.selectById(inv.getSettlementId());
        if (settlement == null) {
            log.warn("关联结算单不存在, settlementId={}", inv.getSettlementId());
            return false;
        }

        LocalDate expectedInvDate = WorkingDayCalculator.nextWorkingDay(settlement.getSettlementDate());
        boolean valid = expectedInvDate.equals(inv.getInvDate());

        if (valid) {
            log.info("INV日期校验通过, invId={}, 期望={}, 实际={}", invId, expectedInvDate, inv.getInvDate());
        } else {
            log.warn("INV日期校验失败, invId={}, 期望={}, 实际={}", invId, expectedInvDate, inv.getInvDate());
        }

        return valid;
    }

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
    @Override
    public boolean validateSettlementSiteMapping(Long settlementId) {
        log.info("校验结算单站点映射, settlementId={}", settlementId);

        DocumentSettlement settlement = documentSettlementMapper.selectById(settlementId);
        if (settlement == null) {
            log.warn("结算单不存在, settlementId={}", settlementId);
            return false;
        }

        String siteSequence = settlement.getSiteSequence();
        String siteCode = settlement.getSiteCode();

        SiteCode expectedSite = SiteCode.fromSequence(siteSequence);
        if (expectedSite == null) {
            log.warn("未知的站点序号, siteSequence={}", siteSequence);
            return false;
        }

        boolean valid = expectedSite.getCurrency().equals(siteCode);

        if (valid) {
            log.info("站点映射校验通过, settlementId={}, {}→{}", settlementId, siteSequence, siteCode);
        } else {
            log.warn("站点映射校验失败, settlementId={}, 序号={}, 期望货币={}, 实际货币={}",
                    settlementId, siteSequence, expectedSite.getCurrency(), siteCode);
        }

        return valid;
    }

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
    @Override
    public Map<String, List<String>> validateAll(LocalDate periodStart, LocalDate periodEnd) {
        log.info("执行全量校验, periodStart={}, periodEnd={}", periodStart, periodEnd);

        Map<String, List<String>> result = new LinkedHashMap<>();
        result.put("consistency", new ArrayList<>());
        result.put("invDate", new ArrayList<>());
        result.put("siteMapping", new ArrayList<>());

        // 查询结算周期内的所有结算单
        LambdaQueryWrapper<DocumentSettlement> settlementQuery = new LambdaQueryWrapper<DocumentSettlement>()
                .eq(DocumentSettlement::getPeriodStart, periodStart)
                .eq(DocumentSettlement::getPeriodEnd, periodEnd);
        List<DocumentSettlement> settlements = documentSettlementMapper.selectList(settlementQuery);

        if (CollectionUtils.isEmpty(settlements)) {
            log.info("未找到结算单, periodStart={}, periodEnd={}", periodStart, periodEnd);
            return result;
        }

        // 查询关联的INV
        List<Long> settlementIds = settlements.stream()
                .map(DocumentSettlement::getId)
                .collect(Collectors.toList());
        LambdaQueryWrapper<DocumentInv> invQuery = new LambdaQueryWrapper<DocumentInv>()
                .in(DocumentInv::getSettlementId, settlementIds);
        List<DocumentInv> invs = documentInvMapper.selectList(invQuery);

        // 构建结算单ID→INV的映射
        Map<Long, DocumentInv> invBySettlementId = invs.stream()
                .collect(Collectors.toMap(DocumentInv::getSettlementId, inv -> inv, (a, b) -> a));

        for (DocumentSettlement settlement : settlements) {
            String docNo = settlement.getDocumentNo();

            // 站点映射校验
            if (!validateSettlementSiteMapping(settlement.getId())) {
                result.get("siteMapping").add("结算单 " + docNo + " 站点映射校验失败");
            }

            // 查找关联INV
            DocumentInv inv = invBySettlementId.get(settlement.getId());
            if (inv == null) {
                result.get("consistency").add("结算单 " + docNo + " 未找到关联INV");
                result.get("invDate").add("结算单 " + docNo + " 未找到关联INV，无法校验日期");
                continue;
            }

            // 一致性校验
            List<String> consistencyErrors = validateSettlementInvConsistency(settlement.getId(), inv.getId());
            if (!CollectionUtils.isEmpty(consistencyErrors)) {
                for (String error : consistencyErrors) {
                    result.get("consistency").add("结算单 " + docNo + ": " + error);
                }
            }

            // INV日期校验
            if (!validateInvDate(inv.getId())) {
                result.get("invDate").add("结算单 " + docNo + " 关联的INV日期校验失败");
            }
        }

        log.info("全量校验完成, 一致性错误={}, 日期错误={}, 站点映射错误={}",
                result.get("consistency").size(),
                result.get("invDate").size(),
                result.get("siteMapping").size());

        return result;
    }
}
