# 广告数据录入优化规划

**创建日期**: 2026-02-23  
**数据来源**: 利润报表-广告发票记录-872100874146775040.xlsx  
**目标**: 解决录入报错、优化交互体验

---

## 1. 问题分析

### 1.1 报错根因

| 问题类型 | 具体表现 | 根因 |
|---------|---------|------|
| **Excel 日期解析** | 账单周期、开具时间解析失败 | Excel 将日期存为序列号(如 44927)，`sheet_to_json` 未启用 `cellDates` |
| **必填字段缺失** | 后端返回 "xxx不能为空" | 前端转换时未填充默认值或字段映射错误 |
| **invoiceAmount 校验** | "账单金额必须大于0" | 后端 `@DecimalMin("0.01")`，Excel 空值/0 会报错 |
| **币种校验** | "不支持的币种" | 原始数据可能是 "加元" 等中文，需映射到 USD/CAD/GBP/EUR |
| **日期格式** | 无法解析为 LocalDate | 前端传空字符串或非 YYYY-MM-DD 格式 |

### 1.2 交互痛点

| 痛点 | 现状 | 期望 |
|-----|------|------|
| **无即时预览** | 上传文件后只显示"已选择文件"，看不到解析结果 | 上传后立即展示解析出的前几行，便于确认 |
| **错误反馈滞后** | 到步骤3才看到失败详情，且无行号定位 | 步骤1/2 即做前端校验，错误行高亮 |
| **手动录入字段不全** | 只有 5 个字段，缺少发票状态、开具时间等必填项 | 补全必填字段或自动填充合理默认值 |
| **步骤割裂** | 文件/手动切换后需重新操作 | 保留已选数据，支持切换模式 |
| **去重无预览** | 导入前不知道会去重多少条 | 预览阶段展示「去重后 N 条」提示 |

---

## 2. 数据格式对照

### 2.1 原始 Excel 字段（利润报表-广告发票记录）

| 列名 | 类型 | 示例 | 映射到 |
|-----|------|------|--------|
| 店铺 | String | 慕声北美-CA | storeName |
| 发票编号 | String | 1871317CQPA25 | invoiceNumber |
| 发票状态 | String | PAID_IN_FULL | invoiceStatus |
| 支付类型 | String | CREDIT_CARD | paymentType |
| **账单周期** | String | 2025-06-30至2025-07-03 | billingStartDate, billingEndDate |
| 开具时间 | Date | 2025-07-02 | issueDate |
| 付款币种 | String | CAD / 加元 | currency |
| 账单金额 | Decimal | 73.23 | invoiceAmount |
| 广告活动 | String | D77自动 | campaignName |
| 活动ID | String | 302861713011712 | campaignId |
| 计价方式 | String | CPC | pricingModel |
| 点击 | Integer | 15 | clicks |
| 平均点击单价 | Decimal | 3.04 | avgCpc |
| 费用 | Decimal | 45.4 | cost |
| 其他费分摊 | Decimal | 0 | otherCost |
| 取值来源 | String | 业务报告 | dataSource |
| 承担商品 | Text | MSCA-D77-7-6,... | productList |
| 广告类型 | String | SPONSORED PRODUCTS | adType |

### 2.2 后端必填与校验

- `storeName`, `invoiceNumber`, `invoiceStatus`: 非空
- `billingStartDate`, `billingEndDate`, `issueDate`: 非空，LocalDate
- `currency`: USD/CAD/GBP/EUR
- `invoiceAmount`: >= 0.01
- `cost`: >= 0

---

## 3. 优化方案

### 3.1 修复 Excel 解析（P0）

**文件**: `musheng-tax-web/src/views/advertising/import/index.vue`

1. **启用日期解析**
   ```javascript
   const workbook = XLSX.read(data, { type: 'array', cellDates: true })
   ```

2. **统一日期转字符串**
   - 新增 `formatExcelDate(val): string`：支持 Date、数字(序列号)、字符串
   - 序列号转换：`const date = XLSX.SSF.parse_date_code(val)` 或 `new Date((val - 25569) * 86400 * 1000)`

3. **账单周期解析增强**
   - 支持 `至`、`~`、`-` 分隔
   - 若为单日期，则 startDate = endDate = 该日期
   - 若 parts 为空或无效，用开具时间或当天兜底

4. **币种映射**
   ```javascript
   const CURRENCY_MAP = { '美元': 'USD', '加元': 'CAD', '英镑': 'GBP', '欧元': 'EUR', 'USD': 'USD', ... }
   ```

5. **invoiceAmount 兜底**
   - 空/0 时：`invoiceAmount = cost || 0.01`（满足后端 >= 0.01）

### 3.2 前端预校验（P0）

**在步骤1→2 时**：

1. 对 `convertRowToImportRequest` 的每条结果做校验
2. 收集错误：`{ rowIndex, field, message }[]`
3. 若有错误：不进入步骤2，弹窗/行内展示错误列表，支持「仅导入有效行」选项

**在步骤2 预览表格**：

1. 错误行背景标红或加错误列
2. 展示「有效 N 条 / 无效 M 条」

### 3.3 交互优化（P1）

1. **上传后即时预览**
   - 文件解析成功后，在步骤1 下方展示前 5 行表格预览
   - 显示「共解析 N 条，去重后约 M 条」（需先做去重预览）

2. **手动录入补全**
   - 新增字段：开具时间（默认当天）、发票状态（默认 PAID_IN_FULL）
   - 或保持简化，在 `convertManualRowToImportRequest` 中自动补全

3. **错误定位**
   - 失败详情中展示「第 X 行」并支持点击定位到预览表格对应行

4. **空行过滤**
   - 解析后过滤全空行，避免无意义报错

### 3.4 后端容错（P2，可选）

1. **invoiceAmount 放宽**
   - 将 `@DecimalMin("0.01")` 改为 `@DecimalMin("0")`，业务层若为 0 则用 cost 填充

2. **错误信息增强**
   - 返回 `field` 字段，便于前端定位到具体输入框

---

## 4. 实施任务清单

| 序号 | 任务 | 文件 | 优先级 |
|-----|------|------|--------|
| 1 | 启用 XLSX cellDates，新增 formatExcelDate | import/index.vue | P0 |
| 2 | 增强 convertRowToImportRequest：日期、币种、invoiceAmount 兜底 | import/index.vue | P0 |
| 3 | 过滤空行、空发票编号行 | import/index.vue | P0 |
| 4 | 步骤1→2 时前端预校验，有误时阻止并展示错误 | import/index.vue | P0 |
| 5 | 补全 convertManualRowToImportRequest 必填字段 | import/index.vue | P0 |
| 6 | 上传后展示解析预览（前几行 + 去重提示） | import/index.vue | P1 |
| 7 | 预览表格错误行高亮 | import/index.vue | P1 |
| 8 | 后端 invoiceAmount 校验放宽（可选） | AdvertisingDataImportRequest.java | P2 |

---

## 5. 验收标准

- [ ] 使用「利润报表-广告发票记录」Excel 可无报错完成导入
- [ ] 日期为 Excel 序列号时能正确解析
- [ ] 币种为中文时能正确映射
- [ ] 步骤1 有解析预览，步骤2 有错误行提示
- [ ] 手动录入 5 字段可成功导入（其余自动补全）

---

**下一步**: 按任务清单顺序实施，优先完成 P0 任务。
