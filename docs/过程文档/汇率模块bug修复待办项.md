# 汇率模块Bug修复待办项

**创建时间**: 2026-01-20
**关联文档**: [测试报告-汇率模块bug清单.md](测试报告-汇率模块bug清单.md)

---

## 修复进度总览

| Bug编号 | 优先级 | 状态 | 负责方 | 相关文件 |
|---------|--------|------|--------|----------|
| Bug #1 | P0 | ⏳ 待修复 | 后端 | RateController.java, RateService.java |
| Bug #2 | P1 | ⏳ 待修复 | 后端+前端 | RateController.java, index.vue |
| Bug #3 | P1 | ⏳ 待修复 | 后端+前端 | RateController.java, index.vue |
| Bug #4 | P2 | ⏳ 待修复 | 前端 | index.vue |
| Bug #5 | P2 | ⏳ 待修复 | 前端 | index.vue |

---

## Bug #1: 实现汇率转换接口 (P0)

### 后端任务
- [ ] 在 `RateController.java` 中添加 `/convert` 接口
  - 路径: `POST /v1/business/rates/convert`
  - 参数: `RateConvertRequest { amount, currencyCode, rateDate }`
  - 返回: `RateConvertResult { originalAmount, convertedAmount, currencyCode, rate, rateDate }`

- [ ] 在 `RateService.java` 中实现 `convertCurrency()` 方法
  - 根据货币编码和日期查询汇率
  - 如果日期为空，使用最新汇率
  - 计算转换金额: `convertedAmount = originalAmount * rate`

- [ ] 创建请求DTO: `RateConvertRequest.java`
- [ ] 创建响应DTO: `RateConvertResultDTO.java`

### 测试验证
- [ ] 测试正常转换
- [ ] 测试使用最新汇率(不传日期)
- [ ] 测试货币不存在的错误处理
- [ ] 测试日期无汇率的错误处理

### 文件路径
- `musheng-business/src/main/java/com/musheng/business/rate/controller/RateController.java`
- `musheng-business/src/main/java/com/musheng/business/rate/service/RateService.java`
- `musheng-business/src/main/java/com/musheng/business/rate/service/impl/RateServiceImpl.java`
- `musheng-business/src/main/java/com/musheng/business/rate/dto/RateConvertRequest.java` (新建)
- `musheng-business/src/main/java/com/musheng/business/rate/dto/RateConvertResultDTO.java` (新建)

---

## Bug #2: 数据来源搜索条件支持 (P1)

### 后端任务
- [ ] 在 `RateController.list()` 方法中添加 `source` 参数
  - 参数类型: `String source`
  - 可选值: `PBOC`, `MANUAL`, `IMPORT`

- [ ] 在 `RateService.list()` 方法中添加 `source` 参数
- [ ] 在 `RateServiceImpl.list()` 中添加 source 查询条件
  - 使用 MyBatis-Plus 的 `eq()` 方法: `.eq(source != null, "source", source)`

### 前端任务
- [ ] 在 `fetchData()` 方法中添加 `source` 参数传递
  - 修改位置: `musheng-tax-web/src/views/rate/list/index.vue:353`
  - 添加: `source: searchForm.source`

### 测试验证
- [ ] 选择"人民银行"过滤
- [ ] 选择"手动录入"过滤
- [ ] 选择"文件导入"过滤
- [ ] 不选择(查询全部)

### 文件路径
- 后端: `musheng-business/src/main/java/com/musheng/business/rate/controller/RateController.java:40`
- 后端: `musheng-business/src/main/java/com/musheng/business/rate/service/impl/RateServiceImpl.java`
- 前端: `musheng-tax-web/src/views/rate/list/index.vue:347-366`

---

## Bug #3: 日期范围查询支持 (P1)

### 后端任务
- [ ] 在 `RateController.list()` 方法中添加日期范围参数
  - 移除: `String yearMonth`
  - 添加: `@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate`
  - 添加: `@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate`

