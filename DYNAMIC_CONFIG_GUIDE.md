# FBA货件动态配置功能说明

## 📋 功能概述

将国家和店铺名称从**硬编码**改为**动态配置**，实现自动化管理。

---

## 🎯 改进对比

### 改进前（硬编码）

```typescript
// 写死的国家选项
export const FbaShipmentCountryOptions = [
  { label: '英国', value: '英国' },
  { label: '美国', value: '美国' },
  { label: '加拿大', value: '加拿大' },
  { label: '德国', value: '德国' },
  { label: '法国', value: '法国' },
  { label: '日本', value: '日本' }
]

// 店铺名称：手动输入
<a-input v-model:value="searchForm.shopName" placeholder="店铺名称" />
```

**问题：**
- ❌ 新增国家需要修改代码
- ❌ 店铺名称容易输入错误
- ❌ 无法自动适应业务变化

### 改进后（动态配置）

```typescript
// 从数据库动态获取
const countryOptions = ref<Array<{ label: string; value: string }>>([])
const shopNameOptions = ref<Array<{ label: string; value: string }>>([])

// 页面加载时获取
onMounted(() => {
  fetchCountries()
  fetchShopNames()
})

// 下拉选择 + 搜索过滤
<a-select
  v-model:value="searchForm.country"
  placeholder="国家"
  show-search
  :filter-option="filterOption"
>
  <a-select-option v-for="option in countryOptions" :key="option.value">
    {{ option.label }}
  </a-select-option>
</a-select>
```

**优点：**
- ✅ 自动更新：导入新数据后自动出现
- ✅ 数据准确：只显示实际存在的值
- ✅ 用户体验：支持搜索，避免输入错误
- ✅ 零维护：无需修改代码

---

## 🔧 技术实现

### 后端实现

#### 1. Service 层新增方法

```java
@Override
public List<String> getCountryList() {
    Long shopId = ShopContext.requireShopId();

    LambdaQueryWrapper<FbaShipment> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(FbaShipment::getShopId, shopId)
            .select(FbaShipment::getCountry)
            .groupBy(FbaShipment::getCountry)
            .orderByAsc(FbaShipment::getCountry);

    List<FbaShipment> list = fbaShipmentMapper.selectList(wrapper);
    return list.stream()
            .map(FbaShipment::getCountry)
            .filter(StringUtils::hasText)
            .distinct()
            .collect(Collectors.toList());
}

@Override
public List<String> getShopNameList() {
    // 类似实现
}
```

**SQL 查询示例：**
```sql
SELECT DISTINCT country
FROM t_fba_shipment
WHERE shop_id = ?
  AND country IS NOT NULL
  AND country != ''
GROUP BY country
ORDER BY country ASC;
```

#### 2. Controller 层新增接口

```java
@Operation(summary = "获取国家列表")
@GetMapping("/countries")
public Result<List<String>> getCountryList() {
    List<String> countries = fbaShipmentService.getCountryList();
    return Result.success(countries);
}

@Operation(summary = "获取店铺列表")
@GetMapping("/shop-names")
public Result<List<String>> getShopNameList() {
    List<String> shopNames = fbaShipmentService.getShopNameList();
    return Result.success(shopNames);
}
```

**API 端点：**
- `GET /api/v1/business/fba-shipment/countries`
- `GET /api/v1/business/fba-shipment/shop-names`

### 前端实现

#### 1. API 封装

```typescript
// src/api/fbaShipment.ts
export function getFbaShipmentCountries() {
  return request.get<string[]>(`${BASE_URL}/countries`)
}

export function getFbaShipmentShopNames() {
  return request.get<string[]>(`${BASE_URL}/shop-names`)
}
```

#### 2. 页面集成

```typescript
// 数据定义
const countryOptions = ref<Array<{ label: string; value: string }>>([])
const shopNameOptions = ref<Array<{ label: string; value: string }>>([])

// 获取选项
async function fetchCountries() {
  try {
    const res = await getFbaShipmentCountries()
    countryOptions.value = (res.data || []).map(country => ({
      label: country,
      value: country
    }))
  } catch (error) {
    console.error('获取国家列表失败:', error)
  }
}

// 搜索过滤
function filterOption(input: string, option: any) {
  return option.value.toLowerCase().includes(input.toLowerCase())
}

// 页面加载时获取
onMounted(() => {
  fetchCountries()
  fetchShopNames()
})
```

