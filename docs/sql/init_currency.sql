-- ============================================================
-- 慕声报税系统 - 货币初始化数据 (t_currency)
-- 包含：亚马逊全球站点货币 + 跨境电商常用货币
-- 执行前请确认表已创建
-- 使用 ON DUPLICATE KEY UPDATE，可安全重复执行
-- ============================================================

INSERT INTO t_currency (currency_code, currency_name, currency_symbol, decimal_places, status, pair_direction) VALUES

-- ========================================
-- 第一部分：亚马逊站点直接使用的货币（18种）
-- ========================================

-- 北美区域
('USD', '美元', '$', 2, 1, 'DIRECT'),
('CAD', '加元', 'C$', 2, 1, 'DIRECT'),
('MXN', '墨西哥比索', 'MX$', 2, 1, 'DIRECT'),
('BRL', '巴西雷亚尔', 'R$', 2, 1, 'DIRECT'),

-- 欧洲区域
('GBP', '英镑', '£', 2, 1, 'DIRECT'),
('EUR', '欧元', '€', 2, 1, 'DIRECT'),
('SEK', '瑞典克朗', 'kr', 2, 1, 'DIRECT'),
('PLN', '波兰兹罗提', 'zł', 2, 1, 'DIRECT'),
('TRY', '土耳其里拉', '₺', 2, 1, 'DIRECT'),
('EGP', '埃及镑', 'E£', 2, 1, 'DIRECT'),
('ZAR', '南非兰特', 'R', 2, 1, 'DIRECT'),

-- 中东区域
('SAR', '沙特里亚尔', 'SAR', 2, 1, 'DIRECT'),
('AED', '阿联酋迪拉姆', 'AED', 2, 1, 'DIRECT'),
('INR', '印度卢比', '₹', 2, 1, 'DIRECT'),

-- 亚太区域
('SGD', '新加坡元', 'S$', 2, 1, 'DIRECT'),
('AUD', '澳元', 'A$', 2, 1, 'DIRECT'),
('JPY', '日元', '¥', 0, 1, 'DIRECT'),

-- 基准货币
('CNY', '人民币', '¥', 2, 1, 'DIRECT'),

-- ========================================
-- 第二部分：跨境电商常用扩展货币（8种）
-- 用于供应链结算、转口贸易、离岸公司等场景
-- 默认禁用，按需启用
-- ========================================

('HKD', '港币', 'HK$', 2, 0, 'DIRECT'),
('TWD', '新台币', 'NT$', 0, 0, 'DIRECT'),
('KRW', '韩元', '₩', 0, 0, 'DIRECT'),
('THB', '泰铢', '฿', 2, 0, 'DIRECT'),
('VND', '越南盾', '₫', 0, 0, 'DIRECT'),
('PHP', '菲律宾比索', '₱', 2, 0, 'DIRECT'),
('MYR', '马来西亚林吉特', 'RM', 2, 0, 'DIRECT'),
('NZD', '新西兰元', 'NZ$', 2, 0, 'DIRECT')

ON DUPLICATE KEY UPDATE
  currency_name = VALUES(currency_name),
  currency_symbol = VALUES(currency_symbol),
  decimal_places = VALUES(decimal_places),
  pair_direction = VALUES(pair_direction);