- [ ] 在 `RateService.list()` 方法签名中修改参数
- [ ] 在 `RateServiceImpl.list()` 中修改查询逻辑
  - 移除 yearMonth 的模糊查询
  - 添加日期范围查询: `.ge(startDate != null, "rate_date", startDate)`
  - 添加日期范围查询: `.le(endDate != null, "rate_date", endDate)`

### 前端任务
- [ ] 修改 `fetchData()` 方法的参数构建
  - 移除: `yearMonth: searchDateRange.value?.[0]?.format('YYYY-MM')`
  - 添加: `startDate: searchDateRange.value?.[0]?.format('YYYY-MM-DD')`
  - 添加: `endDate: searchDateRange.value?.[1]?.format('YYYY-MM-DD')`

### 测试验证
- [ ] 选择单个日期范围
- [ ] 选择跨月日期范围
- [ ] 不选择日期(查询全部)
- [ ] 只选择开始日期
- [ ] 只选择结束日期

### 文件路径
- 后端: `musheng-business/src/main/java/com/musheng/business/rate/controller/RateController.java:40-45`
- 后端: `musheng-business/src/main/java/com/musheng/business/rate/service/impl/RateServiceImpl.java`
- 前端: `musheng-tax-web/src/views/rate/list/index.vue:347-366`

---

## Bug #4: 导入汇率按钮路由修正 (P2)

### 前端任务
- [ ] 修改 `handleGoImport()` 方法中的路由路径
  - 文件: `musheng-tax-web/src/views/rate/list/index.vue:387-389`
  - 修改前: `router.push('/rate/manage')`
  - 修改后: `router.push('/rate/import')`

### 测试验证
- [ ] 点击"导入汇率"按钮
- [ ] 验证跳转到汇率导入页面

### 文件路径
- 前端: `musheng-tax-web/src/views/rate/list/index.vue:387-389`

---

## Bug #5: 统一导出参数逻辑 (P2)

### 前端任务
- [ ] 修改 `handleExport()` 方法的参数构建，与查询参数保持一致
  - 文件: `musheng-tax-web/src/views/rate/list/index.vue:391-404`
  - 修改前: 使用 `startDate/endDate`
  - 修改后: 根据Bug #3的修复结果，保持与查询一致

### 测试验证
- [ ] 设置搜索条件后导出
- [ ] 验证导出数据与列表显示一致
- [ ] 测试无搜索条件的导出

### 文件路径
- 前端: `musheng-tax-web/src/views/rate/list/index.vue:391-404`

---

## 修复顺序

### 第一批次 (P0 - 必须立即修复)
1. ✅ Bug #1 (后端) - 实现汇率转换接口

### 第二批次 (P1 - 主要功能修复)
2. ✅ Bug #2 (后端) - 添加数据来源查询支持
3. ✅ Bug #3 (后端) - 修改为日期范围查询
4. ✅ Bug #2 (前端) - 传递source参数
5. ✅ Bug #3 (前端) - 传递日期范围参数

### 第三批次 (P2 - 体验优化)
6. ✅ Bug #4 (前端) - 修正路由路径
7. ✅ Bug #5 (前端) - 统一导出参数

---

## 注意事项

1. **接口兼容性**: 修改后端接口参数时，确保不影响其他调用方
2. **参数验证**: 添加必要的参数校验和错误处理
3. **日期格式**: 统一使用 `YYYY-MM-DD` 格式
4. **空值处理**: 所有搜索条件都应支持为空(查询全部)
5. **测试数据**: 确保有足够的测试数据覆盖各种场景

---

## 完成标准

所有任务完成后，需要：
- [ ] 编译通过无错误
- [ ] 单元测试通过(如有)
- [ ] 手工测试所有场景通过
- [ ] 更新接口文档(如有变更)
- [ ] 提交代码并注明修复的Bug编号

---

**最后更新**: 2026-01-20
