# API接口开发流程

**适用场景**: 前端需要封装后端API接口

**前置条件**:
- [ ] 后端接口已定义（后端技术设计文档）
- [ ] 接口文档已就绪（Swagger或接口说明文档）
- [ ] 数据类型已明确

---

## 执行步骤

### 1️⃣ 准备阶段

#### 1.1 切换角色
```
/agent 前端
```

#### 1.2 搜索接口文档
```
/doc [接口模块名称]
```

查找：
- 后端技术设计文档中的接口定义
- 接口路径、请求方法、参数、响应格式
- 相关的业务规则和错误码

#### 1.3 确认接口信息
收集以下关键信息：

**基本信息**:
- 接口路径: `/api/v1/xxx`
- 请求方法: GET/POST/PUT/DELETE
- 请求参数: Query/Body/Path
- 响应格式: `Result<T>`
- 认证要求: 是否需要Token

**示例**:
```typescript
// 接口: 获取销售数据列表
// 路径: GET /api/v1/sales
// 参数:
//   - page: number (查询参数)
//   - pageSize: number (查询参数)
//   - siteCode?: string (查询参数，可选)
// 响应: PageResult<SalesData>
```

---

### 2️⃣ 类型定义阶段

#### 2.1 定义请求参数类型
文件: `src/types/[module].ts`

```typescript
/**
 * 销售数据查询参数
 */
export interface SalesQuery {
  /** 页码 */
  page: number
  /** 每页条数 */
  pageSize: number
  /** 站点编码（可选） */
  siteCode?: string
  /** 开始日期（可选） */
  startDate?: string
  /** 结束日期（可选） */
  endDate?: string
  /** 排序字段（可选） */
  sortField?: string
  /** 排序方向（可选） */
  sortOrder?: 'asc' | 'desc'
}
```

#### 2.2 定义响应数据类型
```typescript
/**
 * 销售数据
 */
export interface SalesData {
  /** 数据ID */
  id: number
  /** 交易日期时间 */
  transactionDate: string
  /** 订单号 */
  orderId: string
  /** SKU */
  sku: string
  /** 产品销售额 */
  productSales: number
  /** 货币代码 */
  currencyCode: string
  /** 站点编码 */
  siteCode: string
  /** 创建时间 */
  createdAt: string
  // ... 其他字段
}
```

#### 2.3 定义通用类型（如需要）
```typescript
/**
 * 分页结果
 */
export interface PageResult<T> {
  /** 数据列表 */
  list: T[]
  /** 总条数 */
  total: number
  /** 当前页码 */
  page: number
  /** 每页条数 */
  pageSize: number
}

/**
 * 导入结果
 */
export interface ImportResult {
  /** 成功条数 */
  successCount: number
  /** 失败条数 */
  failureCount: number
  /** 错误详情 */
  errors: ImportError[]
  /** 批次ID */
  batchId: number
}

/**
 * 导入错误
 */
export interface ImportError {
  /** 行号 */
  row: number
  /** 错误字段 */
  field: string
  /** 错误信息 */
  message: string
}
```

---

### 3️⃣ API封装阶段

#### 3.1 创建或更新API文件
文件: `src/api/[module].ts`

**文件结构**:
```typescript
import { request } from '@/utils/request'
import type {
  SalesData,
  SalesQuery,
  PageResult,
  ImportResult
} from '@/types'

/**
 * 销售数据相关API
 */

/**
 * 查询销售数据列表
 * @param params 查询参数
 * @returns 分页数据
 */
export function getSalesList(params: SalesQuery): Promise<PageResult<SalesData>> {
  return request.get('/api/v1/sales', { params })
}

/**
 * 获取销售数据详情
 * @param id 数据ID
 * @returns 销售数据
 */
export function getSalesDetail(id: number): Promise<SalesData> {
  return request.get(`/api/v1/sales/${id}`)
}

/**
 * 导入销售数据
 * @param file CSV文件
 * @param templateId 映射模板ID
 * @returns 导入结果
 */
export function importSales(
  file: File,
  templateId: number
): Promise<ImportResult> {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('templateId', String(templateId))

  return request.post('/api/v1/sales/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/**
 * 导出销售数据
 * @param params 查询参数
 * @returns Blob数据
 */
export function exportSales(params: SalesQuery): Promise<Blob> {
  return request.download('/api/v1/sales/export', params)
}

/**
 * 删除销售数据（按批次）
 * @param batchId 批次ID
 */
export function deleteSalesByBatch(batchId: number): Promise<void> {
  return request.delete(`/api/v1/sales/batch/${batchId}`)
}

/**
 * 更新销售数据
 * @param id 数据ID
 * @param data 更新数据
 */
export function updateSales(
  id: number,
  data: Partial<SalesData>
): Promise<SalesData> {
  return request.put(`/api/v1/sales/${id}`, data)
}
```

#### 3.2 特殊情况处理

**文件上传**:
```typescript
export function uploadFile(file: File): Promise<{ fileUrl: string }> {
  const formData = new FormData()
  formData.append('file', file)

  return request.post('/api/v1/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
```

**文件下载**:
```typescript
export function downloadReport(params: ReportQuery): Promise<void> {
  return request.download('/api/v1/report/export', params)
    .then(blob => {
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `report_${Date.now()}.xlsx`
      a.click()
      window.URL.revokeObjectURL(url)
    })
}
```

**轮询接口**:
```typescript
export function checkImportStatus(batchId: number): Promise<{
  status: 'pending' | 'processing' | 'completed' | 'failed'
  progress: number
}> {
  return request.get(`/api/v1/sales/import/status/${batchId}`)
}
```

---

### 4️⃣ 测试阶段

#### 4.1 创建测试文件（可选）
文件: `src/api/__tests__/[module].test.ts`

