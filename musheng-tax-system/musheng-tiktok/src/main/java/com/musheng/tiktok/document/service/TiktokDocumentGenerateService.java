package com.musheng.tiktok.document.service;

import com.musheng.tiktok.document.entity.TiktokDocumentDn;
import com.musheng.tiktok.document.entity.TiktokDocumentInv;
import com.musheng.tiktok.document.entity.TiktokDocumentPo;
import com.musheng.tiktok.document.entity.TiktokDocumentSettlement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * TK单据生成服务（对齐亚马逊多份生成逻辑）
 *
 * @author wanhua
 * 19:54 2026年05月15日
 */
public interface TiktokDocumentGenerateService {

    /**
     * 生成PO（按货件创建时间分组，同PO日期合并，可能生成多份）
     *
     * @param siteCode 站点代码
     * @param shipmentIds 选中的货件ID列表
     * @return 生成的PO列表
     */
    List<TiktokDocumentPo> generatePo(String siteCode, List<String> shipmentIds);

    /**
     * 生成DN（按锚点+21天周期分组，可能生成多份）
     *
     * @param siteCode 站点代码
     * @param shipmentIds 选中的货件ID列表（与poId二选一）
     * @param anchorDate 锚点日期
     * @param poId 关联PO主键（传入时自动从PO明细提取货件）
     * @return 生成的DN列表
     */
    List<TiktokDocumentDn> generateDn(String siteCode, List<String> shipmentIds, LocalDate anchorDate, Long poId);

    /**
     * 生成Settlement+INV（按月拆分为3份，每份对应一份INV）
     *
     * @param siteCode 站点代码
     * @param quarter 季度（格式: 2025-Q3）
     * @param costAmount 采购成本（原币）
     * @return 生成的结算单列表（每份已自动生成对应INV）
     */
    List<TiktokDocumentSettlement> generateSettlement(String siteCode, String quarter, BigDecimal costAmount);

    /**
     * 根据结算单生成INV
     */
    TiktokDocumentInv generateInv(Long settlementId);
}
