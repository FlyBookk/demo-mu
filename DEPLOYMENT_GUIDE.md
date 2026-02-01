# FBA货件功能部署指南

## 📋 部署前检查清单

- [x] 后端代码已实现（12个Java文件）
- [x] 前端代码已实现（3个Vue页面）
- [x] 数据库迁移脚本已准备
- [x] 代码编译通过
- [ ] 数据库迁移已执行 ⚠️ **待执行**
- [ ] 后端服务已重启
- [ ] 前端已刷新

---

## 🗄️ 步骤 1: 执行数据库迁移（必须）

### 方式 A: 一次性执行（推荐）

```bash
mysql -u root -p musheng_tax << 'EOF'
-- 1. 表结构重构
SOURCE musheng-tax-system/sql/v2.2_fba_shipment_restructure.sql;

-- 2. 添加 file_hash 字段
ALTER TABLE t_import_record
ADD COLUMN file_hash varchar(64) DEFAULT NULL
COMMENT '文件哈希值（MD5，用于幂等性检查）'
AFTER file_path;

ALTER TABLE t_import_record
ADD INDEX idx_file_hash (file_hash);

ALTER TABLE t_import_record
ADD INDEX idx_shop_hash_type (shop_id, file_hash, data_type);

-- 3. 验证
DESCRIBE t_import_record;
SHOW TABLES LIKE 't_fba_shipment%';
SELECT 'Migration completed successfully!' AS result;
EOF
```

### 方式 B: 分步执行

```bash
# 步骤 1: 表结构重构
mysql -u root -p musheng_tax < musheng-tax-system/sql/v2.2_fba_shipment_restructure.sql

# 步骤 2: 添加哈希字段
mysql -u root -p musheng_tax < musheng-tax-system/sql/v2.3_add_file_hash_to_import_record.sql
```

### 验证数据库迁移

```bash
# 检查 file_hash 字段是否存在
mysql -u root -p musheng_tax -e "DESCRIBE t_import_record;" | grep file_hash

# 检查新表是否创建
mysql -u root -p musheng_tax -e "SHOW TABLES LIKE 't_fba_shipment%';"

# 预期输出:
# t_fba_shipment
# t_fba_shipment_item
```

---

## 🚀 步骤 2: 重启后端服务

```bash
cd musheng-tax-system

# 方式 1: Maven 运行
mvn spring-boot:run

# 方式 2: 如果已打包
java -jar musheng-web/target/musheng-web-1.0.0-SNAPSHOT.jar
```

---

## 🌐 步骤 3: 刷新前端

```bash
# 如果前端正在运行，刷新浏览器
# 按 Ctrl+Shift+R (Windows/Linux) 或 Cmd+Shift+R (Mac)

# 或重启前端开发服务器
cd musheng-tax-web
npm run dev
```

---

## 🧪 步骤 4: 功能测试

### 测试 1: 批量导入功能

1. **访问导入页面**
   ```
   http://localhost:3000/fba-shipment/import
   ```

2. **上传多个文件**
   - 拖拽或选择 2-3 个 Excel 文件
   - 查看文件列表
   - 点击"开始导入 (N 个文件)"

3. **验证导入结果**
   - ✅ 所有文件成功导入
   - ✅ 显示总文件数、SKU数、货件数
   - ✅ 每个文件的状态为"成功"

### 测试 2: 幂等性验证

1. **再次上传相同文件**
   - 上传刚才导入过的文件

2. **验证幂等性**
   - ✅ 文件状态显示为"跳过"
   - ✅ 提示信息："文件已导入过"
   - ✅ 不会重复导入数据

### 测试 3: SKU 明细视图

1. **进入明细视图**
   ```
   http://localhost:3000/fba-shipment/list
   ```
   - 点击"SKU明细视图"按钮

2. **测试筛选功能**
   - 输入货件单号搜索
   - 输入 SKU 搜索
   - 选择店铺和国家
   - 设置日期范围

