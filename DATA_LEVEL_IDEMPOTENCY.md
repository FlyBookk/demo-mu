# FBA货件数据级幂等性实现说明

## 📋 需求说明

**原需求：** 文件级幂等 - 如果文件已导入过，则阻止整个文件再次导入

**新需求：** 数据级幂等 - 检查每条货件记录，只导入新记录，跳过重复记录

**示例：**
- 文件包含100条货件数据
- 其中30条已存在（重复）
- 系统应该：导入70条新数据，跳过30条重复数据
- 而不是：阻止整个文件导入

---

## 🔄 改进对比

### 改进前（文件级幂等）

```java
// 计算文件哈希值
String fileHash = calculateFileHash(file);

// 检查文件是否已导入
if (isFileAlreadyImported(shopId, fileHash)) {
    throw new BusinessException(ErrorCode.IMPORT_DUPLICATE,
        "该文件已导入过，请勿重复导入");
}
```

**问题：**
- ❌ 文件导入过一次后，无法再次导入
- ❌ 即使文件中有新数据，也会被完全阻止
- ❌ 用户需要手动筛选出新数据，重新制作Excel

### 改进后（数据级幂等）

```java
// 批量检测重复货件（基于 shipmentId + shopId）
Set<String> existingShipmentIds = batchCheckDuplicates(shipments, shopId);

// 逐条检查并保存
for (FbaShipment shipment : shipments) {
    if (existingShipmentIds.contains(shipment.getShipmentId())) {
        duplicateShipmentCount++;
        duplicateSkuCount += shipment.getItems().size();
        continue;  // 跳过重复记录
    }

    // 保存新记录
    fbaShipmentMapper.insert(shipment);
    // ...
}
```

**优点：**
- ✅ 智能识别：自动识别哪些是新数据，哪些是重复数据
- ✅ 灵活导入：同一个文件可以多次导入，只会导入新增的数据
- ✅ 用户友好：无需手动筛选数据，直接上传即可
- ✅ 详细反馈：明确告知导入了多少新数据，跳过了多少重复数据

---

## 🔧 技术实现

### 后端实现

#### 1. 幂等性检查逻辑

**检查维度：** `shipmentId` + `shopId`

```java
/**
 * 批量检测重复货件
 * 通过一次SQL查询检查所有货件是否已存在
 */
private Set<String> batchCheckDuplicates(List<FbaShipment> shipments, Long shopId) {
    Set<String> existingIds = new HashSet<>();

    // 收集所有货件单号
    Set<String> shipmentIds = new HashSet<>();
    for (FbaShipment shipment : shipments) {
        if (StringUtils.hasText(shipment.getShipmentId())) {
            shipmentIds.add(shipment.getShipmentId());
        }
    }

    if (shipmentIds.isEmpty()) {
        return existingIds;
    }

    // 批量查询已存在的货件
    LambdaQueryWrapper<FbaShipment> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(FbaShipment::getShopId, shopId)
            .in(FbaShipment::getShipmentId, shipmentIds)
            .select(FbaShipment::getShipmentId);

    List<FbaShipment> existing = fbaShipmentMapper.selectList(wrapper);

    for (FbaShipment shipment : existing) {
        existingIds.add(shipment.getShipmentId());
    }

    return existingIds;
}
```

**SQL 查询示例：**
```sql
SELECT DISTINCT shipment_id
FROM t_fba_shipment
WHERE shop_id = ?
  AND shipment_id IN ('FBA001', 'FBA002', 'FBA003', ...)
```

#### 2. 导入流程

