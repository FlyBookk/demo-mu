-- 数据库迁移脚本：添加货币对方向字段
-- 日期: 2026-01-20
-- 说明: 为 t_currency 表添加 pair_direction 字段，支持正向和反向货币对

-- 1. 添加 pair_direction 字段
ALTER TABLE `t_currency`
ADD COLUMN `pair_direction` varchar(20) DEFAULT 'DIRECT' COMMENT '货币对方向(DIRECT=XXX/CNY, REVERSE=CNY/XXX)';

-- 2. 更新现有货币的 pair_direction 值
-- 主要国际货币使用 DIRECT (XXX/CNY) 格式
UPDATE `t_currency` SET `pair_direction` = 'DIRECT'
WHERE `currency_code` IN ('USD', 'EUR', 'GBP', 'JPY', 'HKD', 'AUD', 'NZD', 'SGD', 'CHF', 'CAD', 'CNY');

-- 3. 如果有新兴市场货币，使用 REVERSE (CNY/XXX) 格式
-- 示例（如果需要添加这些货币）：
-- UPDATE `t_currency` SET `pair_direction` = 'REVERSE'
-- WHERE `currency_code` IN ('MOP', 'MYR', 'RUB', 'ZAR', 'KRW', 'AED', 'SAR', 'HUF', 'PLN', 'DKK', 'SEK', 'NOK', 'TRY', 'MXN', 'THB');
