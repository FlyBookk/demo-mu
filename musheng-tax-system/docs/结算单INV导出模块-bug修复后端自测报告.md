# 结算单与 INV 导出缺陷修复 - 接口测试指南

> 对应 bugfix spec: `.kiro/specs/settlement-inv-export-fix/`
> 修复内容: 结算单 buyerAddress 缺失 + 结算单/INV 列宽固定不自适应

## 前置条件

1. 启动服务: `cd musheng-tax-system && mvn spring-boot:run -pl musheng-web`
2. 服务地址: `http://localhost:8080/api`
3. 需要有效的 `Authorization: Bearer <token>` 请求头（先登录获取）
4. 需要在请求头中携带 `Shop-Id: <shopId>`（店铺数据隔离）

---

## 测试流程

### Step 1: 导入结算数据

**接口**: `POST /api/v1/business/document/settlement-data/import`

```bash
curl -X POST http://localhost:8080/api/v1/business/document/settlement-data/import \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -H "Shop-Id: 1" \
  -d '{
    "periodStart": "2025-09-02",
    "periodEnd": "2025-09-08",
    "items": [
      {
        "siteCode": "USD",
        "msku": "MSUS-BLUETOOTH-SPEAKER-PRO-MAX-001",
        "currency": "USD",
        "unitPrice": 25.99,
        "quantity": 10
      },
      {
        "siteCode": "CAD",
        "msku": "MSCA-BLUETOOTH-SPEAKER-PRO-MAX-001",
        "currency": "CAD",
        "unitPrice": 32.50,
        "quantity": 8
      },
      {
        "siteCode": "GBP",
        "msku": "MSUK-BLUETOOTH-SPEAKER-PRO-MAX-001",
        "currency": "GBP",
        "unitPrice": 19.99,
        "quantity": 12
      },
      {
        "siteCode": "EUR",
        "msku": "MSEU-BLUETOOTH-SPEAKER-PRO-MAX-001",
        "currency": "EUR",
        "unitPrice": 22.50,
        "quantity": 6
      }
    ]
  }'
```

**预期响应**:
```json
{
  "code": 200,
  "data": 4,
  "message": "success"
}
```

---

### Step 2: 生成结算单

**接口**: `POST /api/v1/business/document/settlement/generate`

> 注意: `shipmentIds` 需要填入数据库中已有的 FBA 货件 ID，如果没有货件数据可以先查询或使用已有数据

```bash
curl -X POST http://localhost:8080/api/v1/business/document/settlement/generate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -H "Shop-Id: 1" \
  -d '{
    "periodStart": "2025-09-02",
    "periodEnd": "2025-09-08",
    "shipmentIds": [1, 2, 3]
  }'
```

**预期响应** (4份结算单，按站点拆分):
```json
{
  "code": 200,
  "data": [
    {
      "id": 101,
      "documentNo": "20250902001-东莞市慕声商贸有限公司-Hong Kong Andeo Group Limited-结算单",
      "buyerName": "东莞市慕声商贸有限公司",
      "buyerAddress": "广东省东莞市虎门镇连升路82号虎门万达广场2栋606房",
      "sellerName": "Hong Kong Andeo Group Limited",
      "siteCode": "USD"
    },
    ...
  ]
}
```

**✅ 验证点 1 (bugfix 3.1)**: 响应中每份结算单的 `buyerAddress` 字段必须等于 `"广东省东莞市虎门镇连升路82号虎门万达广场2栋606房"`，不能为 `null` 或空字符串。

记录返回的结算单 ID（如 101, 102, 103, 104），后续步骤使用。

---

### Step 3: 查询结算单详情（验证 buyerAddress）

**接口**: `GET /api/v1/business/document/settlement/{id}`

```bash
curl -X GET http://localhost:8080/api/v1/business/document/settlement/101 \
  -H "Authorization: Bearer <token>" \
  -H "Shop-Id: 1"
```

**预期响应**:
```json
{
  "code": 200,
  "data": {
    "id": 101,
    "buyerName": "东莞市慕声商贸有限公司",
    "buyerAddress": "广东省东莞市虎门镇连升路82号虎门万达广场2栋606房",
    "sellerName": "Hong Kong Andeo Group Limited",
    ...
  }
}
```

**✅ 验证点 2**: `buyerAddress` 不为空，值正确。

---

### Step 4: 导出结算单 Excel（验证列宽自适应）

**接口**: `GET /api/v1/business/document/export/settlement/{id}`

```bash
curl -X GET http://localhost:8080/api/v1/business/document/export/settlement/101 \
  -H "Authorization: Bearer <token>" \
  -H "Shop-Id: 1" \
  -o settlement_101.xlsx
```

