# FBA货件明细模块后端自测方案

## 测试时间
2026-01-21

## 测试目标
验证FBA货件明细模块后端接口功能完整性和正确性

---

## 前置条件

### 1. 数据库准备
```sql
-- 执行建表SQL
source musheng-tax-system/sql/fba_shipment_detail.sql;

-- 验证表创建成功
SHOW TABLES LIKE 't_fba_shipment_detail';

-- 查看表结构
DESC t_fba_shipment_detail;

-- 查看索引
SHOW INDEX FROM t_fba_shipment_detail;
```

### 2. 后端服务启动
```bash
# 确保后端服务运行在 localhost:8080
# 检查服务状态
curl http://localhost:8080/actuator/health
```

### 3. 获取认证Token
```bash
# 登录获取token（根据实际登录接口调整）
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# 保存返回的token用于后续测试
export TOKEN="your_token_here"
```

---

## 测试用例

### 测试1: CSV文件导入（核心功能）

#### 1.1 正常导入测试
**目的**: 验证GBK编码CSV文件导入功能

**请求**:
```bash
curl -X POST http://localhost:8080/api/v1/business/fba-shipment/import \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@/Users/wanhua/Documents/慕声/musheng/慕声加拿大2025年3-12月FBA货件明细表.csv"
```

**预期结果**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "totalCount": 17,
    "successCount": 17,
    "failCount": 0,
    "duplicateCount": 0,
    "errors": [],
    "batchNo": "FBA-1737449123456-abc12345"
  }
}
```

**验证点**:
- [ ] 返回code=200
- [ ] totalCount等于CSV文件数据行数
- [ ] successCount = totalCount
- [ ] failCount = 0
- [ ] duplicateCount = 0
- [ ] batchNo格式正确（FBA-时间戳-UUID）

#### 1.2 重复导入测试
**目的**: 验证重复数据检测机制

**请求**: 再次执行1.1的导入命令

**预期结果**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "totalCount": 17,
    "successCount": 0,
    "failCount": 0,
    "duplicateCount": 17,
    "errors": [
      "重复: shipmentId=FBA19334SZXM",
      "重复: shipmentId=FBA190PML7KM",
      "..."
    ],
    "batchNo": "FBA-1737449123457-def45678"
  }
}
```

**验证点**:
- [ ] duplicateCount = 17（所有记录都重复）
- [ ] successCount = 0（没有新记录插入）
- [ ] errors数组包含重复记录的货件编号

#### 1.3 数据库验证
```sql
-- 查看导入的数据总数
SELECT COUNT(*) FROM t_fba_shipment_detail;
-- 预期: 17

-- 查看导入批次
SELECT DISTINCT import_batch_id FROM t_fba_shipment_detail;
-- 预期: 只有第一次导入的batch_id

-- 查看具体数据
SELECT shipment_name, shipment_id, status, created_date, sku_count
FROM t_fba_shipment_detail
LIMIT 5;

-- 验证字段解析正确
SELECT * FROM t_fba_shipment_detail WHERE shipment_id = 'FBA19334SZXM';
-- 验证:
-- - shipment_name = 'RP251030001'
-- - created_date 正确解析为datetime
-- - receiving_address = 'YEG2'
-- - sku_count = 24
-- - expected_quantity = 304
-- - found_quantity = 304
-- - status = '已完成'
```

---

### 测试2: 列表查询

#### 2.1 无条件分页查询
```bash
curl -X GET "http://localhost:8080/api/v1/business/fba-shipment?page=1&size=10" \
  -H "Authorization: Bearer $TOKEN"
```

**预期结果**:
```json
{
  "code": 200,
  "data": {
    "records": [...],
    "total": 17,
    "page": 1,
    "size": 10
  }
}
```

**验证点**:
- [ ] total = 17
- [ ] records数组长度 = 10
- [ ] 按create_time降序排列

#### 2.2 按状态筛选
```bash
curl -X GET "http://localhost:8080/api/v1/business/fba-shipment?status=已完成&page=1&size=10" \
  -H "Authorization: Bearer $TOKEN"
```

**验证点**:
- [ ] 返回的所有记录status都是"已完成"

#### 2.3 按货件编号模糊查询
```bash
curl -X GET "http://localhost:8080/api/v1/business/fba-shipment?shipmentId=FBA19&page=1&size=10" \
  -H "Authorization: Bearer $TOKEN"
```

**验证点**:
- [ ] 返回的记录货件编号都包含"FBA19"

