package com.musheng.tiktok.settlement.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.musheng.common.context.ShopContext;
import com.musheng.tiktok.settlement.entity.TiktokSettlementOrder;
import com.musheng.tiktok.settlement.mapper.TiktokSettlementOrderMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * TK报税汇总服务（按季度聚合，含月度明细）
 *
 * @author wanhua
 * 21:12 2026年05月14日
 */
@Service
@Slf4j
public class TiktokTaxSummaryService {

    @Autowired
    private TiktokSettlementOrderMapper orderMapper;

    /**
     * 报税口径汇总（按季度，含月度明细）
     *
     * @param quarter 格式: "2025-Q3"
     * @param siteCode 站点代码
     * @param exchangeRate 汇率
     */
    public QuarterTaxSummary getQuarterTaxSummary(String quarter, String siteCode, BigDecimal exchangeRate) {
        Long shopId = ShopContext.requireShopId();
        LocalDate[] range = parseQuarter(quarter);
        LocalDate start = range[0];
        LocalDate end = range[1];

        QuarterTaxSummary result = new QuarterTaxSummary();
        result.setQuarter(quarter);
        result.setSiteCode(siteCode);
        result.setExchangeRate(exchangeRate);

        List<MonthTaxSummary> months = new ArrayList<>();
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalRefund = BigDecimal.ZERO;
        BigDecimal totalServiceFee = BigDecimal.ZERO;

        // 按月拆分
        LocalDate monthStart = start;
        while (monthStart.isBefore(end) || monthStart.isEqual(end)) {
            LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
            if (monthEnd.isAfter(end)) monthEnd = end;

            MonthTaxSummary monthSummary = calcMonthTax(shopId, siteCode, monthStart, monthEnd, exchangeRate);
            months.add(monthSummary);

            totalRevenue = totalRevenue.add(monthSummary.getRevenueUsd());
            totalRefund = totalRefund.add(monthSummary.getRefundUsd());

            monthStart = monthStart.plusMonths(1);
        }

        // 服务费：按季度级别 |SUM| 后取 MAX，不按月加总
        totalServiceFee = calcQuarterServiceFee(shopId, siteCode, start, end);

        result.setMonths(months);
        result.setTotalRevenueUsd(totalRevenue);
        result.setTotalRefundUsd(totalRefund);
        result.setTotalServiceFeeUsd(totalServiceFee);
        result.setTotalRevenueRmb(totalRevenue.multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP));
        result.setTotalRefundRmb(totalRefund.multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP));
        result.setTotalServiceFeeRmb(totalServiceFee.multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP));
        return result;
    }

    /**
     * 运营口径汇总（按季度）
     */
    public QuarterOperationSummary getQuarterOperationSummary(String quarter, String siteCode, BigDecimal exchangeRate) {
        Long shopId = ShopContext.requireShopId();
        LocalDate[] range = parseQuarter(quarter);

        List<TiktokSettlementOrder> all = orderMapper.selectList(
                new LambdaQueryWrapper<TiktokSettlementOrder>()
                        .eq(TiktokSettlementOrder::getShopId, shopId)
                        .eq(TiktokSettlementOrder::getSiteCode, siteCode)
                        .ge(TiktokSettlementOrder::getStatementDate, range[0])
                        .le(TiktokSettlementOrder::getStatementDate, range[1]));

        QuarterOperationSummary summary = new QuarterOperationSummary();
        summary.setQuarter(quarter);
        summary.setSiteCode(siteCode);
        summary.setExchangeRate(exchangeRate);

        BigDecimal revenue = BigDecimal.ZERO, refund = BigDecimal.ZERO;
        BigDecimal commission = BigDecimal.ZERO, logistics = BigDecimal.ZERO;
        BigDecimal affiliate = BigDecimal.ZERO, promotion = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO, other = BigDecimal.ZERO;
        BigDecimal adjustIncome = BigDecimal.ZERO, adjustExpense = BigDecimal.ZERO;

        for (TiktokSettlementOrder o : all) {
            if ("Order".equals(o.getType())) {
                revenue = revenue.add(safe(o.getSubtotalAfterDiscount()));
                refund = refund.add(safe(o.getRefundAfterDiscount()));
                commission = commission.add(safe(o.getCommissionFee()));
                logistics = logistics.add(safe(o.getLogisticsFee()));
                affiliate = affiliate.add(safe(o.getAffiliateFee()));
                promotion = promotion.add(safe(o.getPromotionFee()));
                tax = tax.add(safe(o.getTaxFee()));
                other = other.add(safe(o.getOtherFee()));
            } else {
                BigDecimal amt = safe(o.getTotalSettlementAmount());
                if (amt.compareTo(BigDecimal.ZERO) > 0) adjustIncome = adjustIncome.add(amt);
                else adjustExpense = adjustExpense.add(amt);
            }
        }

        BigDecimal netRevenue = revenue.add(refund);
        BigDecimal orderProfit = netRevenue.add(commission).add(logistics).add(affiliate).add(promotion).add(tax).add(other);
        BigDecimal netProfit = orderProfit.add(adjustIncome).add(adjustExpense);

        summary.setNetRevenue(netRevenue);
        summary.setCommission(commission);
        summary.setLogistics(logistics);
        summary.setAffiliate(affiliate);
        summary.setPromotion(promotion);
        summary.setTax(tax);
        summary.setOther(other);
        summary.setOrderProfit(orderProfit);
        summary.setAdjustmentIncome(adjustIncome);
        summary.setAdjustmentExpense(adjustExpense);
        summary.setNetProfit(netProfit);
        summary.setMarginRate(netRevenue.compareTo(BigDecimal.ZERO) != 0
                ? netProfit.divide(netRevenue, 4, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        return summary;
    }

    // ==================== 私有方法 ====================

    private MonthTaxSummary calcMonthTax(Long shopId, String siteCode, LocalDate start, LocalDate end, BigDecimal exchangeRate) {
        List<TiktokSettlementOrder> orders = orderMapper.selectList(
                new LambdaQueryWrapper<TiktokSettlementOrder>()
                        .eq(TiktokSettlementOrder::getShopId, shopId)
                        .eq(TiktokSettlementOrder::getSiteCode, siteCode)
                        .eq(TiktokSettlementOrder::getType, "Order")
                        .ge(TiktokSettlementOrder::getStatementDate, start)
                        .le(TiktokSettlementOrder::getStatementDate, end));

        BigDecimal revenue = BigDecimal.ZERO;
        BigDecimal refundAfter = BigDecimal.ZERO;
        BigDecimal refundOfDisc = BigDecimal.ZERO;
        BigDecimal returnReimb = BigDecimal.ZERO;
        BigDecimal referral = BigDecimal.ZERO;
        BigDecimal sellerShip = BigDecimal.ZERO;
        BigDecimal fbt = BigDecimal.ZERO;
        BigDecimal refundAdmin = BigDecimal.ZERO;
        BigDecimal returnShip = BigDecimal.ZERO;
        // 先 SUM（保留正负号），最后再取绝对值
        // 订单笔数：按 statementId+orderId 去重，且该组有销售行无退款行才计入
        java.util.Map<String, int[]> orderFlags = new java.util.HashMap<>();
        for (TiktokSettlementOrder o : orders) {
            revenue = revenue.add(safe(o.getSubtotalAfterDiscount()));
            refundAfter = refundAfter.add(safe(o.getRefundAfterDiscount()));
            refundOfDisc = refundOfDisc.add(safe(o.getRefundOfSellerDiscount()));
            returnReimb = returnReimb.add(safe(o.getReturnShippingReimb()));
            referral = referral.add(safe(o.getReferralFee()));
            sellerShip = sellerShip.add(safe(o.getSellerShippingFee()));
            fbt = fbt.add(safe(o.getFbtFulfillmentFee()));
            refundAdmin = refundAdmin.add(safe(o.getRefundAdminFee()));
            returnShip = returnShip.add(safe(o.getActualReturnShippingFee()));
            // 按 statementId+orderId 分组判断销售/退款
            String key = o.getStatementId() + "|" + o.getOrderId();
            int[] flags = orderFlags.computeIfAbsent(key, k -> new int[]{0, 0});
            if (safe(o.getSubtotalAfterDiscount()).compareTo(BigDecimal.ZERO) > 0
                    || safe(o.getTotalRevenue()).compareTo(BigDecimal.ZERO) > 0) {
                flags[0] = 1;
            }
            if (safe(o.getRefundAfterDiscount()).compareTo(BigDecimal.ZERO) < 0
                    || safe(o.getTotalRevenue()).compareTo(BigDecimal.ZERO) < 0) {
                flags[1] = 1;
            }
        }
        int orderCount = (int) orderFlags.values().stream()
                .filter(f -> f[0] == 1 && f[1] == 0).count();

        // 公式：|SUM(x)| — 先求和再取绝对值
        MonthTaxSummary m = new MonthTaxSummary();
        m.setMonth(start.getYear() + "-" + String.format("%02d", start.getMonthValue()));
        m.setRevenueUsd(revenue);
        m.setRefundUsd(refundAfter.abs().subtract(refundOfDisc.abs()).add(returnReimb));
        m.setServiceFeeUsd(referral.abs().add(sellerShip.abs().max(fbt.abs())).add(refundAdmin.abs()).add(returnShip.abs()));
        m.setRevenueRmb(m.getRevenueUsd().multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP));
        m.setRefundRmb(m.getRefundUsd().multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP));
        m.setServiceFeeRmb(m.getServiceFeeUsd().multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP));
        m.setOrderCount(orderCount);
        return m;
    }

    private BigDecimal calcQuarterServiceFee(Long shopId, String siteCode, LocalDate start, LocalDate end) {
        List<TiktokSettlementOrder> orders = orderMapper.selectList(
                new LambdaQueryWrapper<TiktokSettlementOrder>()
                        .eq(TiktokSettlementOrder::getShopId, shopId)
                        .eq(TiktokSettlementOrder::getSiteCode, siteCode)
                        .eq(TiktokSettlementOrder::getType, "Order")
                        .ge(TiktokSettlementOrder::getStatementDate, start)
                        .le(TiktokSettlementOrder::getStatementDate, end));

        BigDecimal referral = BigDecimal.ZERO, sellerShip = BigDecimal.ZERO;
        BigDecimal fbt = BigDecimal.ZERO, refundAdmin = BigDecimal.ZERO, returnShip = BigDecimal.ZERO;
        for (TiktokSettlementOrder o : orders) {
            referral = referral.add(safe(o.getReferralFee()));
            sellerShip = sellerShip.add(safe(o.getSellerShippingFee()));
            fbt = fbt.add(safe(o.getFbtFulfillmentFee()));
            refundAdmin = refundAdmin.add(safe(o.getRefundAdminFee()));
            returnShip = returnShip.add(safe(o.getActualReturnShippingFee()));
        }
        return referral.abs().add(sellerShip.abs().max(fbt.abs())).add(refundAdmin.abs()).add(returnShip.abs());
    }

    private LocalDate[] parseQuarter(String quarter) {
        String[] parts = quarter.split("-Q");
        int year = Integer.parseInt(parts[0]);
        int q = Integer.parseInt(parts[1]);
        int startMonth = (q - 1) * 3 + 1;
        LocalDate start = LocalDate.of(year, startMonth, 1);
        LocalDate end = start.plusMonths(3).minusDays(1);
        return new LocalDate[]{start, end};
    }

    private BigDecimal safe(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }

    // ==================== VO ====================

    @Data
    public static class QuarterTaxSummary {
        private String quarter;
        private String siteCode;
        private BigDecimal exchangeRate;
        private List<MonthTaxSummary> months;
        private BigDecimal totalRevenueUsd;
        private BigDecimal totalRevenueRmb;
        private BigDecimal totalRefundUsd;
        private BigDecimal totalRefundRmb;
        private BigDecimal totalServiceFeeUsd;
        private BigDecimal totalServiceFeeRmb;
    }

    @Data
    public static class MonthTaxSummary {
        private String month;
        private BigDecimal revenueUsd;
        private BigDecimal revenueRmb;
        private BigDecimal refundUsd;
        private BigDecimal refundRmb;
        private BigDecimal serviceFeeUsd;
        private BigDecimal serviceFeeRmb;
        private int orderCount;
    }

    @Data
    public static class QuarterOperationSummary {
        private String quarter;
        private String siteCode;
        private BigDecimal exchangeRate;
        private BigDecimal netRevenue;
        private BigDecimal commission;
        private BigDecimal logistics;
        private BigDecimal affiliate;
        private BigDecimal promotion;
        private BigDecimal tax;
        private BigDecimal other;
        private BigDecimal orderProfit;
        private BigDecimal adjustmentIncome;
        private BigDecimal adjustmentExpense;
        private BigDecimal netProfit;
        private BigDecimal marginRate;
    }
}
