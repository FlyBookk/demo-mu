-- ============================================================
-- 慕声报税系统 - 站点初始化数据 (t_marketplace)
-- 数据来源：Amazon SP-API 官方 Marketplace IDs（全量 24 个站点）
-- https://developer-docs.amazon.com/sp-api/docs/marketplace-ids
--
-- status 说明：
--   1 = 启用（当前业务在用的站点）
--   0 = 禁用（预置数据，按需开启）
--
-- seller_id 留空，请根据实际卖家账号填写
-- 执行前请确认 t_currency 中已有对应货币数据
-- 使用 ON DUPLICATE KEY UPDATE，可安全重复执行
-- ============================================================

INSERT INTO t_marketplace (site_code, site_name, marketplace_id, currency_code, seller_id, header_language, date_format, number_format, timezone, status) VALUES

-- ================================================================
-- 北美区域 (North America) — 统一账号，共享 Seller ID
-- 区域端点：https://sellingpartnerapi-na.amazon.com
-- ================================================================
('US',  '美国站',     'ATVPDKIKX0DER',  'USD', NULL, 'EN', 'MMM d, yyyy h:mm:ss a z',  '.', 'America/Los_Angeles',  1),
('CA',  '加拿大站',   'A2EUQ1WTGCTBG2',  'CAD', NULL, 'EN', 'MMM d, yyyy h:mm:ss a z',  '.', 'America/Toronto',      1),
('MX',  '墨西哥站',   'A1AM78C64UM0Y8',  'MXN', NULL, 'ES', 'dd/MM/yyyy HH:mm:ss z',    '.', 'America/Mexico_City',  0),
('BR',  '巴西站',     'A2Q3Y263D00KWC',  'BRL', NULL, 'PT', 'dd/MM/yyyy HH:mm:ss z',    ',', 'America/Sao_Paulo',    0),

-- ================================================================
-- 欧洲区域 (Europe) — 统一账号，共享 Seller ID
-- 区域端点：https://sellingpartnerapi-eu.amazon.com
-- ================================================================

-- 西欧核心站点
('UK',  '英国站',     'A1F83G8C2ARO7P',  'GBP', NULL, 'EN', 'dd MMM yyyy HH:mm:ss z',   '.', 'Europe/London',        1),
('DE',  '德国站',     'A1PA6795UKMFR9',  'EUR', NULL, 'DE', 'dd.MM.yyyy HH:mm:ss z',    ',', 'Europe/Berlin',        1),
('FR',  '法国站',     'A13V1IB3VIYZZH',  'EUR', NULL, 'FR', 'dd/MM/yyyy HH:mm:ss z',    ',', 'Europe/Paris',         0),
('IT',  '意大利站',   'APJ6JRA9NG5V4',   'EUR', NULL, 'IT', 'dd/MM/yyyy HH:mm:ss z',    ',', 'Europe/Rome',          0),
('ES',  '西班牙站',   'A1RKKUPIHCS9HS',  'EUR', NULL, 'ES', 'dd/MM/yyyy HH:mm:ss z',    ',', 'Europe/Madrid',        0),

-- 西欧扩展站点
('NL',  '荷兰站',     'A1805IZSGTT6HS',  'EUR', NULL, 'NL', 'dd-MM-yyyy HH:mm:ss z',    ',', 'Europe/Amsterdam',     0),
('BE',  '比利时站',   'AMEN7PMS3EDWL',   'EUR', NULL, 'FR', 'dd/MM/yyyy HH:mm:ss z',    ',', 'Europe/Brussels',      0),
('IE',  '爱尔兰站',   'A28R8C7NBKEWEA',  'EUR', NULL, 'EN', 'dd/MM/yyyy HH:mm:ss z',    '.', 'Europe/Dublin',        0),

-- 北欧 & 东欧
('SE',  '瑞典站',     'A2NODRKZP88ZB9',  'SEK', NULL, 'SV', 'yyyy-MM-dd HH:mm:ss z',    ',', 'Europe/Stockholm',     0),
('PL',  '波兰站',     'A1C3SOZRARQ6R3',  'PLN', NULL, 'PL', 'dd.MM.yyyy HH:mm:ss z',    ',', 'Europe/Warsaw',        0),

-- ================================================================
-- 中东与非洲 (Middle East & Africa) — 归属欧洲区域端点
-- ================================================================
('TR',  '土耳其站',   'A33AVAJ2PDY3EV',  'TRY', NULL, 'TR', 'dd.MM.yyyy HH:mm:ss z',    ',', 'Europe/Istanbul',      0),
('EG',  '埃及站',     'ARBP9OOSHTCHU',   'EGP', NULL, 'AR', 'dd/MM/yyyy HH:mm:ss z',    '.', 'Africa/Cairo',         0),
('SA',  '沙特站',     'A17E79C6D8DWNP',  'SAR', NULL, 'AR', 'dd/MM/yyyy HH:mm:ss z',    '.', 'Asia/Riyadh',          0),
('AE',  '阿联酋站',   'A2VIGQ35RCS4UG',  'AED', NULL, 'EN', 'dd/MM/yyyy HH:mm:ss z',    '.', 'Asia/Dubai',           0),
('ZA',  '南非站',     'AE08WJ6YKNBMC',   'ZAR', NULL, 'EN', 'dd/MM/yyyy HH:mm:ss z',    '.', 'Africa/Johannesburg',  0),
('IN',  '印度站',     'A21TJRUUN4KGV',   'INR', NULL, 'EN', 'dd/MM/yyyy HH:mm:ss z',    '.', 'Asia/Kolkata',         0),

-- ================================================================
-- 亚太区域 (Far East)
-- 区域端点：https://sellingpartnerapi-fe.amazon.com
-- ================================================================
('JP',  '日本站',     'A1VC38T7YXB528',  'JPY', NULL, 'JA', 'yyyy/MM/dd HH:mm:ss z',    '.', 'Asia/Tokyo',           1),
('AU',  '澳大利亚站', 'A39IBJ37TRP1C6',  'AUD', NULL, 'EN', 'dd/MM/yyyy HH:mm:ss z',    '.', 'Australia/Sydney',     0),
('SG',  '新加坡站',   'A19VAU5U5O7RUS',  'SGD', NULL, 'EN', 'dd/MM/yyyy HH:mm:ss z',    '.', 'Asia/Singapore',       0)

ON DUPLICATE KEY UPDATE
  site_name = VALUES(site_name),
  marketplace_id = VALUES(marketplace_id),
  currency_code = VALUES(currency_code),
  header_language = VALUES(header_language),
  date_format = VALUES(date_format),
  number_format = VALUES(number_format),
  timezone = VALUES(timezone);

-- ============================================================
-- 备注：亚马逊目前没有韩国 (KR) 独立卖家站点
-- 韩国消费者可通过 amazon.com 跨境购买，但不存在 amazon.kr
-- 如果未来亚马逊开放韩国站，补充一条即可：
-- ('KR', '韩国站', 'TBD', 'KRW', NULL, 'KO',
--  'yyyy-MM-dd HH:mm:ss z', '.', 'Asia/Seoul', 0)
-- ============================================================
