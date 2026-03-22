package com.musheng.business.document.service;

import com.musheng.business.document.dto.DocumentPartyConfigDTO;
import com.musheng.business.document.entity.DocumentPartyConfig;
import com.musheng.business.document.vo.DocumentPartyConfigVO;

import java.util.List;

/**
 * FBA单据交易方配置服务接口
 *
 * @author wanhua
 * 10:30 2026年03月22日
 */
public interface DocumentPartyConfigService {

    /**
     * 查询所有未删除的配置列表
     *
     * @return 配置列表
     * @author wanhua
     * 10:30 2026年03月22日
     */
    List<DocumentPartyConfigVO> list();

    /**
     * 新增交易方配置
     *
     * @param dto 配置请求参数
     * @author wanhua
     * 10:30 2026年03月22日
     */
    void add(DocumentPartyConfigDTO dto);

    /**
     * 修改交易方配置
     *
     * @param dto 配置请求参数（id必填）
     * @author wanhua
     * 10:30 2026年03月22日
     */
    void update(DocumentPartyConfigDTO dto);

    /**
     * 逻辑删除交易方配置
     *
     * @param id 配置ID
     * @author wanhua
     * 10:30 2026年03月22日
     */
    void delete(Long id);

    /**
     * 按站点代码查询配置
     *
     * @param siteCode 站点代码（US/CA/UK/EU）
     * @return 配置实体，不存在时抛出业务异常
     * @author wanhua
     * 10:30 2026年03月22日
     */
    DocumentPartyConfig getBySiteCode(String siteCode);

    /**
     * 复制配置到目标站点（目标站点已存在则覆盖，不存在则新增）
     *
     * @param sourceId       来源配置ID
     * @param targetSiteCode 目标站点代码
     * @author wanhua
     * 10:30 2026年03月22日
     */
    void copy(Long sourceId, String targetSiteCode);
}
