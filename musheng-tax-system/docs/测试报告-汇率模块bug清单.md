# 汇率模块测试报告 - Bug清单

**测试时间**: 2026-01-20
**测试范围**: 汇率查询页面、汇率转换功能
**测试页面**: `/rate/list` (汇率列表页面)

---

## Bug清单

### 🔴 P0级别 - 严重bug (阻塞功能)

#### Bug #1: 汇率转换接口未实现
**Bug描述**:
- 前端调用 `convertCurrency()` API,但后端 `RateController.java` 中没有对应的 `/convert` 接口
- 前端期望调用 `POST /api/v1/business/rates/convert`,但后端未提供此接口

**影响范围**:
- 汇率列表页面的货币转换工具无法使用
- 用户无法进行货币转换计算

**复现路径**:
1. 访问汇率列表页面 `/rate/list`
2. 在"货币转换"卡片中填写金额、选择货币、选择日期
3. 点击"转换"按钮
4. 触发错误: 404 Not Found

**相关文件**:
- 前端: `musheng-tax-web/src/api/rate.ts:78-80` (定义了 `convertCurrency()` 函数)
- 前端: `musheng-tax-web/src/views/rate/list/index.vue:406-426` (调用 `convertCurrency()`)
- 后端: `musheng-business/src/main/java/com/musheng/business/rate/controller/RateController.java` (缺少 `/convert` 接口)

**期望行为**:
- 后端应该提供 `POST /v1/business/rates/convert` 接口
- 接受参数: `{ amount, currencyCode, rateDate }`
- 返回结果: `{ originalAmount, convertedAmount, currencyCode, rate, rateDate }`

---

### 🟡 P1级别 - 主要bug (功能受损)

#### Bug #2: 搜索条件"数据来源"未生效
**Bug描述**:
- 汇率列表页面的搜索栏中有"数据来源"下拉框 (source字段)
- 但在 `fetchData()` 方法中构建查询参数时,未将 `searchForm.source` 传递给后端接口
- 导致选择数据来源后进行搜索,实际上未按数据来源过滤

**影响范围**:
- 用户无法按数据来源(人民银行/手动录入/文件导入)筛选汇率数据

**复现路径**:
1. 访问汇率列表页面 `/rate/list`
2. 在搜索栏选择"数据来源"为"文件导入"
3. 点击"查询"按钮
4. 观察表格数据,发现仍然显示所有来源的数据

**相关文件**:
- 前端: `musheng-tax-web/src/views/rate/list/index.vue:347-366` (`fetchData()` 方法)
- 前端: `musheng-tax-web/src/views/rate/list/index.vue:246-249` (`searchForm` 定义)
- 后端: `musheng-business/src/main/java/com/musheng/business/rate/controller/RateController.java:38-53` (list接口未接收source参数)

**问题代码** (index.vue:347-366):
```javascript
async function fetchData() {
  loading.value = true
  try {
    // 根据接口文档,汇率列表接口使用 yearMonth 参数
    const params = {
      currencyCode: searchForm.currencyCode,
      yearMonth: searchDateRange.value?.[0]?.format('YYYY-MM'),
      page: pagination.current,
      size: pagination.pageSize
      // ⚠️ 缺少 source: searchForm.source
    }
    const res = await getRateList(params)
    // ...
  }
}
```

**期望行为**:
1. 前端: 在 `fetchData()` 的 params 中添加 `source: searchForm.source`
2. 后端: 在 `RateController.list()` 方法中添加 `source` 参数
3. 后端: 在 `RateService.list()` 中根据 source 参数进行过滤

---

#### Bug #3: 搜索条件"日期范围"参数不匹配
**Bug描述**:
- 前端搜索栏使用的是"日期范围选择器" (`a-range-picker`),用户可选择开始日期和结束日期
- 但实际传递给后端的参数只有 `yearMonth` (只取了开始日期的年月)
- 后端接口也只接受 `yearMonth` 参数,不支持日期范围查询
- 导致用户选择"2025-01-01 至 2025-02-28"时,实际只查询了2025年1月的数据

**影响范围**:
- 用户无法精确查询指定日期范围的汇率数据
- UI显示的是日期范围选择器,但实际只能按月份查询,交互不一致

**复现路径**:
1. 访问汇率列表页面 `/rate/list`
2. 在搜索栏选择日期范围: 2025-01-15 至 2025-02-20
3. 点击"查询"按钮
4. 观察请求参数: 只有 `yearMonth=2025-01`
5. 观察结果: 只显示2025年1月的数据,2月的数据未包含

**相关文件**:
- 前端: `musheng-tax-web/src/views/rate/list/index.vue:48-56` (日期范围选择器UI)
- 前端: `musheng-tax-web/src/views/rate/list/index.vue:353` (只传递yearMonth)
- 后端: `musheng-business/src/main/java/com/musheng/business/rate/controller/RateController.java:38-53` (只接收yearMonth)

**期望行为**:
方案A (推荐): 支持日期范围查询
- 前端传递 `startDate` 和 `endDate` (格式: YYYY-MM-DD)
- 后端接口支持 `startDate` 和 `endDate` 参数
- 后端查询时按日期范围过滤: `rate_date BETWEEN startDate AND endDate`

方案B: 改为月份选择器
- 前端UI改为单个月份选择器,而非日期范围选择器
- 保持当前的 `yearMonth` 参数传递逻辑

---

### 🟢 P2级别 - 次要bug (体验优化)

#### Bug #4: 导入汇率按钮路由错误
**Bug描述**:
- 汇率列表页面右上角的"导入汇率"按钮,点击后跳转到 `/rate/manage`
- 但根据路由配置,正确的导入页面路径应该是 `/rate/import`

