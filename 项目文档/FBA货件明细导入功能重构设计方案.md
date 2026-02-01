# FBA货件明细导入功能重构设计方案

## 一、需求背景

### 1.1 现状分析
- **旧格式**：CSV文件，货件级别汇总数据（一行一个货件）
- **新格式**：Excel文件，SKU明细级别数据（一行一个SKU）
- **核心变化**：从货件汇总视图转变为SKU明细视图

### 1.2 新Excel文件结构
```
Sheet: 发货单详情
列名：
- 物流中心编码：亚马逊仓库地址（如：BHX4 - Plot 1...）
- 创建时间：货件创建时间（2025-10-15 08:58:54）
- SKU：内部SKU编码（如：D06-234-2）
- 店铺：店铺名称（如：慕声欧洲-UK）
- 国家：目标国家（如：英国）
- MSKU：亚马逊MSKU（如：MS-D06-234-2）
- 货件单号：FBA货件编号（如：FBA15KYVTSMJ）
- 发货量：该SKU的发货数量
```

### 1.3 数据特征
- 一个货件单号（shipment_id）对应多个SKU
- 物流中心编码只在第一行显示，后续行为NaN
- 创建时间只在第一行显示，后续行为NaN
- 同一货件的所有SKU共享：货件单号、店铺、国家、物流中心编码、创建时间

---

## 二、数据库设计

### 2.1 表结构设计

#### 2.1.1 货件主表（t_fba_shipment）
存储货件级别的汇总信息

```sql
CREATE TABLE `t_fba_shipment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `shop_id` bigint NOT NULL COMMENT '店铺ID（数据隔离）',
  `shipment_id` varchar(50) NOT NULL COMMENT '货件单号（FBA货件编号，如：FBA15KYVTSMJ）',
  `warehouse_code` varchar(255) DEFAULT NULL COMMENT '物流中心编码（亚马逊仓库地址）',
  `shop_name` varchar(100) DEFAULT NULL COMMENT '店铺名称（如：慕声欧洲-UK）',
  `country` varchar(50) DEFAULT NULL COMMENT '国家（如：英国）',
  `created_date` datetime DEFAULT NULL COMMENT '货件创建时间',
  `sku_count` int DEFAULT 0 COMMENT 'SKU种类数量（自动计算）',
  `total_quantity` int DEFAULT 0 COMMENT '总发货量（自动汇总）',
  `import_batch_id` bigint DEFAULT NULL COMMENT '导入批次ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_shop_shipment` (`shop_id`, `shipment_id`) COMMENT '店铺+货件单号唯一索引',
  KEY `idx_shop_id` (`shop_id`) COMMENT '店铺索引',
  KEY `idx_created_date` (`created_date`) COMMENT '创建时间索引',
  KEY `idx_import_batch` (`import_batch_id`) COMMENT '导入批次索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='FBA货件主表';
```

#### 2.1.2 货件明细表（t_fba_shipment_item）
存储SKU级别的明细信息

```sql
CREATE TABLE `t_fba_shipment_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `shop_id` bigint NOT NULL COMMENT '店铺ID（数据隔离）',
  `shipment_id` bigint NOT NULL COMMENT '货件主表ID（外键）',
  `shipment_no` varchar(50) NOT NULL COMMENT '货件单号（冗余字段，便于查询）',
  `sku` varchar(100) NOT NULL COMMENT '内部SKU编码',
  `msku` varchar(100) DEFAULT NULL COMMENT '亚马逊MSKU',
  `quantity` int NOT NULL DEFAULT 0 COMMENT '发货量',
  `import_batch_id` bigint DEFAULT NULL COMMENT '导入批次ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_shipment_sku` (`shipment_id`, `sku`) COMMENT '货件+SKU唯一索引',
  KEY `idx_shop_id` (`shop_id`) COMMENT '店铺索引',
  KEY `idx_shipment_no` (`shipment_no`) COMMENT '货件单号索引',
  KEY `idx_sku` (`sku`) COMMENT 'SKU索引',
  KEY `idx_msku` (`msku`) COMMENT 'MSKU索引',
  KEY `idx_import_batch` (`import_batch_id`) COMMENT '导入批次索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='FBA货件明细表';
