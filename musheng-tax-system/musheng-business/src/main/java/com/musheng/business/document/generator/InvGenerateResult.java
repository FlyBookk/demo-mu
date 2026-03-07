package com.musheng.business.document.generator;

import com.musheng.business.document.entity.DocumentInv;
import com.musheng.business.document.entity.DocumentInvItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * INV发票生成结果包装类
 *
 * <p>包含INV主表数据和明细列表，由 InvGenerator 生成。</p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvGenerateResult {

    /** INV主表数据 */
    private DocumentInv inv;

    /** INV明细列表 */
    private List<DocumentInvItem> items;
}
