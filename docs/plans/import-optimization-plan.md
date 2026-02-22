# 销售/配送数据导入 - 综合检测报告与处理计划

## 一、N+1 查询问题

### 1.1 销售数据导入

#### 路径 A：`importData(siteCode, file)`（简单 CSV 导入）

| 问题点 | 位置 | 现状 | 严重程度 |
|--------|------|------|----------|
| **重复检查 N+1** | `SalesDataImportServiceImpl.importData` 第 185 行 | 每行调用 `isDuplicate(salesData)` → 每行 1 次 `SELECT COUNT` | 高 |
| **逐条插入** | 第 186 行 | 每行调用 `salesDataRepository.save()` → 每行 1 次 `INSERT` | 高 |
| **汇率未填充** | - | 该路径未调用 `fillExchangeRate()`，导入数据可能缺少汇率 | 中 |

**影响**：导入 1 万行 ≈ 1 万次重复检查 + 1 万次单条插入，性能差。

---

#### 路径 B：`executeImport(request)`（上传→预览→执行）

| 问题点 | 位置 | 现状 | 严重程度 |
|--------|------|------|----------|
| **汇率查询循环** | 第 537-541 行 | `for` 循环内逐条调用 `fillExchangeRate(data)` | 中 |
| 重复检查 | 第 546 行 | `batchCheckDuplicates` 批量查询 | 已优化 |
| 批量插入 | 第 578-584 行 | `executeBatchInsert` 分批插入 | 已优化 |

**说明**：`fillExchangeRate` 内部通过 `rateService.getRateWithDate()` 查询，有 Caffeine 缓存（key: `currencyCode_date`）。相同货币+日期会命中缓存，但不同组合仍会产生多次 DB 查询。若 1 万行涉及 100 种 (货币, 日期) 组合，则约 100 次 DB 查询。

---

### 1.2 配送数据导入

| 问题点 | 位置 | 现状 | 严重程度 |
|--------|------|------|----------|
| **汇率查询循环** | `ShippingDataServiceImpl.importData` 第 206-209 行 | `for` 循环内逐条调用 `fillExchangeRate(data)` | 中 |
| 站点配置 | 第 149-151 行 | 预加载 `marketplaceMap`，无 N+1 | 已优化 |
| 重复检查 | 第 191 行 | `batchCheckDuplicates` 批量查询 | 已优化 |
| 批量插入 | 第 214-232 行 | SqlSession BATCH 模式 | 已优化 |

**说明**：与销售路径 B 相同，汇率查询有缓存，但不同 (货币, 日期) 组合仍会产生多次 DB 查询。

---

### 1.3 N+1 处理计划

| 序号 | 任务 | 优先级 | 方案概要 |
|------|------|--------|----------|
| 1 | 销售 `importData` 重复检查 | P0 | 改为批量：先解析全部记录，再 `batchCheckDuplicates`，与 `executeImport` 一致 |
| 2 | 销售 `importData` 批量插入 | P0 | 改为 `SqlSession BATCH` 或 `saveBatch`，与配送一致 |
| 3 | 销售 `importData` 汇率填充 | P1 | 增加 `fillExchangeRate` 调用，与 `executeImport` 一致 |
| 4 | 汇率批量预加载 | P2 | 在 `fillExchangeRate` 前，按 (currencyCode, date) 去重，一次性批量查询并预热缓存，再循环填充 |

---

## 二、货币/站点不存在时的处理

### 2.1 当前行为

| 场景 | 销售导入 | 配送导入 |
|------|----------|----------|
| **站点不存在** | `importData`：用户传入 siteCode，查不到则抛 `Marketplace not found` | 从 CSV 解析销售渠道→siteCode，`marketplaceMap.get(siteCode)` 为 null 则抛 `Marketplace not found for site: X` |
| **货币不存在** | 货币来自站点配置，不单独校验 | 货币来自 CSV 或站点，不校验是否在 `t_currency` |
| **汇率不存在** | `fillExchangeRate` 中 `getRateWithDate` 抛异常 | 同上 |

**结论**：当前逻辑为「不存在即报错」，无自动创建。

---

### 2.2 自动添加需求与方案

| 序号 | 任务 | 方案概要 |
|------|------|----------|
| 1 | **站点自动添加** | 当 `marketplaceMap.get(siteCode)` 为 null 时，根据 siteCode 创建默认 Marketplace（如 marketplaceId=siteCode, currencyCode 按站点映射），插入 `t_marketplace` 后加入本次导入的缓存 |
| 2 | **货币自动添加** | 当 CSV 中出现新货币且 `t_currency` 中不存在时，调用 `CurrencyService.create` 创建并启用该货币 |
| 3 | **边界与配置** | 可配置开关（如 `import.auto-create-marketplace`、`import.auto-create-currency`），默认建议关闭，避免误创建脏数据 |

---

### 2.3 实现注意点

1. **站点自动添加**：需确定默认 `marketplaceId`、`currencyCode` 等字段的生成规则（可参考 `mapSalesChannelToSiteCode` 的站点→货币映射）。
2. **货币自动添加**：需确定 `currencyName`、`symbol` 等默认值，可考虑内置常用货币映射表。
3. **事务**：自动创建应在导入事务内执行，失败时整体回滚。

---

## 三、处理优先级建议

| 优先级 | 任务 | 预估工作量 |
|--------|------|------------|
| P0 | 销售 `importData`：批量重复检查 + 批量插入 | 中 |
| P1 | 销售 `importData`：补充汇率填充 | 小 |
| P2 | 汇率批量预加载（销售+配送） | 中 |
| P3 | 站点/货币自动添加（含配置开关） | 中 |

---

## 四、相关代码位置

| 模块 | 文件 | 关键方法/行号 |
|------|------|---------------|
| 销售导入 | `SalesDataImportServiceImpl.java` | `importData` 175-201, `executeImport` 537-541, `fillExchangeRate` 896-918, `isDuplicate` 880-884 |
| 配送导入 | `ShippingDataServiceImpl.java` | `importData` 103-286, `fillExchangeRate` 607-627, `parseShippingRecord` 360-385 |
| 汇率服务 | `RateServiceImpl.java` | `getRateWithDate` 197-221（含缓存） |
| 站点服务 | `MarketplaceServiceImpl.java` | `create` |
| 货币服务 | `CurrencyServiceImpl.java` | `create` |