#### 2.4 按收货地址筛选
```bash
curl -X GET "http://localhost:8080/api/v1/business/fba-shipment?receivingAddress=YEG2&page=1&size=10" \
  -H "Authorization: Bearer $TOKEN"
```

**验证点**:
- [ ] 返回的所有记录receiving_address都是"YEG2"

---

### 测试3: 详情查询

```bash
# 先查询列表获取一个ID
curl -X GET "http://localhost:8080/api/v1/business/fba-shipment?page=1&size=1" \
  -H "Authorization: Bearer $TOKEN"

# 使用获取的ID查询详情（假设ID=1）
curl -X GET "http://localhost:8080/api/v1/business/fba-shipment/1" \
  -H "Authorization: Bearer $TOKEN"
```

**预期结果**:
```json
{
  "code": 200,
  "data": {
    "id": 1,
    "shipmentName": "RP251030001",
    "shipmentId": "FBA19334SZXM",
    "createdDate": "2025-10-30T16:04:00",
    "lastUpdated": "2025-12-27T10:12:00",
    "receivingAddress": "YEG2",
    "skuCount": 24,
    "expectedQuantity": 304,
    "foundQuantity": 304,
    "status": "已完成",
    "importBatchId": 1,
    "createTime": "...",
    "updateTime": "...",
    "createBy": null,
    "updateBy": null
  }
}
```

**验证点**:
- [ ] 返回完整的货件明细信息
- [ ] 日期字段格式正确
- [ ] 数量字段类型正确

---

### 测试4: 新增记录

```bash
curl -X POST http://localhost:8080/api/v1/business/fba-shipment \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "shipmentName": "TEST001",
    "shipmentId": "TEST-FBA-001",
    "createdDate": "2025-01-21T10:00:00",
    "lastUpdated": "2025-01-21T12:00:00",
    "receivingAddress": "TEST1",
    "skuCount": 10,
    "expectedQuantity": 100,
    "foundQuantity": 98,
    "status": "测试中"
  }'
```

**预期结果**:
```json
{
  "code": 200,
  "data": 18  // 新记录的ID
}
```

**验证点**:
- [ ] 返回新记录的ID
- [ ] 数据库中能查到该记录
- [ ] create_time和update_time自动填充

#### 测试4.1: 重复货件编号新增（应失败）
```bash
curl -X POST http://localhost:8080/api/v1/business/fba-shipment \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "shipmentName": "TEST002",
    "shipmentId": "TEST-FBA-001",
    "receivingAddress": "TEST2"
  }'
```

**预期结果**:
```json
{
  "code": 30004,
  "message": "货件编号已存在"
}
```

---

### 测试5: 更新记录

```bash
# 更新ID=18的记录
curl -X PUT http://localhost:8080/api/v1/business/fba-shipment/18 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "shipmentName": "TEST001-UPDATED",
    "shipmentId": "TEST-FBA-001",
    "status": "已完成",
    "foundQuantity": 100
  }'
```

**预期结果**:
```json
{
  "code": 200,
  "message": "success"
}
```

**验证点**:
- [ ] 记录更新成功
- [ ] update_time自动更新
- [ ] shipmentName变为"TEST001-UPDATED"
- [ ] foundQuantity变为100

---

### 测试6: 删除记录

#### 6.1 单条删除
```bash
curl -X DELETE http://localhost:8080/api/v1/business/fba-shipment/18 \
  -H "Authorization: Bearer $TOKEN"
```

**预期结果**:
```json
{
  "code": 200,
  "message": "success"
}
```

**验证点**:
- [ ] 记录删除成功
- [ ] 数据库中查不到ID=18的记录

#### 6.2 批量删除
```bash
# 先新增几条测试数据
curl -X POST http://localhost:8080/api/v1/business/fba-shipment \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"shipmentName":"BATCH-TEST-1","shipmentId":"BATCH-001","receivingAddress":"TEST1"}'

curl -X POST http://localhost:8080/api/v1/business/fba-shipment \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"shipmentName":"BATCH-TEST-2","shipmentId":"BATCH-002","receivingAddress":"TEST1"}'

# 批量删除（假设ID为19和20）
curl -X POST http://localhost:8080/api/v1/business/fba-shipment/batch-delete \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '[19, 20]'
```

**验证点**:
- [ ] 批量删除成功
- [ ] 数据库中查不到ID=19和20的记录

---

### 测试7: 统计汇总

```bash
curl -X GET "http://localhost:8080/api/v1/business/fba-shipment/summary" \
  -H "Authorization: Bearer $TOKEN"
```

