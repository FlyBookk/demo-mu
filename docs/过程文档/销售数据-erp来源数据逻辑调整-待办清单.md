# 销售数据 - ERP来源数据逻辑调整 待办清单

> **文档目的**：控制开发进度，确保可持续增量开发，会话中断不影响整体流程
> 
> **创建时间**：2026-01-24
> 
> **关联文档**：[销售数据-erp来源数据逻辑调整流程.md](销售数据-erp来源数据逻辑调整流程.md)

---

## 进度概览

| 阶段 | 状态 | 完成时间 |
|------|------|----------|
| 第一阶段：枚举与映射完善 | ✅ 已完成 | 2026-01-24 |
| 第二阶段：聚合逻辑调整 | ✅ 已完成 | 2026-01-24 |
| 第三阶段：数据库适配 | ✅ 已完成 | 2026-01-24 |
| 第四阶段：测试与验证 | ✅ 已完成 | 2026-01-24 |
| 第五阶段：设计简化 | ✅ 已完成 | 2026-01-24 |
| 第六阶段：去重逻辑优化 | ✅ 已完成 | 2026-01-24 |

**图例**：✅ 已完成 | 🔄 进行中 | ⏳ 待开始 | ❌ 已取消

---

## 第一阶段：枚举与映射完善

### 1.1 创建来源类型枚举 `ErpSourceType` ✅
- [x] 创建枚举文件 `musheng-common/src/main/java/com/musheng/common/enums/ErpSourceType.java`
- [x] 定义 11 种来源类型及对应的结算类型
- [x] 添加根据来源值获取枚举的方法
- **完成时间**：2026-01-24

### 1.2 更新交易类型映射 ✅
- [x] 在 `ErpSettlementParser` 中新增 30+ 交易类型映射
- [x] 确保所有发现的交易类型都有对应映射
- **完成时间**：2026-01-24
- **新增映射**：共 48 个交易类型映射，覆盖所有发现的类型

### 1.3 更新数据实体 `SalesData` ✅
- [x] 添加 `settlementCategory` 字段（结算类型）
- [x] 添加 `source` 字段（原始来源）
- [x] 添加相应的 `@FieldMapping` 注解
- **完成时间**：2026-01-24

---

## 第二阶段：聚合逻辑调整

### 2.1 修改 `ErpRow` 内部类 ✅
- [x] 添加 `source` 字段
- **完成时间**：2026-01-24

### 2.2 修改 `ErpAggregateRow` 内部类 ✅
- [x] 添加 `source` 字段
- [x] 添加 `settlementCategory` 字段
- **完成时间**：2026-01-24

### 2.3 修改聚合 Key 生成逻辑 ✅
- [x] 更新 `buildAggregateKey()` 方法，增加来源维度
- [x] 新的 Key 格式：`source|orderId|siteCode|sku`
- **完成时间**：2026-01-24

### 2.4 修改解析逻辑 ✅
- [x] 在 `parseErpRow()` 中解析来源字段
- [x] 在 `createAggregateRow()` 中设置来源和结算类型（使用枚举转换）
- [x] 在 `convertToSalesData()` 中设置结算类型和来源
- [x] 交易分类逻辑改为基于结算类型判断
- [x] 更新预览 Map 添加新字段
- **完成时间**：2026-01-24

---

## 第三阶段：数据库适配

### 3.1 创建数据库迁移脚本 ✅
- [x] 创建 SQL 脚本添加 `settlement_category` 字段
- [x] 创建 SQL 脚本添加 `source` 字段
- [x] 添加字段注释和索引
- [x] 脚本路径：`sql/v1.4_erp_source_settlement_category.sql`
- **完成时间**：2026-01-24

### 3.2 验证数据库变更
- [ ] 执行迁移脚本（需手动执行）
- [ ] 验证表结构正确

---

## 第四阶段：测试与验证

### 4.1 代码编译验证 ✅
- [x] 项目编译通过，无语法错误
- **完成时间**：2026-01-24