```

### 2.2 数据迁移策略
由于系统尚未上线，可以直接删除旧表，创建新表：
```sql
-- 删除旧表
DROP TABLE IF EXISTS `t_fba_shipment_detail`;

-- 创建新表（见上方SQL）
```

---

## 三、后端设计

### 3.1 实体类设计

#### 3.1.1 FbaShipment.java（货件主表实体）
```java
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_fba_shipment")
@Schema(description = "FBA货件主表")
public class FbaShipment extends BaseEntity {
    @Schema(description = "店铺ID")
    private Long shopId;

    @Schema(description = "货件单号")
    private String shipmentId;

    @Schema(description = "物流中心编码")
    private String warehouseCode;

    @Schema(description = "店铺名称")
    private String shopName;

    @Schema(description = "国家")
    private String country;

    @Schema(description = "货件创建时间")
    private LocalDateTime createdDate;

    @Schema(description = "SKU种类数量")
    private Integer skuCount;

    @Schema(description = "总发货量")
    private Integer totalQuantity;

    @Schema(description = "导入批次ID")
    private Long importBatchId;

    // 非数据库字段：关联的明细列表
    @TableField(exist = false)
    private List<FbaShipmentItem> items;
}
```

#### 3.1.2 FbaShipmentItem.java（货件明细实体）
```java
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_fba_shipment_item")
@Schema(description = "FBA货件明细")
public class FbaShipmentItem extends BaseEntity {
    @Schema(description = "店铺ID")
    private Long shopId;

    @Schema(description = "货件主表ID")
    private Long shipmentId;

    @Schema(description = "货件单号")
    private String shipmentNo;

    @Schema(description = "内部SKU")
    private String sku;

    @Schema(description = "亚马逊MSKU")
    private String msku;

    @Schema(description = "发货量")
    private Integer quantity;

    @Schema(description = "导入批次ID")
    private Long importBatchId;
}
```

### 3.2 导入逻辑设计

#### 3.2.1 Excel解析流程
```
1. 读取Excel文件（Sheet: 发货单详情）
2. 解析表头，识别列名
3. 按行读取数据：
   - 第一行：提取货件信息（物流中心编码、创建时间）+ SKU信息
   - 后续行：提取SKU信息（物流中心编码、创建时间为NaN，使用第一行的值）
4. 按货件单号分组数据
5. 批量插入数据库
```

#### 3.2.2 数据处理逻辑
```java
// 伪代码
Map<String, ShipmentData> shipmentMap = new HashMap<>();

for (Row row : excelRows) {
    String shipmentNo = row.get("货件单号");

    // 获取或创建货件数据
    ShipmentData shipment = shipmentMap.computeIfAbsent(shipmentNo, k -> {
        ShipmentData data = new ShipmentData();
        data.setShipmentNo(shipmentNo);
        // 只在第一次遇到时设置货件级别信息
        if (row.get("物流中心编码") != null) {
            data.setWarehouseCode(row.get("物流中心编码"));
            data.setCreatedDate(row.get("创建时间"));
        }
        data.setShopName(row.get("店铺"));
        data.setCountry(row.get("国家"));
        return data;
    });

    // 添加SKU明细
    ShipmentItem item = new ShipmentItem();
    item.setSku(row.get("SKU"));
    item.setMsku(row.get("MSKU"));
    item.setQuantity(row.get("发货量"));
    shipment.addItem(item);
}

// 批量保存
for (ShipmentData shipment : shipmentMap.values()) {
    // 1. 保存货件主表
    FbaShipment master = saveMaster(shipment);

    // 2. 批量保存明细
    batchSaveItems(master.getId(), shipment.getItems());

    // 3. 更新主表统计
    updateMasterStatistics(master.getId());
}
```

#### 3.2.3 重复数据处理
- **唯一性约束**：`shop_id + shipment_id`（货件主表）、`shipment_id + sku`（明细表）
- **导入策略**：
  - 如果货件已存在：跳过该货件的所有SKU，记录为重复
  - 如果货件不存在：插入货件主表和所有明细

### 3.3 API接口设计

#### 3.3.1 导入接口
```
POST /api/v1/business/fba-shipment/import
Content-Type: multipart/form-data

