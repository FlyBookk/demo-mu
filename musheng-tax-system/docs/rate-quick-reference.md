# 🎯 汇率模块前后端对接快速参考

## 📌 核心变更

### Before（旧方式）
```
用户上传CSV文件 → 导入汇率数据
```

### After（新方式）
```
方式1: 手动录入 → 新增/编辑单条汇率
方式2: 自动同步 → 从外汇中心批量同步
```

---

## 🔌 接口对接清单

### ✅ 已实现接口

| HTTP方法 | 接口路径 | 说明 | 前端调用 |
|---------|----------|------|---------|
| GET | `/api/v1/business/rates` | 汇率列表 | `getRateList()` |
| GET | `/api/v1/business/rates/{id}` | 汇率详情 | `getRateById(id)` |
| POST | `/api/v1/business/rates` | 新增汇率 | `createRate(data)` |
| PUT | `/api/v1/business/rates/{id}` | 修改汇率 | `updateRate(id, data)` |
| DELETE | `/api/v1/business/rates/{id}` | 删除汇率 | `deleteRate(id)` |
| DELETE | `/api/v1/business/rates` | 批量删除 | `batchDeleteRates(ids)` |
| POST | `/api/v1/business/rates/sync` | 同步汇率 | `syncRates(params)` |
| POST | `/api/v1/business/rates/sync/recent` | 同步N天 | `syncRecentDays(days)` |

### 📄 接口示例

#### 1. 新增汇率
```typescript
// 前端调用
const data = {
  currencyCode: 'USD',
  rateDate: '2026-01-20',
  rate: 7.234567,
  isWorkday: 1,
  source: 'MANUAL'
}
await createRate(data)
```

```java
// 后端接口
@PostMapping
public Result<ExchangeRate> create(@Valid @RequestBody RateRequest request)
```

#### 2. 同步汇率
```typescript
// 前端调用 - 最近7天
await syncRecentDays(7)

// 前端调用 - 指定日期范围
await syncRates({
  startDate: '2026-01-01',
  endDate: '2026-01-20'
})
```

```java
// 后端接口
@PostMapping("/sync/recent")
public Result<RateSyncResultDTO> syncRecentDays(@RequestParam int days)

@PostMapping("/sync")
public Result<RateSyncResultDTO> syncRates(
  @RequestParam LocalDate startDate,
  @RequestParam LocalDate endDate
)
```

---

## 📁 文件清单

### 后端文件
```
musheng-tax-system/musheng-business/src/main/java/com/musheng/business/rate/
├── controller/
│   └── RateController.java              ✏️ 新增CRUD接口
├── dto/
│   ├── RateRequest.java                 ✨ 新增
│   └── RateSyncResultDTO.java           ✅ 已有
├── service/
│   ├── RateService.java                 ✏️ 新增CRUD方法
│   ├── RateSyncService.java             ✅ 已有
│   └── impl/
│       ├── RateServiceImpl.java         ✏️ 实现CRUD方法
│       └── RateSyncServiceImpl.java     ✅ 已有
└── client/
    └── ChinaMoneyClient.java            ✅ 已有
```

### 前端文件
```
musheng-tax-web/src/
├── api/
│   └── rate.ts                          ✏️ 更新API接口
├── views/rate/
│   ├── manage/
│   │   └── index.vue                    ✨ 新建（主页面）
│   ├── import/
│   │   └── index.vue                    🗑️ 废弃
│   └── list/
│       └── index.vue                    🗑️ 废弃
└── types/
    └── rate.ts                          ✏️ 补充类型定义
```

---

## 🔀 数据流转

### 手动录入流程
```
用户操作 → 前端表单 → API调用 → 后端Controller → Service层校验 → 数据库
```

### 自动同步流程
```
用户触发 → 前端调用sync接口 → RateSyncService → ChinaMoneyClient
  → 获取外汇中心数据 → 过滤已启用货币 → 批量保存/更新 → 返回结果
```

---

## 🎨 前端组件结构

