-- ============================================================
-- 站点数据同步脚本
-- 目的：将 SiteCode 枚举中的域名信息同步到 t_marketplace 表
-- 
-- 背景：
--   SiteCode 枚举（历史遗留）中 marketplaceId 存的是域名（amazon.com 等），
--   数据库 t_marketplace.marketplace_id 存的是 SP-API ID（ATVPDKIKX0DER 等），
--   代码中 MarketplaceConfigService 有 getSiteCodeByDomain / buildDomainToSiteCodeMap
--   等方法需要域名做匹配，但表里没有域名字段。
--   需要新增 domain 字段，把枚举里的域名同步过来，并补全其余站点的域名。
--
-- 执行顺序：
--   1. DDL - 新增 domain 字段
--   2. DML - 同步枚举中 4 个站点的域名
--   3. DML - 补全其余站点的域名
--   4. DDL - 更新 marketplace_id 字段注释
--
-- 安全说明：可重复执行
-- ============================================================

-- ============================================================
-- 第一部分：DDL - 新增 domain 字段
-- 存放 Amazon 站点域名，用于 CSV 解析时的站点匹配
-- ============================================================

ALTER TABLE `t_marketplace`
  ADD COLUMN `domain` varchar(100) DEFAULT NULL COMMENT 'Amazon站点域名(amazon.com等)'
  AFTER `marketplace_id`;

-- 添加索引，支持域名查询
ALTER TABLE `t_marketplace`
  ADD KEY `idx_domain` (`domain`);


-- ============================================================
-- 第二部分：DML - 同步 SiteCode 枚举中 4 个站点的域名
-- 来源：SiteCode.java 中的 marketplaceId 字段
-- ============================================================

UPDATE `t_marketplace` SET `domain` = 'amazon.com'    WHERE `site_code` = 'US';
UPDATE `t_marketplace` SET `domain` = 'amazon.ca'     WHERE `site_code` = 'CA';
UPDATE `t_marketplace` SET `domain` = 'amazon.co.uk'  WHERE `site_code` = 'UK';
UPDATE `t_marketplace` SET `domain` = 'amazon.de'     WHERE `site_code` = 'DE';


-- ============================================================
-- 第三部分：DML - 补全其余站点的域名
-- 来源：Amazon 各站点官方域名
-- ============================================================

-- 北美
UPDATE `t_marketplace` SET `domain` = 'amazon.com.mx' WHERE `site_code` = 'MX';
UPDATE `t_marketplace` SET `domain` = 'amazon.com.br' WHERE `site_code` = 'BR';

-- 欧洲
UPDATE `t_marketplace` SET `domain` = 'amazon.fr'     WHERE `site_code` = 'FR';
UPDATE `t_marketplace` SET `domain` = 'amazon.it'     WHERE `site_code` = 'IT';
UPDATE `t_marketplace` SET `domain` = 'amazon.es'     WHERE `site_code` = 'ES';
UPDATE `t_marketplace` SET `domain` = 'amazon.nl'     WHERE `site_code` = 'NL';
UPDATE `t_marketplace` SET `domain` = 'amazon.com.be' WHERE `site_code` = 'BE';
UPDATE `t_marketplace` SET `domain` = 'amazon.ie'     WHERE `site_code` = 'IE';
UPDATE `t_marketplace` SET `domain` = 'amazon.se'     WHERE `site_code` = 'SE';
UPDATE `t_marketplace` SET `domain` = 'amazon.pl'     WHERE `site_code` = 'PL';

-- 中东与非洲
UPDATE `t_marketplace` SET `domain` = 'amazon.com.tr' WHERE `site_code` = 'TR';
UPDATE `t_marketplace` SET `domain` = 'amazon.eg'     WHERE `site_code` = 'EG';
UPDATE `t_marketplace` SET `domain` = 'amazon.sa'     WHERE `site_code` = 'SA';
UPDATE `t_marketplace` SET `domain` = 'amazon.ae'     WHERE `site_code` = 'AE';
UPDATE `t_marketplace` SET `domain` = 'amazon.co.za'  WHERE `site_code` = 'ZA';
UPDATE `t_marketplace` SET `domain` = 'amazon.in'     WHERE `site_code` = 'IN';

-- 亚太
UPDATE `t_marketplace` SET `domain` = 'amazon.co.jp'  WHERE `site_code` = 'JP';
UPDATE `t_marketplace` SET `domain` = 'amazon.com.au' WHERE `site_code` = 'AU';
UPDATE `t_marketplace` SET `domain` = 'amazon.sg'     WHERE `site_code` = 'SG';


-- ============================================================
-- 第四部分：DDL - 更新 marketplace_id 字段注释
-- 明确该字段存的是 SP-API Marketplace ID，不是域名
-- ============================================================

ALTER TABLE `t_marketplace`
  MODIFY COLUMN `marketplace_id` varchar(50) NOT NULL COMMENT 'SP-API Marketplace ID(如ATVPDKIKX0DER)';


-- ============================================================
-- 执行后验证
-- ============================================================
-- SELECT site_code, site_name, marketplace_id, domain, currency_code, status
-- FROM t_marketplace
-- ORDER BY FIELD(status, 1, 0), site_code;
--
-- 预期：23 条数据都有 domain 值
--
-- 后续代码改动提示：
--   MarketplaceConfigService 中 getSiteCodeByDomain / getDomainBySiteCode /
--   buildDomainToSiteCodeMap 等方法需要从读 marketplace_id 改为读 domain 字段
--   Marketplace 实体类需要新增 domain 字段
-- ============================================================