Request:
- file: Excel文件

Response:
{
  "code": 200,
  "message": "success",
  "data": {
    "batchNo": "FBA-1738123456789-abc123",
    "totalCount": 100,        // 总行数（SKU数）
    "successCount": 95,       // 成功导入的SKU数
    "failCount": 5,           // 失败的SKU数
    "duplicateCount": 0,      // 重复的货件数
    "shipmentCount": 3,       // 导入的货件数
    "errors": []
  }
}
```

#### 3.3.2 列表查询接口
```
GET /api/v1/business/fba-shipment/list
Query Parameters:
- shipmentId: 货件单号（模糊查询）
- shopName: 店铺名称
- country: 国家
- startDate: 开始日期
- endDate: 结束日期
- page: 页码
- size: 每页条数

Response:
{
  "code": 200,
  "data": {
    "records": [
      {
        "id": 1,
        "shipmentId": "FBA15KYVTSMJ",
        "warehouseCode": "BHX4 - Plot 1...",
        "shopName": "慕声欧洲-UK",
        "country": "英国",
        "createdDate": "2025-10-15 08:58:54",
        "skuCount": 30,
        "totalQuantity": 250,
        "createTime": "2026-01-22 10:00:00"
      }
    ],
    "total": 100,
    "page": 1,
    "size": 20
  }
}
```

#### 3.3.3 详情查询接口
```
GET /api/v1/business/fba-shipment/{id}

