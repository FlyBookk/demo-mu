package com.musheng.business.document.generator;

import com.musheng.business.document.entity.DocumentDn;
import com.musheng.business.document.entity.DocumentDnItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DN送货单生成结果包装类
 *
 * <p>包含DN主表数据和明细列表，由 DnGenerator 生成。</p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DnGenerateResult {

    /** DN主表数据 */
    private DocumentDn dn;

    /** DN明细列表 */
    private List<DocumentDnItem> items;
}
