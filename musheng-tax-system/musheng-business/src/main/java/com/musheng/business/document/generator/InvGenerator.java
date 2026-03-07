package com.musheng.business.document.generator;

import com.musheng.business.common.config.DocumentPartyProperties;
import com.musheng.business.document.entity.DocumentInv;
import com.musheng.business.document.entity.DocumentInvItem;
import com.musheng.business.document.entity.DocumentSettlement;
import com.musheng.business.document.entity.DocumentSettlementItem;
import com.musheng.business.document.utils.DocumentNumberCalculator;
import com.musheng.business.document.utils.WorkingDayCalculator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * INV发票生成器
 *
 * <p>纯函数设计，无状态，不依赖系统时间，确保确定性输出。
 * 核心逻辑：基于结算单数据 → INV日期=nextWorkingDay(结算日) →
 * 复制MSKU/数量/单价/金额 → 生成编号 → 填充卖方/买方/银行信息。
 * 卖方/买方/银行信息通过 DocumentPartyProperties 配置注入，不硬编码。</p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
public final class InvGenerator {

    private InvGenerator() {
        // 工具类，禁止实例化
    }

    /**
     * 根据结算单生成结果列表生成INV发票
     *
     * <p>算法流程：
     * 1. 遍历每份结算单
     * 2. INV日期 = nextWorkingDay(结算日)
     * 3. 复制结算单的 MSKU/数量/单价/金额 到 INV 明细
     * 4. 生成编号（使用 DocumentNumberCalculator）
     * 5. 从 party 配置填充卖方/买方/银行信息
     * 6. 站点序号和站点代码从结算单复制
     * 7. 合计（totalQuantity, totalAmount）从结算单复制</p>
     *
     * @param settlementResults 结算单生成结果列表，不能为 null
     * @param startSequence 起始编号序号
     * @param party 交易方信息配置（卖方/买方/银行）
     * @return INV生成结果列表
     * @throws IllegalArgumentException 如果 settlementResults 为 null
     * @author wanhua
     * 10:30 2026年01月29日
     */
    public static List<InvGenerateResult> generate(List<SettlementGenerateResult> settlementResults,
                                                    int startSequence,
                                                    DocumentPartyProperties party) {
        if (settlementResults == null) {
            throw new IllegalArgumentException("结算单结果列表不能为 null");
        }
        if (settlementResults.isEmpty()) {
            return List.of();
        }

        List<InvGenerateResult> results = new ArrayList<>();
        int sequence = startSequence;

        for (SettlementGenerateResult settlementResult : settlementResults) {
            InvGenerateResult invResult = buildInv(settlementResult, sequence, party);
            results.add(invResult);
            sequence++;
        }

        return results;
    }

    /**
     * 根据单份结算单构建INV
     *
     * @param settlementResult 结算单生成结果
     * @param sequence 编号序号
     * @param party 交易方信息配置
     * @return INV生成结果
     * @author wanhua
     * 10:30 2026年01月29日
     */
    private static InvGenerateResult buildInv(SettlementGenerateResult settlementResult,
                                               int sequence,
                                               DocumentPartyProperties party) {
        DocumentSettlement settlement = settlementResult.getSettlement();

        // 计算INV日期：结算日的下一个工作日
        LocalDate invDate = WorkingDayCalculator.nextWorkingDay(settlement.getSettlementDate());

        // 生成编号
        String documentNo = DocumentNumberCalculator.generate(invDate, sequence);

        // 复制结算单明细到INV明细
        List<DocumentInvItem> invItems = copySettlementItems(settlementResult.getItems());

        // 构建INV主表
        DocumentInv inv = new DocumentInv();
        inv.setDocumentNo(documentNo);
        inv.setInvDate(invDate);
        inv.setSiteCode(settlement.getSiteCode());
        inv.setSiteSequence(settlement.getSiteSequence());
        // 卖方信息（来自配置）
        inv.setSellerName(party.getSellerName());
        inv.setSellerAddress(party.getSellerAddress());
        inv.setSellerPhone(party.getSellerPhone());
        // 买方信息（来自配置）
        inv.setBuyerName(party.getBuyerName());
        inv.setBuyerAddress(party.getBuyerAddress());
        inv.setBuyerPhone(party.getBuyerPhone());
        // 合计
        inv.setTotalQuantity(settlement.getTotalQuantity());
        inv.setTotalAmount(settlement.getTotalAmount());
        // 银行信息（来自配置）
        inv.setBankAccountName(party.getBankAccountName());
        inv.setBankAccountNumber(party.getBankAccountNumber());
        inv.setBankName(party.getBankName());
        inv.setBankAddress(party.getBankAddress());
        inv.setSwiftCode(party.getSwiftCode());

        return InvGenerateResult.builder()
                .inv(inv)
                .items(invItems)
                .build();
    }

    /**
     * 复制结算单明细到INV明细
     *
     * <p>逐行复制 lineNo、msku、quantity、unitPrice、amount。</p>
     *
     * @param settlementItems 结算单明细列表
     * @return INV明细列表
     * @author wanhua
     * 10:30 2026年01月29日
     */
    private static List<DocumentInvItem> copySettlementItems(List<DocumentSettlementItem> settlementItems) {
        List<DocumentInvItem> invItems = new ArrayList<>();
        for (DocumentSettlementItem sItem : settlementItems) {
            DocumentInvItem invItem = new DocumentInvItem();
            invItem.setLineNo(sItem.getLineNo());
            invItem.setMsku(sItem.getMsku());
            invItem.setQuantity(sItem.getQuantity());
            invItem.setUnitPrice(sItem.getUnitPrice());
            invItem.setAmount(sItem.getAmount());
            invItems.add(invItem);
        }
        return invItems;
    }
}
