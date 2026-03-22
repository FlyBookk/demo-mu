package com.musheng.business.document.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.musheng.business.document.dto.DocumentPartyConfigDTO;
import com.musheng.business.document.entity.DocumentPartyConfig;
import com.musheng.business.document.mapper.DocumentPartyConfigMapper;
import com.musheng.business.document.service.DocumentPartyConfigService;
import com.musheng.business.document.vo.DocumentPartyConfigVO;
import com.musheng.common.exception.BusinessException;
import com.musheng.common.result.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * FBA单据交易方配置服务实现类
 *
 * @author wanhua
 * 10:30 2026年03月22日
 */
@Service
@Slf4j
public class DocumentPartyConfigServiceImpl implements DocumentPartyConfigService {

    @Autowired
    private DocumentPartyConfigMapper documentPartyConfigMapper;

    /**
     * 查询所有未删除的配置列表
     *
     * @return 配置 VO 列表
     * @author wanhua
     * 10:30 2026年03月22日
     */
    @Override
    public List<DocumentPartyConfigVO> list() {
        List<DocumentPartyConfig> configs = documentPartyConfigMapper.selectList(
                new LambdaQueryWrapper<DocumentPartyConfig>()
                        .orderByAsc(DocumentPartyConfig::getSiteCode));
        return configs.stream()
                .map(c -> BeanUtil.toBean(c, DocumentPartyConfigVO.class))
                .collect(Collectors.toList());
    }

    /**
     * 新增交易方配置
     *
     * <p>校验 siteCode 唯一性，通过后持久化。</p>
     *
     * @param dto 配置请求参数
     * @author wanhua
     * 10:30 2026年03月22日
     */
    @Override
    public void add(DocumentPartyConfigDTO dto) {
        // 校验 siteCode 唯一性
        DocumentPartyConfig existing = documentPartyConfigMapper.selectOne(
                new LambdaQueryWrapper<DocumentPartyConfig>()
                        .eq(DocumentPartyConfig::getSiteCode, dto.getSiteCode()));
        if (existing != null) {
            throw new BusinessException(ErrorCode.DATA_ALREADY_EXIST, "该站点配置已存在");
        }
        DocumentPartyConfig config = BeanUtil.toBean(dto, DocumentPartyConfig.class);
        documentPartyConfigMapper.insert(config);
        log.info("新增交易方配置成功，siteCode={}", dto.getSiteCode());
    }

    /**
     * 修改交易方配置
     *
     * @param dto 配置请求参数（id必填）
     * @author wanhua
     * 10:30 2026年03月22日
     */
    @Override
    public void update(DocumentPartyConfigDTO dto) {
        DocumentPartyConfig existing = documentPartyConfigMapper.selectById(dto.getId());
        if (existing == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST, "配置不存在");
        }
        BeanUtil.copyProperties(dto, existing, "id", "createTime");
        documentPartyConfigMapper.updateById(existing);
        log.info("修改交易方配置成功，id={}, siteCode={}", dto.getId(), dto.getSiteCode());
    }

    /**
     * 逻辑删除交易方配置
     *
     * @param id 配置ID
     * @author wanhua
     * 10:30 2026年03月22日
     */
    @Override
    public void delete(Long id) {
        DocumentPartyConfig existing = documentPartyConfigMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST, "配置不存在");
        }
        documentPartyConfigMapper.deleteById(id);
        log.info("删除交易方配置成功，id={}", id);
    }

    /**
     * 按站点代码查询配置，不存在时抛出业务异常
     *
     * @param siteCode 站点代码（US/CA/UK/EU）
     * @return 配置实体
     * @author wanhua
     * 10:30 2026年03月22日
     */
    @Override
    public DocumentPartyConfig getBySiteCode(String siteCode) {
        DocumentPartyConfig config = documentPartyConfigMapper.selectOne(
                new LambdaQueryWrapper<DocumentPartyConfig>()
                        .eq(DocumentPartyConfig::getSiteCode, siteCode));
        if (config == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST,
                    "站点 " + siteCode + " 的交易方配置不存在，请先完成配置");
        }
        return config;
    }

    /**
     * 复制配置到目标站点（目标站点已存在则覆盖，不存在则新增）
     *
     * @param sourceId       来源配置ID
     * @param targetSiteCode 目标站点代码
     * @author wanhua
     * 10:30 2026年03月22日
     */
    @Override
    public void copy(Long sourceId, String targetSiteCode) {
        // 查询来源配置
        DocumentPartyConfig source = documentPartyConfigMapper.selectById(sourceId);
        if (source == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST, "来源配置不存在");
        }

        // 查询目标站点是否已存在
        DocumentPartyConfig target = documentPartyConfigMapper.selectOne(
                new LambdaQueryWrapper<DocumentPartyConfig>()
                        .eq(DocumentPartyConfig::getSiteCode, targetSiteCode));

        if (target != null) {
            // 目标站点已存在，覆盖更新（保留 id 和 createTime）
            BeanUtil.copyProperties(source, target, "id", "createTime", "siteCode");
            documentPartyConfigMapper.updateById(target);
            log.info("复制交易方配置：覆盖更新目标站点，source={}, target={}", source.getSiteCode(), targetSiteCode);
        } else {
            // 目标站点不存在，新增
            DocumentPartyConfig newConfig = BeanUtil.toBean(source, DocumentPartyConfig.class);
            newConfig.setId(null);
            newConfig.setSiteCode(targetSiteCode);
            documentPartyConfigMapper.insert(newConfig);
            log.info("复制交易方配置：新增目标站点，source={}, target={}", source.getSiteCode(), targetSiteCode);
        }
    }
}