```typescript
import { describe, it, expect, vi } from 'vitest'
import { getSalesList } from '../sales'

describe('Sales API', () => {
  it('should get sales list', async () => {
    const params = { page: 1, pageSize: 20 }
    const result = await getSalesList(params)
    expect(result).toHaveProperty('list')
    expect(result).toHaveProperty('total')
  })
})
```

#### 4.2 手动测试接口
在组件中测试调用：

```vue
<script setup lang="ts">
import { onMounted } from 'vue'
import { getSalesList } from '@/api/sales'

onMounted(async () => {
  try {
    const result = await getSalesList({ page: 1, pageSize: 20 })
    console.log('接口调用成功:', result)
  } catch (error) {
    console.error('接口调用失败:', error)
  }
})
</script>
```

#### 4.3 测试清单
- [ ] 接口路径正确
- [ ] 请求方法正确
- [ ] 参数传递正确
- [ ] 响应数据结构正确
- [ ] 错误处理正常
- [ ] Token认证正常（如需要）
- [ ] 类型提示完整

---

### 5️⃣ 文档阶段

#### 5.1 添加JSDoc注释
为每个API函数添加完整的注释：
- 功能描述
- 参数说明（@param）
- 返回值说明（@returns）
- 示例代码（@example，可选）

```typescript
/**
 * 查询销售数据列表
 *
 * @param params 查询参数
 * @param params.page 页码，从1开始
 * @param params.pageSize 每页条数
 * @param params.siteCode 站点编码，可选
 * @returns 分页数据，包含列表和总数
 *
 * @example
 * const result = await getSalesList({ page: 1, pageSize: 20 })
 * console.log(result.list)
 */
```

#### 5.2 更新API索引
文件: `src/api/index.ts`

```typescript
// 销售数据
export * from './sales'

// 配送数据
export * from './shipping'

// ... 其他模块
```

---

### 6️⃣ 优化阶段

#### 6.1 请求去重（如需要）
对于频繁调用的接口，添加请求去重：

```typescript
import { useDebounceFn } from '@vueuse/core'

export const getSalesListDebounced = useDebounceFn(
  getSalesList,
  300
)
```

#### 6.2 请求缓存（如需要）
对于不常变化的数据，添加缓存：

```typescript
const cache = new Map<string, any>()

export function getCurrencyList(useCache = true): Promise<Currency[]> {
  const cacheKey = 'currency_list'

  if (useCache && cache.has(cacheKey)) {
    return Promise.resolve(cache.get(cacheKey))
  }

  return request.get('/api/v1/currencies').then(data => {
    cache.set(cacheKey, data)
    return data
  })
}
```

#### 6.3 错误重试（如需要）
对于不稳定的接口，添加重试逻辑：

```typescript
async function retryRequest<T>(
  fn: () => Promise<T>,
  maxRetries = 3
): Promise<T> {
  let lastError: any

  for (let i = 0; i < maxRetries; i++) {
    try {
      return await fn()
    } catch (error) {
      lastError = error
      if (i < maxRetries - 1) {
        await new Promise(resolve => setTimeout(resolve, 1000 * (i + 1)))
      }
    }
  }

  throw lastError
}

export function getSalesListWithRetry(params: SalesQuery): Promise<PageResult<SalesData>> {
  return retryRequest(() => getSalesList(params))
}
```

---

### 7️⃣ 提交阶段

#### 7.1 代码审查清单
- [ ] 类型定义完整准确
- [ ] 接口路径正确
- [ ] 参数传递正确
- [ ] 响应处理正确
- [ ] JSDoc注释完整
- [ ] 无拼写错误
- [ ] 遵循命名规范

#### 7.2 提交代码
```bash
git add .
git commit -m "feat: 添加[模块]API接口

- 添加了 [接口1]
- 添加了 [接口2]
- 包含完整类型定义
"
```

---

## 接口命名规范

### 查询类接口
- `getXxxList` - 获取列表
- `getXxxDetail` - 获取详情
- `getXxxById` - 通过ID获取
- `searchXxx` - 搜索

### 操作类接口
- `createXxx` - 创建
- `updateXxx` - 更新
- `deleteXxx` - 删除
- `batchDeleteXxx` - 批量删除

### 特殊操作
- `importXxx` - 导入
- `exportXxx` - 导出
- `uploadXxx` - 上传
- `downloadXxx` - 下载

---

## 常见问题

### Q1: 接口返回的数据字段和类型定义不一致怎么办？
1. 优先与后端确认，看是否是接口文档错误
2. 如果后端确认无误，更新类型定义
3. 如果是后端错误，提Bug让后端修复

### Q2: 如何处理接口的错误码？
在`utils/request.ts`的响应拦截器中统一处理：
```typescript
if (res.code !== 0) {
  // 根据错误码做不同处理
  if (res.code === 401) {
    // Token过期，跳转登录
  } else if (res.code === 403) {
    // 无权限
  }
  // ...
}
```

### Q3: 接口需要传递文件怎么办？
使用FormData：
```typescript
const formData = new FormData()
formData.append('file', file)
formData.append('otherParam', 'value')

return request.post('/api/upload', formData, {
  headers: { 'Content-Type': 'multipart/form-data' }
})
```

### Q4: 如何下载文件？
设置`responseType: 'blob'`：
```typescript
return request.get('/api/export', {
  params,
  responseType: 'blob'
}).then(blob => {
  // 创建下载链接
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'filename.xlsx'
  a.click()
})
```

---

## 参考资料

- 后端技术设计文档: `项目文档/02_设计文档/后端技术设计/后端技术设计文档.md`
- Axios文档: https://axios-http.com/
- TypeScript类型体操: https://type-challenges.github.io/

---

*流程版本: v1.0*
*最后更新: 2026年1月20日*
