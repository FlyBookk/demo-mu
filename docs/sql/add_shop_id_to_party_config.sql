-- 为 t_document_party_config 表添加 shop_id 字段，实现店铺数据隔离
-- 执行时间：2026年03月23日

-- 1. 添加 shop_id 字段
ALTER TABLE t_document_party_config
    ADD COLUMN shop_id BIGINT NOT NULL DEFAULT 0 COMMENT '店铺ID（数据隔离）' AFTER id;

-- 2. 将现有数据归属到默认店铺（shop_id=1），按实际情况调整
UPDATE t_document_party_config SET shop_id = 1 WHERE shop_id = 0;

-- 3. 删除旧的全局唯一索引，改为店铺维度唯一
ALTER TABLE t_document_party_config DROP INDEX uk_site_code;
ALTER TABLE t_document_party_config
    ADD UNIQUE KEY uk_shop_site_code (shop_id, site_code, deleted);

-- 4. 添加 shop_id 索引，加速按店铺查询
ALTER TABLE t_document_party_config
    ADD INDEX idx_shop_id (shop_id);