```vue
<template>
  汇率管理页面
  ├── 操作栏（新增、同步、导出、批量删除）
  ├── 搜索栏（货币、年月筛选）
  ├── 数据表格
  │   ├── 货币
  │   ├── 日期
  │   ├── 汇率值
  │   ├── 数据来源
  │   └── 操作列（编辑、删除）
  ├── 新增/编辑弹窗
  │   └── 表单（货币、日期、汇率、工作日）
  └── 同步弹窗
      └── 同步配置（方式、日期范围）
</template>
```

---

## 🎯 关键业务逻辑

### 1. 重复数据校验
```java
// 后端实现
LambdaQueryWrapper<ExchangeRate> wrapper = new LambdaQueryWrapper<>();
wrapper.eq(ExchangeRate::getRateDate, request.getRateDate())
       .eq(ExchangeRate::getCurrencyCode, request.getCurrencyCode());

if (exchangeRateMapper.selectCount(wrapper) > 0) {
    throw new BusinessException("该日期和货币的汇率已存在");
}
```

### 2. 货币过滤
```java
// 只同步已启用的货币
List<Currency> enabledCurrencies = currencyService.getEnabled();
List<String> currencyCodes = enabledCurrencies.stream()
    .map(Currency::getCurrencyCode)
    .collect(Collectors.toList());
```

### 3. 数据来源标识
```typescript
// 前端展示不同颜色
const sourceOptions = [
  { value: 'MANUAL', label: '手动录入', color: 'default' },
  { value: 'CHINA_MONEY', label: '外汇中心', color: 'blue' },
  { value: 'IMPORT', label: '文件导入', color: 'green' }
]
```

---

## ⚡ 快速测试命令

### 1. 新增汇率
```bash
curl -X POST http://localhost:8080/api/v1/business/rates \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {token}" \
  -d '{
    "currencyCode": "USD",
    "rateDate": "2026-01-20",
    "rate": 7.234567,
    "isWorkday": 1,
    "source": "MANUAL"
  }'
```

### 2. 同步最近7天
```bash
curl -X POST "http://localhost:8080/api/v1/business/rates/sync/recent?days=7" \
  -H "Authorization: Bearer {token}"
```

### 3. 查询汇率列表
```bash
curl -X GET "http://localhost:8080/api/v1/business/rates?page=1&size=20" \
  -H "Authorization: Bearer {token}"
```

---

## 📋 部署Checklist

### 后端
- [ ] 确认 `RateRequest.java` DTO已创建
- [ ] 确认 `RateController` 新增接口已添加
- [ ] 确认 `RateService` 实现类已更新
- [ ] 数据库无需变更（使用现有表）
- [ ] 重启后端服务
- [ ] 测试Swagger文档（http://localhost:8080/doc.html）

### 前端
- [ ] 更新 `src/api/rate.ts`
- [ ] 创建 `src/views/rate/manage/index.vue`
- [ ] 补充类型定义 `src/types/rate.ts`
- [ ] 更新路由配置
- [ ] 更新菜单配置
- [ ] 构建并部署前端

### 测试
- [ ] 新增汇率测试
- [ ] 编辑汇率测试
- [ ] 删除汇率测试
- [ ] 同步汇率测试
- [ ] 重复数据校验测试
- [ ] 权限测试

---

## 🆘 常见问题

### Q1: 同步失败怎么办？
**A**: 检查以下几点
1. 货币管理中是否有已启用的货币
2. 网络是否能访问外汇中心
3. 查看后端日志获取详细错误信息

### Q2: 为什么有些货币没有同步？
**A**: 系统只同步货币管理中**已启用**的货币，请先在货币管理中启用需要的货币。

### Q3: 可以批量导入历史数据吗？
**A**: 两种方式
1. 使用"同步汇率"功能，选择较大的日期范围
2. 保留的CSV导入功能（虽然已废弃但仍可用）

### Q4: 手动录入和自动同步的数据有什么区别？
**A**: 通过`source`字段区分
- `MANUAL` - 手动录入
- `CHINA_MONEY` - 自动同步

### Q5: 如何修改已同步的数据？
**A**: 直接点击"编辑"按钮即可修改，修改后`source`字段会保持不变。

---

**文档版本**: v2.0
**最后更新**: 2026-01-20
**联系方式**: 前端研发团队
