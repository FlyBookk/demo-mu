# FBA货件明细模块开发流程

## 1. 模块概述

### 1.1 业务背景
FBA（Fulfillment by Amazon）货件明细模块用于管理亚马逊FBA入库货件信息，追踪货件从创建到入库完成的全过程。

### 1.2 数据来源
- **数据文件**: `慕声加拿大2025年3-12月FBA货件明细表.csv`
- **数据格式**: CSV (GBK编码)
- **数据字段**:
  | 字段名 | 说明 | 数据类型 | 示例 |
  |--------|------|---------|------|
  | 货件名称 | 货件批次名称 | String | RP251030001 |
  | 货件编号 | FBA货件ID | String | FBA19334SZXM |
  | 已创建 | 创建时间 | DateTime | 2025年10月30日 下午4:04 |
  | 上次更新 | 最后更新时间 | DateTime | 2025年12月27日 上午10:12 |
  | 收货地址 | 亚马逊仓库代码 | String | YEG2 |
  | SKU | SKU种类数量 | Integer | 24 |
  | 预计商品数量 | 计划发货数量 | Integer | 304 |
  | 找到的商品数量 | 实际入库数量 | Integer | 304 |
  | 状态 | 货件状态 | String | 已完成 |

### 1.3 参照模块
- **前端参照**: `musheng-tax-web/src/views/shipping/`
  - 导入页面: `shipping/import/index.vue`
  - 列表页面: `shipping/list/index.vue`
- **后端参照**: `com/musheng/business/shipping/`
  - Controller: `shipping/controller/ShippingDataController.java`
  - Service: `shipping/service/impl/ShippingDataServiceImpl.java`
  - Entity: `shipping/entity/ShippingData.java`
  - Mapper: `shipping/mapper/ShippingDataMapper.java`

## 2. 功能需求

### 2.1 核心功能
- [x] **数据导入**: CSV文件上传导入，自动解析GBK编码
- [x] **批量插入**: 批量数据录入，避免N+1查询问题
- [x] **重复检测**: 基于货件编号检测重复数据，不更新已存在记录
- [x] **列表查询**: 支持分页、筛选、排序
- [x] **数据管理**: 新增、编辑、删除单条或批量记录
- [x] **导入统计**: 记录总数、成功数、失败数、重复数

### 2.2 技术要求
- **编码支持**: UTF-8/GBK自动检测
- **日期解析**: 使用Hutool DateUtil支持多种日期格式
- **批量处理**: 每批500条记录
- **事务管理**: 使用@Transactional保证数据一致性
- **导入记录**: 记录每次导入的批次信息和结果

## 3. 数据库设计

### 3.1 表名
`fba_shipment_detail` (FBA货件明细表)

### 3.2 字段设计
| 字段名 | 类型 | 长度 | 必填 | 说明 |
|--------|------|------|------|------|
| id | BIGINT | - | Y | 主键ID (自增) |
| shipment_name | VARCHAR | 100 | Y | 货件名称 |
| shipment_id | VARCHAR | 50 | Y | 货件编号(唯一) |
| created_date | DATETIME | - | N | 货件创建时间(来自CSV) |
| last_updated | DATETIME | - | N | 货件最后更新时间(来自CSV) |
| receiving_address | VARCHAR | 50 | N | 收货地址(亚马逊仓库代码) |
| sku_count | INT | - | N | SKU种类数量 |
| expected_quantity | INT | - | N | 预计商品数量 |
| found_quantity | INT | - | N | 找到的商品数量 |
| status | VARCHAR | 20 | N | 货件状态 |
| import_batch_id | BIGINT | - | N | 导入批次ID(关联导入记录表) |
| create_time | DATETIME | - | Y | 记录创建时间(自动填充) |
| update_time | DATETIME | - | Y | 记录更新时间(自动填充) |
| create_by | BIGINT | - | N | 创建人ID(自动填充) |
| update_by | BIGINT | - | N | 更新人ID(自动填充) |

### 3.3 索引设计
- PRIMARY KEY: `id`
- UNIQUE KEY: `uk_shipment_id` (`shipment_id`)
- INDEX: `idx_status` (`status`)
- INDEX: `idx_receiving_address` (`receiving_address`)
- INDEX: `idx_create_time` (`create_time`)

## 4. 接口设计