3. **验证结果**
   - ✅ 显示所有符合条件的 SKU 明细
   - ✅ 支持分页
   - ✅ 可以复制货件单号、SKU、MSKU

### 测试 4: 货件列表功能

1. **查看货件列表**
   ```
   http://localhost:3000/fba-shipment/list
   ```

2. **测试功能**
   - 查看统计卡片（总货件数、总SKU数、总发货量）
   - 搜索和筛选货件
   - 点击"详情"查看单个货件的 SKU 明细
   - 删除货件

---

## 📊 预期结果

### 导入成功示例

```json
{
  "totalFiles": 3,
  "successFiles": 3,
  "failFiles": 0,
  "skippedFiles": 0,
  "totalSkuCount": 150,
  "successSkuCount": 150,
  "totalShipmentCount": 5,
  "fileResults": [
    {
      "fileName": "发货单-MSDE-FBA15LCGV539.xlsx",
      "status": "success",
      "message": "导入 2 个货件"
    },
    ...
  ]
}
```

### 幂等性验证示例

```json
{
  "totalFiles": 3,
  "successFiles": 0,
  "failFiles": 0,
  "skippedFiles": 3,
  "fileResults": [
    {
      "fileName": "发货单-MSDE-FBA15LCGV539.xlsx",
      "status": "skipped",
      "message": "文件已导入过"
    },
    ...
  ]
}
```

---

## ❌ 常见问题排查

### 问题 1: Unknown column 'file_hash'

**原因**: 数据库迁移脚本未执行

**解决**: 执行步骤 1 的数据库迁移命令

### 问题 2: 前端函数冲突错误

**原因**: 已修复，两个函数功能不同
- `handleViewDetail(record)` - 打开货件详情弹窗
- `handleGoToDetailView()` - 跳转到明细视图页面

**验证**: 前端应该正常编译

### 问题 3: 路由 404

**原因**: 前端路由未配置

**解决**: 确保路由配置包含 `/fba-shipment/detail`

---

## 📝 数据库表结构

### t_fba_shipment (货件主表)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键 |
| shop_id | bigint | 店铺ID |
| shipment_id | varchar(50) | 货件单号 |
| warehouse_code | varchar(255) | 物流中心编码 |
| shop_name | varchar(100) | 店铺名称 |
| country | varchar(50) | 国家 |
| created_date | datetime | 货件创建时间 |
| sku_count | int | SKU种类数 |
| total_quantity | int | 总发货量 |
| import_batch_id | bigint | 导入批次ID |

### t_fba_shipment_item (货件明细表)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键 |
| shop_id | bigint | 店铺ID |
| shipment_id | bigint | 货件主表ID |
| shipment_no | varchar(50) | 货件单号 |
| sku | varchar(100) | 内部SKU |
| msku | varchar(100) | 亚马逊MSKU |
| quantity | int | 发货量 |
| import_batch_id | bigint | 导入批次ID |

### t_import_record (导入记录表 - 新增字段)

| 字段 | 类型 | 说明 |
|------|------|------|
| file_hash | varchar(64) | 文件哈希值（MD5） |

---

## 🎯 功能亮点

1. **批量导入效率提升**
   - 一次上传多个文件，节省时间
   - 自动去重，避免重复导入

2. **数据完整性保障**
   - 文件哈希值检查
   - 事务管理确保数据一致性

3. **多视角数据查看**
   - 货件维度：查看货件汇总信息
   - 明细维度：全局检索 SKU 明细

4. **用户体验优化**
   - 清晰的导入结果反馈
   - 灵活的筛选和搜索功能
   - 快速的视图切换

---

## 📞 技术支持

如遇到问题，请检查：
1. 数据库迁移是否成功执行
2. 后端服务是否正常启动
3. 前端是否已刷新
4. 浏览器控制台是否有错误信息

---

**部署完成后，请在此文档中勾选完成的步骤！**
