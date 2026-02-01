# Implementation Plan: FBA 货件批量导入

**Branch**: `001-fba-batch-import` | **Date**: 2026-02-01 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `spec.md`

## Summary

实现 FBA 货件批量导入功能，允许用户通过上传 Excel 文件批量导入货件数据。系统将进行数据验证、预览确认、批量保存，并提供详细的导入结果反馈。

## Technical Context

**Language/Version**: Java 17 (后端), TypeScript 5.x (前端)
**Primary Dependencies**:
- 后端: Spring Boot 3.2.2, MyBatis-Plus 3.5.5, EasyExcel 3.3.3, Sa-Token 1.37.0
- 前端: Vue 3.4, Ant Design Vue 4.1, Axios

**Storage**: MySQL 8.0 (货件数据、导入记录)，文件系统 (原始 Excel 文件备份)
**Testing**: JUnit 5 + Spring Boot Test (后端), Vitest (前端)
**Target Platform**: Web 应用（Chrome/Edge/Safari 最新版本）
**Project Type**: Web 应用（前后端分离）
**Performance Goals**:
- 1000 条数据上传验证 < 30 秒
- 1000 条数据批量保存 < 10 秒
- 10000 条数据导入 < 2 分钟

**Constraints**:
- 文件大小限制 10MB
- 单次导入最多 10000 条
- 必须使用事务确保数据一致性
- 必须记录完整的审计日志

**Scale/Scope**:
- 预计日均导入操作 50 次
- 单次平均导入 500-1000 条数据
- 需要支持 10 个并发用户同时导入

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

✅ **数据准确性优先**:
- 所有数据验证逻辑有单元测试
- 批量保存有集成测试
- 边界条件有测试覆盖

✅ **测试驱动开发**:
- 所有功能先写测试
- 遵循 RED-GREEN-REFACTOR

✅ **前后端契约**:
- API 使用 Knife4j 文档
- 前后端数据结构明确定义

✅ **审计可追溯**:
- 记录导入操作日志
- 保留原始文件备份

✅ **简洁性**:
- 使用现有的 EasyExcel 库
- 不引入新的复杂抽象

## Project Structure

### Documentation (this feature)

```text
.specify/specs/active/001-fba-batch-import/
├── spec.md              # 功能规格（已完成）
├── plan.md              # 本文件
├── tasks.md             # 任务清单（待创建）
└── contracts/           # API 契约
    └── api-spec.md      # API 接口定义
```

### Source Code (repository root)

```text
# 后端代码
musheng-tax-system/
├── musheng-business/
│   └── src/main/java/com/musheng/business/
│       ├── domain/
│       │   ├── FbaShipment.java           # 货件实体
│       │   └── ImportRecord.java          # 导入记录实体
│       ├── mapper/
│       │   ├── FbaShipmentMapper.java
│       │   └── ImportRecordMapper.java
│       ├── service/
│       │   ├── FbaShipmentService.java
│       │   ├── ImportService.java         # 导入服务（核心）
│       │   └── ExcelParseService.java     # Excel 解析服务
│       ├── controller/
│       │   └── FbaImportController.java   # 导入控制器
│       └── dto/
│           ├── ImportPreviewDTO.java      # 预览数据
│           ├── ImportResultDTO.java       # 导入结果
│           └── FbaShipmentImportDTO.java  # 导入数据模型
│
└── src/test/java/com/musheng/business/
    ├── service/
    │   ├── ImportServiceTest.java         # 单元测试
    │   └── ExcelParseServiceTest.java
    └── controller/
        └── FbaImportControllerTest.java   # 集成测试

# 前端代码
musheng-tax-web/
├── src/
│   ├── api/
│   │   └── fba-import.ts                  # 导入 API 封装
│   ├── views/
│   │   └── fba-shipment/
│   │       ├── index.vue                  # 货件列表页
│   │       └── BatchImport.vue            # 批量导入页面
│   ├── components/
│   │   └── import/
│   │       ├── FileUpload.vue             # 文件上传组件
│   │       ├── DataPreview.vue            # 数据预览组件
│   │       └── ImportResult.vue           # 结果展示组件
│   └── types/
│       └── fba-import.ts                  # TypeScript 类型定义
│
└── tests/
    └── views/
        └── fba-shipment/
            └── BatchImport.spec.ts        # 组件测试

# 文件存储
uploads/
└── fba-import/
    └── [YYYY-MM-DD]/
        └── [batch-id]-[filename].xlsx     # 原始文件备份
```

**Structure Decision**: 采用标准的前后端分离结构，后端按照 DDD 分层（domain, service, controller），前端按照功能模块组织（views, components, api）。

## API Contracts

### 1. 上传并解析 Excel 文件

**Endpoint**: `POST /api/fba-import/upload`