Response:
{
  "code": 200,
  "data": {
    "id": 1,
    "shipmentId": "FBA15KYVTSMJ",
    "warehouseCode": "BHX4 - Plot 1...",
    "shopName": "慕声欧洲-UK",
    "country": "英国",
    "createdDate": "2025-10-15 08:58:54",
    "skuCount": 30,
    "totalQuantity": 250,
    "items": [
      {
        "id": 1,
        "sku": "D06-234-2",
        "msku": "MS-D06-234-2",
        "quantity": 2
      },
      {
        "id": 2,
        "sku": "D06-348-2",
        "msku": "MS-D06-348-2",
        "quantity": 4
      }
    ]
  }
}
```

---

## 四、前端设计

### 4.1 页面结构

#### 4.1.1 导入页面（/fba-shipment/import）
- **步骤1**：上传Excel文件
  - 支持拖拽上传
  - 文件格式验证（.xlsx, .xls）
  - 文件大小限制（50MB）
- **步骤2**：导入结果展示
  - 成功/失败/重复统计
  - 货件数统计
  - 错误详情列表

#### 4.1.2 列表页面（/fba-shipment/list）
- **搜索栏**：
  - 货件单号（模糊查询）
  - 店铺名称（下拉选择）
  - 国家（下拉选择）
  - 创建日期范围
- **统计卡片**：
  - 总货件数
  - 总SKU种类数
  - 总发货量
- **数据表格**：
  - 货件单号
  - 物流中心编码
  - 店铺名称
  - 国家
  - 创建时间
  - SKU种类数
  - 总发货量
  - 操作（查看详情、删除）

#### 4.1.3 详情弹窗
- **货件信息**：
  - 货件单号
  - 物流中心编码
  - 店铺名称
  - 国家
  - 创建时间
  - SKU种类数
  - 总发货量
- **SKU明细表格**：
  - SKU
  - MSKU
  - 发货量

### 4.2 交互设计

#### 4.2.1 导入流程
```
1. 用户上传Excel文件
2. 前端验证文件格式和大小
3. 点击"开始导入"，弹出确认窗口
4. 确认后，显示加载状态
5. 导入完成，跳转到结果页面
6. 显示导入统计和错误详情
7. 提供"查看列表"和"继续导入"按钮
```

#### 4.2.2 列表查询
```
1. 页面加载时，自动查询第一页数据
2. 同时加载统计数据
3. 用户可通过搜索栏筛选数据
4. 点击"查看详情"，弹出详情弹窗
5. 详情弹窗展示货件信息和SKU明细表格
```

---

## 五、实施计划

### 5.1 开发任务拆分

#### 阶段1：数据库设计（优先级：高）
- [ ] 编写数据库迁移SQL脚本
- [ ] 创建新表结构
- [ ] 删除旧表

#### 阶段2：后端开发（优先级：高）
- [ ] 创建实体类（FbaShipment、FbaShipmentItem）
- [ ] 创建Mapper接口
- [ ] 实现Excel解析服务
- [ ] 实现导入业务逻辑
- [ ] 实现列表查询接口
- [ ] 实现详情查询接口
- [ ] 实现删除接口
- [ ] 编写单元测试

#### 阶段3：前端开发（优先级：中）
- [ ] 修改导入页面（支持Excel上传）
- [ ] 修改列表页面（调整字段显示）
- [ ] 实现详情弹窗（展示SKU明细）
- [ ] 调整API接口调用
- [ ] 更新TypeScript类型定义

#### 阶段4：测试与优化（优先级：中）
- [ ] 功能测试（导入、查询、删除）
- [ ] 性能测试（大文件导入）
- [ ] 边界测试（异常数据处理）
- [ ] UI/UX优化

### 5.2 技术风险评估

| 风险项 | 风险等级 | 应对措施 |
|--------|---------|---------|
| Excel解析性能 | 中 | 使用流式读取，分批处理 |
| 大批量数据插入 | 中 | 使用批量插入，事务控制 |
| 数据一致性 | 高 | 使用事务，主表和明细表同时成功或失败 |
| 重复数据处理 | 低 | 唯一索引约束，导入前检查 |

### 5.3 时间估算
- 数据库设计：0.5天
- 后端开发：2天
- 前端开发：1.5天
- 测试与优化：1天
- **总计**：5天

---

## 六、测试用例

### 6.1 导入功能测试

#### 测试用例1：正常导入
- **输入**：包含30个SKU、1个货件的Excel文件
- **预期**：成功导入1个货件，30个SKU明细

#### 测试用例2：多货件导入
- **输入**：包含100个SKU、5个货件的Excel文件
- **预期**：成功导入5个货件，100个SKU明细

#### 测试用例3：重复货件
- **输入**：已存在的货件单号
- **预期**：跳过该货件，记录为重复

#### 测试用例4：部分数据缺失
- **输入**：某些行缺少SKU或发货量
- **预期**：跳过缺失数据的行，记录错误信息

#### 测试用例5：大文件导入
- **输入**：包含10000个SKU的Excel文件
- **预期**：成功导入，耗时<30秒

### 6.2 查询功能测试

#### 测试用例6：列表查询
- **输入**：无筛选条件
- **预期**：返回所有货件，按创建时间倒序

#### 测试用例7：条件筛选
- **输入**：货件单号="FBA15KYVTSMJ"
- **预期**：返回匹配的货件

#### 测试用例8：详情查询
- **输入**：货件ID=1
- **预期**：返回货件信息和所有SKU明细

---

## 七、附录

### 7.1 Excel文件示例数据
```
物流中心编码 | 创建时间 | SKU | 店铺 | 国家 | MSKU | 货件单号 | 发货量
BHX4 - Plot 1... | 2025-10-15 08:58:54 | D06-234-2 | 慕声欧洲-UK | 英国 | MS-D06-234-2 | FBA15KYVTSMJ | 2
(空) | (空) | D06-348-2 | 慕声欧洲-UK | 英国 | MS-D06-348-2 | FBA15KYVTSMJ | 4
(空) | (空) | D06-348-3 | 慕声欧洲-UK | 英国 | MS-D06-348-3 | FBA15KYVTSMJ | 10
```

### 7.2 关键技术选型
- **Excel解析**：Apache POI（支持.xlsx, .xls）
- **批量插入**：MyBatis-Plus批量插入
- **事务管理**：Spring @Transactional
- **前端组件**：Ant Design Vue（Upload、Table、Modal）

---

## 八、变更记录

| 版本 | 日期 | 变更内容 | 变更人 |
|------|------|---------|--------|
| v1.0 | 2026-01-22 | 初始版本 | Claude |