### 4.2 单元测试（待执行）
- [ ] 测试来源类型枚举解析
- [ ] 测试交易类型映射完整性
- [ ] 测试聚合逻辑正确性

### 4.3 集成测试（待执行）
- [ ] 执行数据库迁移脚本
- [ ] 使用真实 ERP 数据测试导入
- [ ] 验证聚合结果正确性
- [ ] 验证数据库存储正确

### 4.4 回归测试（待执行）
- [ ] 确保原有功能不受影响
- [ ] 确保亚马逊原始数据导入正常

---

## 变更日志

| 时间 | 操作 | 说明 |
|------|------|------|
| 2026-01-24 | 创建文档 | 初始化待办清单 |
| 2026-01-24 | 完成第一阶段 | 创建枚举、更新映射、添加实体字段 |
| 2026-01-24 | 完成第二阶段 | 修改聚合逻辑、解析逻辑 |
| 2026-01-24 | 完成第三阶段 | 创建数据库迁移脚本 |
| 2026-01-24 | 编译验证 | 项目编译通过 |
| 2026-01-24 | 设计简化 | 移除冗余 source 字段，保留 settlement_category |
| 2026-01-24 | 去重逻辑优化 | ERP数据使用 Settlement ID+订单号+来源 去重 |

---

## 第五阶段：设计简化

### 5.1 移除冗余 source 字段 ✅
- [x] 从 SalesData 实体移除 source 字段
- [x] 更新 ErpSettlementParser 不再设置 source
- [x] 更新预览 Map 使用 transactionType
- **完成时间**：2026-01-24

### 5.2 保留 settlement_category 字段 ✅
- [x] 决定保留用于统计分析
- **完成时间**：2026-01-24

### 5.3 更新数据库迁移脚本 ✅
- [x] 移除 source 字段相关 SQL
- [x] 更新索引定义
- **完成时间**：2026-01-24

### 5.4 更新映射关系文档 ✅
- [x] 更新 docs/销售数据-ERP字段映射关系.md
- **完成时间**：2026-01-24

---

## 第六阶段：去重逻辑优化

### 6.1 修改 ERP 数据去重逻辑 ✅
- [x] ERP 数据使用 `Settlement ID+订单号+来源(transactionType)` 作为唯一键
- [x] 优先使用亚马逊 Settlement ID，如果没有则使用 ERP 结算编号
- [x] 原始数据保持使用 `订单号+站点+交易分类` 作为唯一键
- **完成时间**：2026-01-24

### 6.2 修改 batchCheckDuplicates 方法 ✅
- [x] 添加 `isErpData` 参数
- [x] ERP 数据按结算编号分批查询
- [x] 原始数据按订单号分批查询
- **完成时间**：2026-01-24

### 6.3 修改 buildOrderKey 方法 ✅
- [x] ERP 数据返回 `settlementId|orderId|transactionType`
- [x] 原始数据返回 `orderId|siteCode|transactionCategory`
- **完成时间**：2026-01-24

### 6.4 修改 deleteExistingData 方法 ✅
- [x] ERP 数据按 `结算编号+订单号+来源` 删除
- [x] 原始数据按 `订单号+站点+交易分类` 删除
- **完成时间**：2026-01-24

---

## 变更文件清单

| 文件路径 | 操作 | 说明 |
|----------|------|------|
| `musheng-common/.../enums/ErpSourceType.java` | 新增 | ERP来源类型枚举（11种） |
| `musheng-business/.../entity/SalesData.java` | 修改 | 添加 settlementCategory 字段（已移除 source） |
| `musheng-business/.../parser/ErpSettlementParser.java` | 修改 | 更新映射(48个)、聚合逻辑、解析逻辑 |
| `sql/v1.4_erp_source_settlement_category.sql` | 新增 | 数据库迁移脚本（仅 settlement_category） |
| `docs/销售数据-ERP字段映射关系.md` | 新增 | 字段映射关系文档 |
| `SalesDataServiceImpl.java` | 修改 | ERP去重逻辑：Settlement ID+订单号+来源 |
| `ErpSettlementParser.java` | 修改 | 解析 Settlement ID 作为 settlementId |

