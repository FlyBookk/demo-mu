package com.musheng.business.document.generator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 结算数据输入类（纯函数输入，不依赖数据库实体）
 *
 * <p>用于 SettlementGenerator 的输入参数，包含结算周期和明细数据。
 * 与数据库实体解耦，确保生成器为纯函数。</p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementInput {

    /** 结算周期起始日 */
    private LocalDate periodStart;

    /** 结算周期结束日 */
    private LocalDate periodEnd;

    /** 前端选择的站点列表（按传入顺序生成结算单，序号从 001 开始递增） */
    private List<String> selectedSiteCodes;

    /**
     * 站点→货币映射（来自 t_marketplace 动态配置）
     * key 为站点代码（如 "US"），value 为货币代码（如 "USD"）
     */
    private Map<String, String> siteCurrencyMap;

    /** 结算数据明细列表 */
    private List<SettlementDataItem> items;

    /**
     * 结算数据明细项
     *
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SettlementDataItem {

        /** 交易日期（用于按月拆分结算周期） */
        private LocalDate transactionDate;

        /** 站点代码（如 "US", "CA", "UK", "EU"） */
        private String siteCode;

        /** MSKU编码 */
        private String msku;

        /** 货币代码（如 "USD", "CAD", "GBP", "EUR"） */
        private String currency;

        /** 单价 */
        private BigDecimal unitPrice;

        /** 销售数量 */
        private Integer quantity;
    }
}