**预期结果**:
```json
{
  "code": 200,
  "data": {
    "totalShipments": 17,
    "totalSkuCount": 449,
    "totalExpectedQuantity": 10214,
    "totalFoundQuantity": 10213
  }
}
```

**验证点**:
- [ ] totalShipments = 17（原始数据）
- [ ] 统计数据正确

#### 7.1 按状态统计
```bash
curl -X GET "http://localhost:8080/api/v1/business/fba-shipment/summary?status=已完成" \
  -H "Authorization: Bearer $TOKEN"
```

#### 7.2 按日期范围统计
```bash
curl -X GET "http://localhost:8080/api/v1/business/fba-shipment/summary?startDate=2025-07-01&endDate=2025-12-31" \
  -H "Authorization: Bearer $TOKEN"
```

---

### 测试8: 导出Excel

```bash
curl -X GET "http://localhost:8080/api/v1/business/fba-shipment/export" \
  -H "Authorization: Bearer $TOKEN" \
  -o fba_shipment_export.xlsx
```

**验证点**:
- [ ] 文件下载成功
- [ ] 文件可以用Excel打开
- [ ] 数据完整（17行数据）
- [ ] 表头正确（货件名称、货件编号、创建时间...）

---

## 性能测试

### 测试9: 批量导入性能

**目的**: 验证1000条数据导入时间 < 10秒

**准备**: 复制CSV文件数据到1000行

```bash
time curl -X POST http://localhost:8080/api/v1/business/fba-shipment/import \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@test_1000_rows.csv"
```

**验证点**:
- [ ] 导入时间 < 10秒
- [ ] 无N+1查询问题（查看日志）
- [ ] 批量插入日志正常（每批500条）

---

## 日志验证

查看后端日志，确认：
- [ ] CSV编码检测日志: "Successfully detected headers with charset: GBK"
- [ ] 解析完成日志: "解析完成: 共17行, 成功解析17条"
- [ ] 重复检测日志: "批量重复检测: 找到X条已存在记录"
- [ ] 批量插入日志: "批量插入记录 1-17/17"
- [ ] 导入完成日志: "FBA货件明细导入完成: total=17, success=17, fail=0, duplicate=0"

---

## 测试结果记录

| 测试用例 | 测试结果 | 问题描述 | 备注 |
|---------|---------|---------|------|
| CSV导入-正常 | ⏳ 待测试 | | |
| CSV导入-重复 | ⏳ 待测试 | | |
| 列表查询 | ⏳ 待测试 | | |
| 详情查询 | ⏳ 待测试 | | |
| 新增记录 | ⏳ 待测试 | | |
| 更新记录 | ⏳ 待测试 | | |
| 删除记录 | ⏳ 待测试 | | |
| 批量删除 | ⏳ 待测试 | | |
| 统计汇总 | ⏳ 待测试 | | |
| 导出Excel | ⏳ 待测试 | | |
| 性能测试 | ⏳ 待测试 | | |

---

## 快速测试脚本

创建 `test_fba_shipment.sh`:
```bash
#!/bin/bash

# 配置
BASE_URL="http://localhost:8080/api/v1/business/fba-shipment"
TOKEN="your_token_here"
CSV_FILE="/Users/wanhua/Documents/慕声/musheng/慕声加拿大2025年3-12月FBA货件明细表.csv"

echo "=== 测试1: CSV导入 ==="
curl -X POST "$BASE_URL/import" \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@$CSV_FILE"

echo -e "\n\n=== 测试2: 列表查询 ==="
curl -X GET "$BASE_URL?page=1&size=5" \
  -H "Authorization: Bearer $TOKEN"

echo -e "\n\n=== 测试3: 统计汇总 ==="
curl -X GET "$BASE_URL/summary" \
  -H "Authorization: Bearer $TOKEN"

echo -e "\n\n测试完成！"
```

---

## 预期问题及解决方案

### 问题1: 找不到Bean定义
**现象**: NoSuchBeanDefinitionException
**原因**: Controller/Service未被Spring扫描
**解决**: 检查包路径是否正确，确保在启动类扫描范围内

### 问题2: 日期解析失败
**现象**: 日期字段为null
**原因**: 中文日期格式Hutool解析失败
**解决**: 检查CSV文件日期格式，调整parseDate方法

### 问题3: 编码识别错误
**现象**: 中文显示乱码
**原因**: GBK编码识别失败
**解决**: 检查CSV文件BOM，调整编码检测逻辑

---

测试负责人: Backend Agent
测试日期: 2026-01-21
