package com.musheng.business.document.generator;

import com.musheng.business.document.entity.DocumentSettlement;
import com.musheng.business.document.entity.DocumentSettlementItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 结算单生成结果包装类
 *
 * <p>包含结算单主表数据和明细列表，由 SettlementGenerator 生成。</p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementGenerateResult {

    /** 结算单主表数据 */
    private DocumentSettlement settlement;

    /** 结算单明细列表 */
    private List<DocumentSettlementItem> items;
}