**影响范围**:
- 用户点击"导入汇率"按钮后跳转到错误页面(404或其他页面)
- 无法正常访问汇率导入功能

**复现路径**:
1. 访问汇率列表页面 `/rate/list`
2. 点击右上角"导入汇率"按钮
3. 观察地址栏跳转到 `/rate/manage` (错误路径)

**相关文件**:
- 前端: `musheng-tax-web/src/views/rate/list/index.vue:387-389` (`handleGoImport()` 方法)

**问题代码** (index.vue:387-389):
```javascript
function handleGoImport() {
  router.push('/rate/manage')  // ⚠️ 错误路径
}
```

**期望行为**:
```javascript
function handleGoImport() {
  router.push('/rate/import')  // ✅ 正确路径
}
```

---

#### Bug #5: 导出功能参数传递不完整
**Bug描述**:
- 导出功能在 `handleExport()` 中构建参数时,传递了 `source` 和日期范围
- 但在搜索时,`fetchData()` 并未使用日期范围,而是使用 `yearMonth`
- 导致导出和查询的过滤条件不一致

**影响范围**:
- 用户期望导出当前查询结果,但实际导出的数据可能与列表显示不一致

**相关文件**:
- 前端: `musheng-tax-web/src/views/rate/list/index.vue:391-404` (`handleExport()` 方法)

**问题代码** (index.vue:391-404):
```javascript
async function handleExport() {
  try {
    const params = {
      currencyCode: searchForm.currencyCode,
      source: searchForm.source,  // 这里使用了source
      startDate: searchDateRange.value?.[0]?.format('YYYY-MM-DD'),
      endDate: searchDateRange.value?.[1]?.format('YYYY-MM-DD')
    }
    await exportRateData(params)
    message.success('导出成功')
  } catch (error) {
    console.error('导出失败:', error)
  }
}
```

**期望行为**:
- 统一查询和导出的参数逻辑
- 如果查询使用 `yearMonth`,导出也应使用 `yearMonth`
- 或者两者都改为使用日期范围 (`startDate` 和 `endDate`)

---

## Bug汇总统计

| 级别 | 数量 | Bug编号 |
|------|------|---------|
| P0 - 严重 | 1 | #1 |
| P1 - 主要 | 3 | #2, #3 |
| P2 - 次要 | 2 | #4, #5 |
| **总计** | **6** | - |

---

## 修复优先级建议

### 第一优先级 (阻塞功能,必须修复)
1. **Bug #1**: 实现汇率转换接口 - 后端添加 `/convert` 接口

### 第二优先级 (影响主要功能)
2. **Bug #2**: 修复"数据来源"搜索条件 - 前端传递参数,后端接收参数
3. **Bug #3**: 统一日期查询逻辑 - 支持日期范围查询或改为月份选择器

### 第三优先级 (体验优化)
4. **Bug #4**: 修复导入汇率按钮路由
5. **Bug #5**: 统一查询和导出的参数逻辑

---

## 需要调用的Agent

### 后端Agent
- 修复 **Bug #1**: 在 `RateController` 中实现 `/convert` 接口
- 修复 **Bug #2**: 在 `RateController.list()` 中添加 `source` 参数支持
- 修复 **Bug #3**: 在 `RateController.list()` 中支持日期范围查询 (startDate/endDate)

### 前端Agent
- 修复 **Bug #2**: 在 `fetchData()` 中传递 `source` 参数
- 修复 **Bug #3**: 根据后端接口调整,传递日期范围或改为月份选择器
- 修复 **Bug #4**: 修改 `handleGoImport()` 的路由路径
- 修复 **Bug #5**: 统一导出参数与查询参数逻辑

---

## 测试建议

### 回归测试清单
修复完成后,需要重新测试以下场景:

1. **汇率转换功能**
   - [ ] 选择货币、输入金额、选择日期,点击转换
   - [ ] 验证转换结果正确显示
   - [ ] 验证使用最新汇率(不选日期)
   - [ ] 验证货币不存在时的错误提示

2. **搜索条件功能**
   - [ ] 按货币编码搜索
   - [ ] 按数据来源搜索(人民银行/手动录入/文件导入)
   - [ ] 按日期范围搜索
   - [ ] 组合条件搜索
   - [ ] 重置按钮清空所有条件

3. **导入导出功能**
   - [ ] 点击导入按钮跳转到正确页面
   - [ ] 导出数据与当前查询结果一致
   - [ ] 导出空查询结果

4. **分页功能**
   - [ ] 切换页码
   - [ ] 切换每页条数
   - [ ] 搜索后分页重置到第1页

---

## 附录: 接口定义建议

### 汇率转换接口 (需要后端实现)

**接口路径**: `POST /api/v1/business/rates/convert`

**请求参数**:
```json
{
  "amount": 100,           // 原始金额
  "currencyCode": "USD",   // 货币编码
  "rateDate": "2025-01-15" // 汇率日期(可选,默认最新)
}
```

**响应结果**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "originalAmount": 100,
    "convertedAmount": 724.50,
    "currencyCode": "USD",
    "rate": 7.245,
    "rateDate": "2025-01-15"
  }
}
```

**业务逻辑**:
1. 根据 `currencyCode` 和 `rateDate` 查询汇率
2. 如果 `rateDate` 未提供,使用该货币的最新汇率
3. 如果查询不到汇率,返回错误 "未找到对应汇率"
4. 计算: `convertedAmount = originalAmount * rate`
5. 保留2位小数

---

**测试人员**: 测试Agent
**报告生成时间**: 2026-01-20