**✅ 验证点 3 (bugfix 3.2)**: 用 Excel 打开 `settlement_101.xlsx`，检查:
- 第2行（B2:F2 合并单元格）显示 `广东省东莞市虎门镇连升路82号虎门万达广场2栋606房`，不为空白
- 各列宽度自适应内容，MSKU 列（如 `MSUS-BLUETOOTH-SPEAKER-PRO-MAX-001`）内容完整显示，不被截断
- 印章图片（慕声红章、香港蓝章）位置和大小正常，未发生偏移或变形

---

### Step 5: 生成 INV

**接口**: `POST /api/v1/business/document/inv/generate`

```bash
curl -X POST http://localhost:8080/api/v1/business/document/inv/generate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -H "Shop-Id: 1" \
  -d '[101, 102, 103, 104]'
```

**预期响应** (4份 INV):
```json
{
  "code": 200,
  "data": [
    {
      "id": 201,
      "documentNo": "20250902001-Hong Kong Andeo Group Limited-Dongguan Musheng Trade Co., Ltd.-invoice",
      "sellerName": "Hong Kong Andeo Group Limited",
      "buyerName": "Dongguan Musheng Trade Co., Ltd.",
      "bankName": "Bank of China (Hong Kong) Limited",
      "bankAccount": "012-878-0-001234-5",
      "swiftCode": "BKCHHKHH"
    },
    ...
  ]
}
```

记录返回的 INV ID（如 201, 202, 203, 204）。

---

### Step 6: 导出 INV Excel（验证列宽自适应）

**接口**: `GET /api/v1/business/document/export/inv/{id}`

```bash
curl -X GET http://localhost:8080/api/v1/business/document/export/inv/201 \
  -H "Authorization: Bearer <token>" \
  -H "Shop-Id: 1" \
  -o inv_201.xlsx
```

**✅ 验证点 4 (bugfix 3.3)**: 用 Excel 打开 `inv_201.xlsx`，检查:
- 银行地址 `Bank of China Tower, 1 Garden Road, Central, Hong Kong` 完整显示，不被截断
- MSKU 列内容完整显示
- 印章图片（慕声红章）位置和大小正常

---

### Step 7: 批量导出验证（可选）

**接口**: `GET /api/v1/business/document/export/period`

```bash
curl -X GET "http://localhost:8080/api/v1/business/document/export/period?periodStart=2025-09-02&periodEnd=2025-09-08" \
  -H "Authorization: Bearer <token>" \
  -H "Shop-Id: 1" \
  -o period_export.zip
```

**✅ 验证点 5**: 解压 ZIP，共8个文件（4份结算单 + 4份 INV），每个文件的列宽和地址均正确。

---

## 回归验证（确认无副作用）

### 验证 PO 导出不受影响

```bash
# 先查询已有 PO 的 ID
curl -X GET "http://localhost:8080/api/v1/business/document/list?documentType=PO&pageNum=1&pageSize=5" \
  -H "Authorization: Bearer <token>" \
  -H "Shop-Id: 1"

# 导出 PO
curl -X GET http://localhost:8080/api/v1/business/document/export/po/<po_id> \
  -H "Authorization: Bearer <token>" \
  -H "Shop-Id: 1" \
  -o po_export.xlsx
```

**✅ 验证点 6**: PO Excel 正常导出，列宽自适应，印章正常（PO 本来就有 autoFitColumns，确认未被破坏）。

### 验证 DN 导出不受影响

```bash
curl -X GET http://localhost:8080/api/v1/business/document/export/dn/<dn_id> \
  -H "Authorization: Bearer <token>" \
  -H "Shop-Id: 1" \
  -o dn_export.xlsx
```

**✅ 验证点 7**: DN Excel 正常导出，列宽自适应，印章正常。

---

## 验收标准汇总

| # | 验证点 | 对应 bugfix | 通过标准 |
|---|--------|------------|---------|
| 1 | 生成结算单响应中 buyerAddress 不为 null | 3.1 | `buyerAddress == "广东省东莞市虎门镇连升路82号虎门万达广场2栋606房"` |
| 2 | 结算单详情接口 buyerAddress 正确 | 3.1 | 同上 |
| 3 | 结算单 Excel 第2行地址显示正确 | 3.1 | B2:F2 显示完整地址 |
| 4 | 结算单 Excel 列宽自适应 | 3.2 | MSKU 等长内容不被截断 |
| 5 | 结算单 Excel 印章正常 | 保持不变 | 印章位置/大小未变 |
| 6 | INV Excel 列宽自适应 | 3.3 | 银行地址等长内容不被截断 |
| 7 | INV Excel 印章正常 | 保持不变 | 印章位置/大小未变 |
| 8 | PO 导出不受影响 | 回归 | 与修复前行为一致 |
| 9 | DN 导出不受影响 | 回归 | 与修复前行为一致 |
