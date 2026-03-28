package com.musheng.business.common.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.musheng.common.exception.BusinessException;
import com.musheng.config.marketplace.entity.Marketplace;
import com.musheng.config.marketplace.mapper.MarketplaceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.musheng.common.result.ErrorCode.PARAM_ERROR;

/**
 * 站点动态配置服务 — 所有站点/货币映射的统一入口
 *
 * <p>全部映射数据来自 t_marketplace 表，禁止在业务代码中硬编码站点或货币信息。
 * 查询结果不存在时直接抛出异常，提示用户前往站点管理添加配置。</p>
 */
@Service
@RequiredArgsConstructor
public class MarketplaceConfigService {

    private final MarketplaceMapper marketplaceMapper;

    private static final String CONFIG_HINT = "，请前往【站点管理】添加或启用相关站点配置";

    // ──────────────────────────── 基础查询 ────────────────────────────

    /**
     * 加载所有启用的站点（status=1）
     */
    public List<Marketplace> getEnabled() {
        LambdaQueryWrapper<Marketplace> w = new LambdaQueryWrapper<>();
        w.eq(Marketplace::getStatus, 1);
        return marketplaceMapper.selectList(w);
    }

    /**
     * 获取所有启用的站点编码列表（如 ["US","UK","JP"]）
     */
    public List<String> getEnabledSiteCodes() {
        return getEnabled().stream()
                .map(Marketplace::getSiteCode)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 获取所有启用的货币代码集合（如 {"USD","GBP","JPY"}）
     */
    public Set<String> getEnabledCurrencies() {
        return getEnabled().stream()
                .map(Marketplace::getCurrencyCode)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
    }

    // ──────────────────────────── 站点编码 → 货币 ────────────────────────────

    /**
     * 根据站点编码获取货币代码，找不到则抛异常提示配置
     *
     * @param siteCode 站点编码，如 "US"、"UK"、"JP"
     * @return 货币代码，如 "USD"、"GBP"、"JPY"
     */
    public String getCurrencyBySiteCode(String siteCode) {
        Marketplace mp = findBySiteCode(siteCode);
        if (mp == null || !StringUtils.hasText(mp.getCurrencyCode())) {
            throw new BusinessException(PARAM_ERROR,
                    "站点 [" + siteCode + "] 未配置货币信息" + CONFIG_HINT);
        }
        return mp.getCurrencyCode();
    }

    // ──────────────────────────── Marketplace 域名 → 站点编码 ────────────────────────────

    /**
     * 根据 Amazon marketplace 域名获取站点编码，找不到则抛异常
     *
     * @param domain Amazon 域名，如 "amazon.co.uk"、"amazon.co.jp"
     * @return 站点编码，如 "UK"、"JP"
     */
    public String getSiteCodeByDomain(String domain) {
        if (!StringUtils.hasText(domain)) {
            throw new BusinessException(PARAM_ERROR, "marketplace 域名不能为空");
        }
        String normalized = normalizeDomain(domain);
        LambdaQueryWrapper<Marketplace> w = new LambdaQueryWrapper<>();
        w.eq(Marketplace::getStatus, 1).eq(Marketplace::getMarketplaceId, normalized);
        Marketplace mp = marketplaceMapper.selectOne(w);
        if (mp == null) {
            // 模糊匹配兜底（处理 "www.amazon.co.uk" 之类的变体）
            mp = getEnabled().stream()
                    .filter(m -> StringUtils.hasText(m.getMarketplaceId())
                            && (normalized.contains(m.getMarketplaceId())
                            || m.getMarketplaceId().contains(normalized)))
                    .findFirst().orElse(null);
        }
        if (mp == null) {
            throw new BusinessException(PARAM_ERROR,
                    "Marketplace 域名 [" + domain + "] 未在系统中配置" + CONFIG_HINT);
        }
        return mp.getSiteCode();
    }

    /**
     * 根据站点编码获取 marketplace 域名，找不到则抛异常
     *
     * @param siteCode 站点编码，如 "UK"
     * @return marketplace 域名，如 "amazon.co.uk"
     */
    public String getDomainBySiteCode(String siteCode) {
        Marketplace mp = findBySiteCode(siteCode);
        if (mp == null || !StringUtils.hasText(mp.getMarketplaceId())) {
            throw new BusinessException(PARAM_ERROR,
                    "站点 [" + siteCode + "] 未配置 Marketplace 域名" + CONFIG_HINT);
        }
        return mp.getMarketplaceId();
    }

    // ──────────────────────────── 货币校验 ────────────────────────────

    /**
     * 校验货币代码是否在已配置的站点中，不合法则抛异常
     *
     * @param currency 货币代码，如 "USD"
     */
    public void validateCurrency(String currency) {
        Set<String> valid = getEnabledCurrencies();
        if (!valid.contains(currency)) {
            throw new BusinessException(PARAM_ERROR,
                    "不支持的币种 [" + currency + "]，已配置的币种为: " + valid + CONFIG_HINT);
        }
    }

    // ──────────────────────────── 店铺名称推断站点 ────────────────────────────

    /**
     * 从店铺名称推断站点编码（匹配 siteCode 或 siteName 关键字）
     *
     * <p>例：店铺名包含 "UK" 或 "英国" → 返回 "UK"</p>
     *
     * @param storeName 店铺名称
     * @return 匹配的站点编码，未匹配返回 null（不抛异常）
     */
    public String inferSiteCodeFromStoreName(String storeName) {
        if (!StringUtils.hasText(storeName)) return null;
        String upper = storeName.toUpperCase();
        for (Marketplace mp : getEnabled()) {
            if (StringUtils.hasText(mp.getSiteCode())
                    && upper.contains(mp.getSiteCode().toUpperCase())) {
                return mp.getSiteCode();
            }
            if (StringUtils.hasText(mp.getSiteName())
                    && storeName.contains(mp.getSiteName())) {
                return mp.getSiteCode();
            }
        }
        return null;
    }

    // ──────────────────────────── 内部工具 ────────────────────────────

    private Marketplace findBySiteCode(String siteCode) {
        if (!StringUtils.hasText(siteCode)) return null;
        LambdaQueryWrapper<Marketplace> w = new LambdaQueryWrapper<>();
        w.eq(Marketplace::getStatus, 1).eq(Marketplace::getSiteCode, siteCode);
        return marketplaceMapper.selectOne(w);
    }

    private static String normalizeDomain(String domain) {
        if (domain == null) return "";
        return domain.toLowerCase().trim()
                .replaceAll("^https?://", "")
                .replaceAll("/.*$", "");
    }

    /**
     * 构建域名→站点编码的映射表（供 CSV 解析等批量场景使用）
     */
    public Map<String, String> buildDomainToSiteCodeMap() {
        Map<String, String> map = new LinkedHashMap<>();
        for (Marketplace mp : getEnabled()) {
            if (StringUtils.hasText(mp.getMarketplaceId()) && StringUtils.hasText(mp.getSiteCode())) {
                map.put(mp.getMarketplaceId().toLowerCase(), mp.getSiteCode());
            }
        }
        return Collections.unmodifiableMap(map);
    }

    /**
     * 构建站点编码→货币的映射表（供批量场景使用）
     */
    public Map<String, String> buildSiteCurrencyMap() {
        Map<String, String> map = new LinkedHashMap<>();
        for (Marketplace mp : getEnabled()) {
            if (StringUtils.hasText(mp.getSiteCode()) && StringUtils.hasText(mp.getCurrencyCode())) {
                map.put(mp.getSiteCode(), mp.getCurrencyCode());
            }
        }
        return Collections.unmodifiableMap(map);
    }
}