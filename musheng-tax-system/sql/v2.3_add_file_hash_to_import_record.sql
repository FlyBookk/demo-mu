/*
 导入记录表添加文件哈希字段 - 数据库迁移脚本

 功能说明:
 1. 在 t_import_record 表中添加 file_hash 字段
 2. 用于实现文件导入的幂等性检查，防止重复导入

 创建时间: 2026-02-01
 版本: v2.3
*/

SET NAMES utf8mb4;

-- ----------------------------
-- 添加文件哈希字段
-- ----------------------------
ALTER TABLE `t_import_record`
ADD COLUMN `file_hash` varchar(64) DEFAULT NULL COMMENT '文件哈希值（MD5，用于幂等性检查）' AFTER `file_path`;

-- ----------------------------
-- 添加索引以提高查询性能
-- ----------------------------
ALTER TABLE `t_import_record`
ADD INDEX `idx_file_hash` (`file_hash`) COMMENT '文件哈希索引';

-- ----------------------------
-- 添加联合索引（店铺+哈希+数据类型）
-- ----------------------------
ALTER TABLE `t_import_record`
ADD INDEX `idx_shop_hash_type` (`shop_id`, `file_hash`, `data_type`) COMMENT '店铺+哈希+类型联合索引';

-- ----------------------------
-- 变更说明
-- ----------------------------
-- 1. 新增 file_hash 字段用于存储文件的 MD5 哈希值
-- 2. 通过哈希值检查文件是否已导入，实现幂等性
-- 3. 添加索引以提高查询性能