---

## 📊 数据来源

### 数据表：t_fba_shipment

| 字段 | 说明 | 示例 |
|------|------|------|
| shop_name | 店铺名称 | 慕声欧洲-UK |
| country | 国家 | 英国 |

### 数据来源：Excel 导入

Excel 文件中的"店铺"和"国家"列会被解析并保存到数据库。

**示例数据：**
```
店铺: 慕声欧洲-UK, 慕声美国-US, 慕声德国-DE
国家: 英国, 美国, 德国, 法国, 日本
```

---

## 🧪 测试验证

### 测试步骤

1. **导入测试数据**
   - 上传包含不同国家和店铺的 Excel 文件
   - 例如：英国、美国、德国

2. **验证货件列表页**
   - 访问：http://localhost:3000/fba-shipment/list
   - 点击"国家"下拉框
   - ✅ 应该看到：英国、美国、德国
   - 点击"店铺名称"下拉框
   - ✅ 应该看到实际导入的店铺名称

3. **验证 SKU 明细页**
   - 访问：http://localhost:3000/fba-shipment/detail
   - 验证国家和店铺下拉框
   - ✅ 应该显示相同的选项

4. **测试搜索功能**
   - 在下拉框中输入关键字
   - ✅ 应该实时过滤选项

5. **测试自动更新**
   - 导入包含新国家的数据（如：加拿大）
   - 刷新页面
   - ✅ 下拉框应该自动包含"加拿大"

---

## 🎨 用户体验优化

### 1. 搜索过滤

```vue
<a-select
  show-search
  :filter-option="filterOption"
>
```

**效果：**
- 用户可以输入关键字快速查找
- 支持模糊匹配（不区分大小写）

### 2. 清空选项

```vue
<a-select allow-clear>
```

**效果：**
- 用户可以一键清空选择
- 方便重置筛选条件

### 3. 占位提示

```vue
<a-select placeholder="国家">
<a-select placeholder="店铺名称">
```

**效果：**
- 清晰的提示信息
- 提升用户体验

---

## 🔄 数据更新机制

### 自动更新流程

```
1. 用户导入新的 Excel 文件
   ↓
2. 系统解析并保存到数据库
   - INSERT INTO t_fba_shipment (shop_name, country, ...)
   ↓
3. 用户刷新页面或重新打开页面
   ↓
4. 前端调用 API 获取最新选项
   ↓
5. 下拉框自动显示新增的国家/店铺
```

### 无需手动维护

- ✅ 不需要修改代码
- ✅ 不需要重启服务
- ✅ 不需要配置文件
- ✅ 完全自动化

---

## 💡 扩展建议

### 1. 添加缓存（可选）

如果数据量大，可以考虑添加缓存：

```java
@Cacheable(value = "fbaShipmentCountries", key = "#shopId")
public List<String> getCountryList() {
    // ...
}
```

### 2. 添加统计信息（可选）

在下拉选项中显示数量：

```
英国 (125)
美国 (89)
德国 (56)
```

### 3. 支持多语言（可选）

国家名称支持中英文切换：

```
英国 / United Kingdom
美国 / United States
```

---

## 📝 总结

### 核心优势

1. **自动化**：无需手动维护配置
2. **准确性**：数据来源于实际导入
3. **灵活性**：自动适应业务变化
4. **易用性**：搜索过滤提升体验

### 影响范围

- ✅ 货件列表页（国家、店铺筛选）
- ✅ SKU明细页（国家、店铺筛选）
- ✅ 后端 API（2个新接口）
- ✅ 前端 API（2个新方法）

### 编译状态

- ✅ 后端编译：BUILD SUCCESS
- ✅ 前端类型：无错误

---

**现在刷新浏览器页面即可体验动态配置功能！** 🚀
