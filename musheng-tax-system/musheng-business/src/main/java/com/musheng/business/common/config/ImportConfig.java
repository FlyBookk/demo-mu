package com.musheng.business.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 导入功能配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.import")
public class ImportConfig {

    /**
     * 是否自动创建不存在的站点（默认关闭）
     * 当导入数据中的 siteCode 在 t_marketplace 表中不存在时，自动创建
     */
    private boolean autoCreateMarketplace = false;

    /**
     * 是否自动创建不存在的货币（默认关闭）
     * 当导入数据中的 currencyCode 在 t_currency 表中不存在时，自动创建
     */
    private boolean autoCreateCurrency = false;
}