```java
@Override
@Transactional(rollbackFor = Exception.class)
public Map<String, Object> importData(MultipartFile file) {
    // Step 1: 解析Excel文件
    List<FbaShipment> shipments = excelParser.parseExcel(file, shopId, importRecord.getId());

    // Step 2: 批量检测重复货件（一次SQL查询）
    Set<String> existingShipmentIds = batchCheckDuplicates(shipments, shopId);

    // Step 3: 保存数据（跳过重复记录）
    for (FbaShipment shipment : shipments) {
        totalSkuCount += shipment.getItems().size();

        // 检查是否重复
        if (existingShipmentIds.contains(shipment.getShipmentId())) {
            duplicateShipmentCount++;
            duplicateSkuCount += shipment.getItems().size();
            continue;  // 跳过
        }

        // 保存新记录
        fbaShipmentMapper.insert(shipment);
        for (FbaShipmentItem item : shipment.getItems()) {
            item.setShipmentId(shipment.getId());
            fbaShipmentItemMapper.insert(item);
        }

        successShipmentCount++;
        successSkuCount += shipment.getItems().size();
    }

    // 返回统计结果
    result.put("totalCount", totalSkuCount);
    result.put("successCount", successSkuCount);
    result.put("duplicateCount", duplicateSkuCount);
    result.put("duplicateShipmentCount", duplicateShipmentCount);
    result.put("shipmentCount", successShipmentCount);

    return result;
}
```

#### 3. 返回结果结构

```json
{
  "batchNo": "FBA-1738394637000-a1b2c3d4",
  "totalCount": 100,           // 总SKU数
  "successCount": 70,          // 成功导入的SKU数
  "failCount": 0,              // 失败的SKU数
  "duplicateCount": 30,        // 重复的SKU数（已跳过）
  "duplicateShipmentCount": 10, // 重复的货件数
  "shipmentCount": 20,         // 成功导入的货件数
  "errors": []
}
```

### 前端实现

#### 1. 导入结果展示

```vue
<a-descriptions :column="3" size="small" bordered>
  <a-descriptions-item label="总SKU数">{{ importResult.totalSkuCount }}</a-descriptions-item>
  <a-descriptions-item label="成功SKU">
    <span class="success-text">{{ importResult.successSkuCount }}</span>
  </a-descriptions-item>
  <a-descriptions-item label="失败SKU">
    <span class="error-text">{{ importResult.failSkuCount }}</span>
  </a-descriptions-item>
  <a-descriptions-item label="重复SKU（已跳过）">
    <span class="warning-text">{{ importResult.duplicateSkuCount || 0 }}</span>
  </a-descriptions-item>
  <a-descriptions-item label="成功货件">
    <span class="success-text">{{ importResult.totalShipmentCount }}</span>
  </a-descriptions-item>
</a-descriptions>
```

#### 2. 文件详情展示

```vue
<template #bodyCell="{ column, record }">
  <template v-if="column.key === 'message'">
    <template v-if="record.result">
      导入 {{ record.result.shipmentCount }} 个货件
      <span v-if="record.result.duplicateCount > 0" class="warning-text">
        (跳过 {{ record.result.duplicateCount }} 个重复SKU)
      </span>
    </template>
  </template>
</template>
```

#### 3. TypeScript 类型定义

```typescript
// FBA货件导入结果
export interface FbaShipmentImportResult {
  batchNo: string
  totalCount: number        // 总SKU数
  successCount: number      // 成功导入的SKU数
  failCount: number         // 失败的SKU数
  duplicateCount: number    // 重复的SKU数（已跳过）
  duplicateShipmentCount?: number  // 重复的货件数
  shipmentCount: number     // 成功导入的货件数
  errors: string[]
}

// FBA货件批量导入结果
export interface FbaShipmentBatchImportResult {
  totalFiles: number        // 总文件数
  successFiles: number      // 成功文件数
  failFiles: number         // 失败文件数
  totalSkuCount: number     // 总SKU数
  successSkuCount: number   // 成功SKU数
  failSkuCount: number      // 失败SKU数
  duplicateSkuCount: number // 重复SKU数（已跳过）
  totalShipmentCount: number // 总货件数
  fileResults: Array<{
    fileName: string
    status: 'success' | 'fail'
    message?: string
    result?: FbaShipmentImportResult
  }>
}
```

---

## 📊 使用场景

### 场景1：首次导入

**操作：** 上传包含100条货件的Excel文件

**结果：**
```
总SKU数: 100
成功SKU: 100
重复SKU: 0
成功货件: 30
```

### 场景2：重复导入（完全重复）

**操作：** 再次上传相同的Excel文件

**结果：**
```
总SKU数: 100
成功SKU: 0
重复SKU: 100 (已跳过)
成功货件: 0
```

**说明：** 所有数据都已存在，全部跳过，不会报错

### 场景3：部分重复导入

**操作：** 上传包含150条货件的Excel文件（其中100条已存在，50条是新的）

