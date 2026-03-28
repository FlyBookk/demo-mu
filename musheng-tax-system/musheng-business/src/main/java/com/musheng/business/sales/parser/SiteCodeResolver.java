package com.musheng.business.sales.parser;

import com.musheng.business.common.config.MarketplaceConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * 站点编码解析器（动态版）
 *
 * <p>所有映射关系来自 t_marketplace，不再硬编码。
 * 解析到未配置的站点/域名时抛出异常，提示用户前往站点管理配置。</p>
 */
@Service
@RequiredArgsConstructor
public class SiteCodeResolver {

    private final MarketplaceConfigService marketplaceConfigService;

    /**
     * 根据 marketplace 域名获取站点编码
     *
     * @param marketplace 域名，如 "amazon.co.uk"
     * @return 站点编码，如 "UK"；domain 为空时返回 null
     */
    public String getSiteCode(String marketplace) {
        if (!StringUtils.hasText(marketplace)) return null;
        return marketplaceConfigService.getSiteCodeByDomain(marketplace);
    }

    /**
     * 根据站点编码获取货币代码
     *
     * @param siteCode 站点编码，如 "UK"
     * @return 货币代码，如 "GBP"；siteCode 为空时返回 null
     */
    public String getCurrencyCode(String siteCode) {
        if (!StringUtils.hasText(siteCode)) return null;
        return marketplaceConfigService.getCurrencyBySiteCode(siteCode);
    }

    /**
     * 检查站点是否使用德语表头（siteCode = "DE"）
     */
    public boolean isGermanSite(String siteCode) {
        return "DE".equalsIgnoreCase(siteCode);
    }

    /**
     * 检查是否为欧洲站点（货币为 EUR）
     */
    public boolean isEuropeanSite(String siteCode) {
        if (!StringUtils.hasText(siteCode)) return false;
        try {
            String currency = marketplaceConfigService.getCurrencyBySiteCode(siteCode);
            return "EUR".equals(currency);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 从数据行中检测站点编码（用于 CSV 逐行扫描）
     *
     * @param line CSV 行内容
     * @return 站点编码，未检测到返回 null
     */
    public String detectSiteFromLine(String line) {
        if (!StringUtils.hasText(line)) return null;
        String lowerLine = line.toLowerCase();
        Map<String, String> domainMap = marketplaceConfigService.buildDomainToSiteCodeMap();
        for (Map.Entry<String, String> entry : domainMap.entrySet()) {
            if (lowerLine.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }
}