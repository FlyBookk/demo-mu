package com.musheng.business.document.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.musheng.business.document.dto.DocumentPartyConfigDTO;
import com.musheng.business.document.entity.DocumentPartyConfig;
import com.musheng.business.document.mapper.DocumentPartyConfigMapper;
import com.musheng.business.document.service.DocumentPartyConfigService;
import com.musheng.business.document.vo.DocumentPartyConfigVO;
import com.musheng.common.context.ShopContext;
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
     * 查询当前店铺的配置列表
     *
     * @return 配置 VO 列表
     * @author wanhua
     * 10:30 2026年03月22日
     */
    @Override
    public List<DocumentPartyConfigVO> list() {
        Long shopId = ShopContext.requireShopId();
        List<DocumentPartyConfig> configs = documentPartyConfigMapper.selectList(
                new LambdaQueryWrapper<DocumentPartyConfig>()
                        .eq(DocumentPartyConfig::getShopId, shopId)
                        .orderByAsc(DocumentPartyConfig::getSiteCode));
        return configs.stream()
                .map(c -> BeanUtil.toBean(c, DocumentPartyConfigVO.class))
                .collect(Collectors.toList());
    }

    /**
     * 新增交易方配置
     *
     * <p>同一店铺下 siteCode 唯一，校验通过后持久化。</p>
     *
     * @param dto 配置请求参数
     * @author wanhua
     * 10:30 2026年03月22日
     */
    @Override
    public void add(DocumentPartyConfigDTO dto) {
        Long shopId = ShopContext.requireShopId();
        // 校验同一店铺下 siteCode 唯一性
        DocumentPartyConfig existing = documentPartyConfigMapper.selectOne(
                new LambdaQueryWrapper<DocumentPartyConfig>()
                        .eq(DocumentPartyConfig::getShopId, shopId)
                        .eq(DocumentPartyConfig::getSiteCode, dto.getSiteCode()));
        if (existing != null) {
            throw new BusinessException(ErrorCode.DATA_ALREADY_EXIST, "该站点配置已存在");
        }
        DocumentPartyConfig config = BeanUtil.toBean(dto, DocumentPartyConfig.class);
        config.setShopId(shopId);
        documentPartyConfigMapper.insert(config);
        log.info("新增交易方配置成功，shopId={}, siteCode={}", shopId, dto.getSiteCode());
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
        Long shopId = ShopContext.requireShopId();
        DocumentPartyConfig existing = documentPartyConfigMapper.selectOne(
                new LambdaQueryWrapper<DocumentPartyConfig>()
                        .eq(DocumentPartyConfig::getId, dto.getId())
                        .eq(DocumentPartyConfig::getShopId, shopId));
        if (existing == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST, "配置不存在");
        }
        BeanUtil.copyProperties(dto, existing, "id", "shopId", "createTime");
        documentPartyConfigMapper.updateById(existing);
        log.info("修改交易方配置成功，shopId={}, id={}, siteCode={}", shopId, dto.getId(), dto.getSiteCode());
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
        Long shopId = ShopContext.requireShopId();
        DocumentPartyConfig existing = documentPartyConfigMapper.selectOne(
                new LambdaQueryWrapper<DocumentPartyConfig>()
                        .eq(DocumentPartyConfig::getId, id)
                        .eq(DocumentPartyConfig::getShopId, shopId));
        if (existing == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST, "配置不存在");
        }
        documentPartyConfigMapper.deleteById(id);
        log.info("删除交易方配置成功，shopId={}, id={}", shopId, id);
    }

    /**
     * 按站点代码查询当前店铺的配置，不存在时抛出业务异常
     *
     * @param siteCode 站点代码（US/CA/UK/EU）
     * @return 配置实体
     * @author wanhua
     * 10:30 2026年03月22日
     */
    @Override
    public DocumentPartyConfig getBySiteCode(String siteCode) {
        Long shopId = ShopContext.requireShopId();
        DocumentPartyConfig config = documentPartyConfigMapper.selectOne(
                new LambdaQueryWrapper<DocumentPartyConfig>()
                        .eq(DocumentPartyConfig::getShopId, shopId)
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
        Long shopId = ShopContext.requireShopId();
        // 查询来源配置（必须属于当前店铺）
        DocumentPartyConfig source = documentPartyConfigMapper.selectOne(
                new LambdaQueryWrapper<DocumentPartyConfig>()
                        .eq(DocumentPartyConfig::getId, sourceId)
                        .eq(DocumentPartyConfig::getShopId, shopId));
        if (source == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST, "来源配置不存在");
        }

        // 查询目标站点是否已存在
        DocumentPartyConfig target = documentPartyConfigMapper.selectOne(
                new LambdaQueryWrapper<DocumentPartyConfig>()
                        .eq(DocumentPartyConfig::getShopId, shopId)
                        .eq(DocumentPartyConfig::getSiteCode, targetSiteCode));

        if (target != null) {
            // 目标站点已存在，覆盖更新（保留 id、shopId 和 createTime）
            BeanUtil.copyProperties(source, target, "id", "shopId", "createTime", "siteCode");
            documentPartyConfigMapper.updateById(target);
            log.info("复制交易方配置：覆盖更新目标站点，shopId={}, source={}, target={}", shopId, source.getSiteCode(), targetSiteCode);
        } else {
            // 目标站点不存在，新增
            DocumentPartyConfig newConfig = BeanUtil.toBean(source, DocumentPartyConfig.class);
            newConfig.setId(null);
            newConfig.setSiteCode(targetSiteCode);
            documentPartyConfigMapper.insert(newConfig);
            log.info("复制交易方配置：新增目标站点，shopId={}, source={}, target={}", shopId, source.getSiteCode(), targetSiteCode);
        }
    }
}
