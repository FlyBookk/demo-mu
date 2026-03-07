package com.musheng.business.document.service;

import com.musheng.business.document.dto.DnGenerateRequest;
import com.musheng.business.document.dto.PoGenerateRequest;
import com.musheng.business.document.dto.SettlementGenerateRequest;
import com.musheng.business.document.entity.DocumentDn;
import com.musheng.business.document.entity.DocumentInv;
import com.musheng.business.document.entity.DocumentPo;
import com.musheng.business.document.entity.DocumentSettlement;

import java.util.List;

/**
 * 单据生成服务接口
 *
 * <p>负责调用各生成器并将结果持久化到数据库。</p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
public interface DocumentGenerateService {

    /**
     * 根据选定的FBA货件生成PO采购订单
     *
     * @param request PO生成请求（包含货件ID列表）
     * @return 持久化后的PO实体，无数据时返回 null
     * @author wanhua
     * 10:30 2026年01月29日
     */
    DocumentPo generatePo(PoGenerateRequest request);

    /**
     * 根据DN周期批量生成送货单
     *
     * @param request DN生成请求（包含锚点日期和货件ID列表）
     * @return 持久化后的DN实体，无数据时返回 null
     * @author wanhua
     * 10:30 2026年01月29日
     */
    DocumentDn generateDn(DnGenerateRequest request);

    /**
     * 根据结算周期生成4份结算单（按站点拆分）
     *
     * @param request 结算单生成请求（包含周期起止日期）
     * @return 持久化后的结算单列表（通常4份）
     * @author wanhua
     * 10:30 2026年01月29日
     */
    List<DocumentSettlement> generateSettlements(SettlementGenerateRequest request);

    /**
     * 根据结算单自动生成对应的4份INV
     *
     * @param settlementIds 结算单ID列表
     * @return 持久化后的INV列表
     * @author wanhua
     * 10:30 2026年01月29日
     */
    List<DocumentInv> generateInvoices(List<Long> settlementIds);
}