### 4.1 导入接口
- **路径**: `POST /api/v1/business/fba-shipment/import`
- **参数**: `MultipartFile file`
- **返回**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "batchNo": "FBA-1234567890-abc123",
    "totalCount": 100,
    "successCount": 95,
    "failCount": 2,
    "duplicateCount": 3
  }
}
```

### 4.2 列表查询接口
- **路径**: `GET /api/v1/business/fba-shipment/list`
- **参数**:
  - `status`: 状态(可选)
  - `shipmentId`: 货件编号(可选)
  - `receivingAddress`: 收货地址(可选)
  - `page`: 页码(默认1)
  - `size`: 每页条数(默认20)

### 4.3 其他接口
- `GET /api/v1/business/fba-shipment/{id}`: 获取详情
- `POST /api/v1/business/fba-shipment`: 新增记录
- `PUT /api/v1/business/fba-shipment/{id}`: 更新记录
- `DELETE /api/v1/business/fba-shipment/{id}`: 删除记录
- `DELETE /api/v1/business/fba-shipment/batch`: 批量删除
- `GET /api/v1/business/fba-shipment/summary`: 统计汇总
- `GET /api/v1/business/fba-shipment/export`: 导出数据

## 5. 开发流程

### 5.1 后端开发
**Step 1: 使用后端Agent设计接口**
```bash
/agent 后端
```
- 设计接口路径和参数
- 定义数据模型和数据库表结构
- 编写接口文档

**Step 2: 实现后端功能**
参照文档: `agent-backend.md`
- [ ] 创建Entity实体类
- [ ] 创建Mapper接口和XML
- [ ] 创建Service接口和实现类
- [ ] 创建Controller
- [ ] 实现CSV解析逻辑
- [ ] 实现批量导入逻辑
- [ ] 实现CRUD操作
- [ ] 编写单元测试

### 5.2 前端开发
**Step 1: 使用前端Agent设计交互**
```bash
/agent 前端
```
- 参照配送数据模块设计页面结构
- 根据后端接口设计请求逻辑
- 定义类型接口

**Step 2: 实现前端功能**
参照文档: `agent-frontend.md`
- [ ] 创建类型定义 `types/fbaShipment.ts`
- [ ] 实现导入页面 `views/fba-shipment/import/index.vue`
- [ ] 实现列表页面 `views/fba-shipment/list/index.vue`
- [ ] 配置路由
- [ ] 实现API调用
- [ ] 联调测试

### 5.3 任务管理
- **使用TodoWrite记录开发进度**: 防止会话中断丢失进度
- **会话管理**: 会话过大时使用 `/clear` 命令清理
- **进度恢复**: 使用 `/todo` skill读取待办文档继续开发
- **开发模式**: 无需产品设计和测试环节，前后端联调即可

### 5.4 测试验证
**后端测试**
- 使用Postman或curl测试接口
- 访问地址: `http://localhost:8080/api/v1/business/fba-shipment/*`

**前端测试**
- 启动前端服务
- 测试文件上传导入功能
- 测试列表查询筛选功能
- 测试CRUD操作

## 6. 验收标准

### 6.1 功能验收
- [x] CSV文件可正常导入，支持GBK编码
- [x] 日期字段解析正确
- [x] 重复数据检测准确，不更新已存在记录
- [x] 列表查询、筛选、分页功能正常
- [x] 新增、编辑、删除功能正常
- [x] 批量删除功能正常
- [x] 导入统计数据准确（总数、成功、失败、重复）

### 6.2 性能验收
- [x] 1000条数据导入时间 < 10秒
- [x] 无N+1查询问题
- [x] 列表查询响应时间 < 500ms

### 6.3 代码质量
- [x] 代码符合项目规范
- [x] 异常处理完善
- [x] 日志记录完整
- [x] 事务管理正确

## 7. 注意事项

1. **编码问题**: CSV文件可能是GBK编码，需实现UTF-8/GBK自动检测
2. **日期格式**: 中文日期格式需使用Hutool DateUtil解析
3. **唯一性约束**: 货件编号(shipment_id)必须唯一
4. **批量性能**: 使用批量插入避免N+1查询
5. **重复处理**: 检测到重复数据时不更新，只计数统计
6. **导入记录**: 每次导入需记录批次信息，便于追溯