**结果：**
```
总SKU数: 150
成功SKU: 50
重复SKU: 100 (已跳过)
成功货件: 15
```

**说明：** 自动识别并导入50条新数据，跳过100条重复数据

### 场景4：批量导入多个文件

**操作：** 同时上传3个Excel文件

**结果：**
```
总文件数: 3
成功文件: 3
失败文件: 0
总SKU数: 300
成功SKU: 200
重复SKU: 100 (已跳过)
成功货件: 60
```

---

## 🧪 测试验证

### 测试步骤

1. **准备测试数据**
   - 创建 `test1.xlsx`：包含货件 FBA001-FBA010（共30个SKU）
   - 创建 `test2.xlsx`：包含货件 FBA006-FBA015（共30个SKU，其中5个重复）

2. **测试首次导入**
   ```bash
   # 导入 test1.xlsx
   # 预期结果：成功导入10个货件，30个SKU
   ```

3. **测试完全重复导入**
   ```bash
   # 再次导入 test1.xlsx
   # 预期结果：0个新货件，30个重复SKU（已跳过）
   ```

4. **测试部分重复导入**
   ```bash
   # 导入 test2.xlsx
   # 预期结果：成功导入5个新货件（FBA011-FBA015），跳过5个重复货件（FBA006-FBA010）
   ```

5. **验证数据库**
   ```sql
   SELECT COUNT(*) FROM t_fba_shipment;
   -- 应该是 15 条记录（10 + 5）

   SELECT shipment_id FROM t_fba_shipment ORDER BY shipment_id;
   -- 应该包含 FBA001 到 FBA015
   ```

---

## 🔍 性能优化

### 批量检测优化

**优化前：** 逐条查询数据库
```java
for (FbaShipment shipment : shipments) {
    // 每条记录都查询一次数据库
    boolean exists = checkExists(shipment.getShipmentId());
}
// 100条记录 = 100次数据库查询
```

**优化后：** 批量查询
```java
// 一次查询检查所有记录
Set<String> existingIds = batchCheckDuplicates(shipments, shopId);
// 100条记录 = 1次数据库查询
```

**性能提升：**
- 100条记录：从100次查询 → 1次查询
- 1000条记录：从1000次查询 → 1次查询
- 大幅减少数据库压力和网络开销

---

## 📝 注意事项

### 1. 唯一性判断

**判断依据：** `shipmentId` + `shopId`

- 同一店铺下，`shipmentId` 相同的记录视为重复
- 不同店铺下，即使 `shipmentId` 相同，也不视为重复

### 2. 文件哈希保留

虽然不再用于阻止导入，但仍然保留 `file_hash` 字段用于：
- 记录文件标识
- 审计追踪
- 未来可能的文件去重分析

### 3. 事务处理

- 每个文件的导入在一个事务中
- 如果保存过程中出现异常，会回滚整个文件的导入
- 重复检测不会触发回滚，只是跳过记录

### 4. 错误处理

- 重复记录：不视为错误，正常跳过
- 解析错误：记录到 `errors` 数组，继续处理其他记录
- 保存失败：记录到 `errors` 数组，继续处理其他记录

---

## 🎯 总结

### 核心改进

1. **从文件级幂等 → 数据级幂等**
   - 更智能：自动识别新旧数据
   - 更灵活：支持重复导入
   - 更友好：无需手动筛选

2. **性能优化**
   - 批量检测：减少数据库查询次数
   - 一次SQL：检查所有记录

3. **用户体验**
   - 详细反馈：明确告知导入和跳过的数量
   - 容错性强：重复数据不会导致失败

### 影响范围

**后端：**
- ✅ `FbaShipmentServiceImpl.java` - 核心逻辑修改
- ✅ 移除 `isFileAlreadyImported()` 方法
- ✅ 返回结果增加 `duplicateSkuCount` 字段

**前端：**
- ✅ `import/index.vue` - UI展示更新
- ✅ `fbaShipment.ts` - 类型定义更新
- ✅ 移除 `skippedFiles` 相关逻辑

**数据库：**
- ✅ `file_hash` 字段保留（用于记录，不用于阻止）

---

**现在可以测试数据级幂等功能了！** 🚀
