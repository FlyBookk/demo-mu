package com.musheng.tiktok.document.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.musheng.common.context.ShopContext;
import com.musheng.tiktok.document.entity.TiktokPartyConfig;
import com.musheng.tiktok.document.mapper.TiktokPartyConfigMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * TK交易方配置服务
 *
 * @author wanhua
 * 21:12 2026年05月14日
 */
@Service
@Slf4j
public class TiktokPartyConfigService {

    @Autowired
    private TiktokPartyConfigMapper partyConfigMapper;

    /**
     * 查询当前店铺所有站点配置
     */
    public List<TiktokPartyConfig> listAll() {
        Long shopId = ShopContext.requireShopId();
        return partyConfigMapper.selectList(new LambdaQueryWrapper<TiktokPartyConfig>()
                .eq(TiktokPartyConfig::getShopId, shopId)
                .orderByAsc(TiktokPartyConfig::getSiteCode));
    }

    /**
     * 按站点获取配置
     */
    public TiktokPartyConfig getBySiteCode(String siteCode) {
        Long shopId = ShopContext.requireShopId();
        return partyConfigMapper.selectOne(new LambdaQueryWrapper<TiktokPartyConfig>()
                .eq(TiktokPartyConfig::getShopId, shopId)
                .eq(TiktokPartyConfig::getSiteCode, siteCode));
    }

    /**
     * 保存或更新配置（按 shop_id + site_code 唯一）
     */
    public TiktokPartyConfig saveOrUpdate(TiktokPartyConfig config) {
        Long shopId = ShopContext.requireShopId();
        config.setShopId(shopId);

        TiktokPartyConfig existing = partyConfigMapper.selectOne(new LambdaQueryWrapper<TiktokPartyConfig>()
                .eq(TiktokPartyConfig::getShopId, shopId)
                .eq(TiktokPartyConfig::getSiteCode, config.getSiteCode()));

        if (existing != null) {
            config.setId(existing.getId());
            partyConfigMapper.updateById(config);
        } else {
            partyConfigMapper.insert(config);
        }
        return config;
    }

    /**
     * 删除配置
     */
    public void delete(Long id) {
        partyConfigMapper.deleteById(id);
    }
}
