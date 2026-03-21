package com.musheng.business.settlement.derivation.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 供应商结差退款明细
 *
 * <p>记录供应商供货范围内的退款数量、金额和使用的汇率信息，
 * 用于供货协议第 3.1 条规定的退货退款扣除计算。</p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierRefundDetail {

    /**
     * 退款数量（正数）
     */
    private Integer quantity;

    /**
     * 退款金额（原币，正数）
     */
    private BigDecimal amount;

    /**
     * 使用的汇率
     */
    private BigDecimal exchangeRate;
}
