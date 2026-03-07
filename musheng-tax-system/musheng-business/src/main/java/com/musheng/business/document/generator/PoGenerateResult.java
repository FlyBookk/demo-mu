package com.musheng.business.document.generator;

import com.musheng.business.document.entity.DocumentPo;
import com.musheng.business.document.entity.DocumentPoItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * PO生成结果包装类
 *
 * <p>包含PO主表数据和明细列表，由 PoGenerator 生成。</p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PoGenerateResult {

    /** PO主表数据 */
    private DocumentPo po;

    /** PO明细列表 */
    private List<DocumentPoItem> items;
}