**Request**:
```
Content-Type: multipart/form-data
file: [Excel 文件]
```

**Response**:
```json
{
  "code": 200,
  "message": "解析成功",
  "data": {
    "batchId": "20260201-001",
    "totalCount": 100,
    "validCount": 98,
    "invalidCount": 2,
    "previewData": [
      {
        "rowNumber": 1,
        "shipmentId": "FBA123",
        "quantity": 100,
        "shipDate": "2026-01-15",
        "destination": "US",
        "valid": true,
        "errors": []
      },
      {
        "rowNumber": 3,
        "shipmentId": "FBA125",
        "quantity": -10,
        "shipDate": "2026-01-16",
        "destination": "UK",
        "valid": false,
        "errors": ["数量不能为负数"]
      }
    ]
  }
}
```

### 2. 确认导入

**Endpoint**: `POST /api/fba-import/confirm`

**Request**:
```json
{
  "batchId": "20260201-001"
}
```

**Response**:
```json
{
  "code": 200,
  "message": "导入成功",
  "data": {
    "batchId": "20260201-001",
    "successCount": 98,
    "failCount": 0,
    "totalCount": 98,
    "duration": 3500,
    "failedRows": []
  }
}
```

### 3. 下载导入模板

**Endpoint**: `GET /api/fba-import/template`

**Response**: Excel 文件下载

## Data Model

### FbaShipment (货件表)

```sql
CREATE TABLE fba_shipment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  shipment_id VARCHAR(50) NOT NULL UNIQUE COMMENT '货件编号',
  quantity INT NOT NULL COMMENT '数量',
  ship_date DATE NOT NULL COMMENT '发货日期',
  destination VARCHAR(50) COMMENT '目的地',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  create_by VARCHAR(50),
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  update_by VARCHAR(50),
  deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
  INDEX idx_shipment_id (shipment_id),
  INDEX idx_ship_date (ship_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='FBA货件表';
```

### ImportRecord (导入记录表)

```sql
CREATE TABLE import_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  batch_id VARCHAR(50) NOT NULL UNIQUE COMMENT '批次号',
  file_name VARCHAR(255) NOT NULL COMMENT '文件名',
  file_path VARCHAR(500) NOT NULL COMMENT '文件路径',
  status VARCHAR(20) NOT NULL COMMENT '状态: SUCCESS/FAILED/PARTIAL',
  total_count INT DEFAULT 0 COMMENT '总数',
  success_count INT DEFAULT 0 COMMENT '成功数',
  fail_count INT DEFAULT 0 COMMENT '失败数',
  error_message TEXT COMMENT '错误信息(JSON)',
  operator VARCHAR(50) COMMENT '操作人',
  operate_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  duration INT COMMENT '耗时(毫秒)',
  INDEX idx_batch_id (batch_id),
  INDEX idx_operator (operator),
  INDEX idx_operate_time (operate_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='导入记录表';
```

## Implementation Phases

### Phase 1: 基础设施 (Foundation)
- 数据库表创建
- 实体类和 Mapper
- 文件存储配置

### Phase 2: User Story 1 - 文件上传与解析
- Excel 解析服务
- 数据验证逻辑
- 上传 API
- 前端上传组件

### Phase 3: User Story 2 - 数据验证与预览
- 业务规则验证
- 预览数据组装
- 前端预览组件

### Phase 4: User Story 3 - 批量保存与结果
- 批量保存服务（事务）
- 导入记录保存
- 确认导入 API
- 前端结果展示

### Phase 5: 完善与优化
- 错误处理
- 日志记录
- 性能优化
- 文档完善

## Testing Strategy

### 单元测试
- `ExcelParseServiceTest`: 测试 Excel 解析逻辑
- `ImportServiceTest`: 测试导入业务逻辑
- 覆盖所有验证规则和边界条件

### 集成测试
- `FbaImportControllerTest`: 测试完整的导入流程
- 使用 `@SpringBootTest` 和 `MockMvc`
- 测试事务回滚场景

### 前端测试
- `BatchImport.spec.ts`: 测试组件交互
- 测试文件上传、预览、确认流程
- 测试错误提示展示

### 性能测试
- 测试 10000 条数据的导入性能
- 验证内存使用情况
- 验证并发导入场景

## Risk Mitigation

1. **内存溢出**: 使用 EasyExcel 流式读取，不一次性加载全部数据
2. **事务超时**: 批量保存使用分批提交（每 1000 条一批）
3. **文件安全**: 严格验证文件类型，使用白名单机制
4. **并发冲突**: 使用数据库唯一约束 + 乐观锁

## Complexity Tracking

无违反 Constitution 的复杂性引入。

---

**下一步**: 创建 `tasks.md` 任务清单
