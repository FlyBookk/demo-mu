# Tasks: FBA 货件批量导入

**Input**: Design documents from `.specify/specs/active/001-fba-batch-import/`
**Prerequisites**: spec.md (✓), plan.md (✓)

**Tests**: 本功能包含完整的测试任务，遵循 TDD 原则。

**Organization**: 任务按用户故事分组，每个故事可独立实施和测试。

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 可并行执行（不同文件，无依赖）
- **[Story]**: 所属用户故事（US1, US2, US3）
- 包含精确的文件路径

---

## Phase 1: Setup (共享基础设施)

**Purpose**: 项目初始化和基础结构

- [ ] T001 创建数据库迁移脚本 `musheng-tax-system/sql/V1.1__create_import_tables.sql`
- [ ] T002 [P] 配置文件上传路径 `musheng-tax-system/musheng-web/src/main/resources/application.yml`
- [ ] T003 [P] 添加 EasyExcel 依赖（如未添加）`musheng-tax-system/pom.xml`

---

## Phase 2: Foundational (阻塞性前置条件)

**Purpose**: 所有用户故事依赖的核心基础设施

**⚠️ CRITICAL**: 此阶段完成前，用户故事无法开始

- [ ] T004 创建 FbaShipment 实体类 `musheng-business/src/main/java/com/musheng/business/domain/FbaShipment.java`
- [ ] T005 [P] 创建 ImportRecord 实体类 `musheng-business/src/main/java/com/musheng/business/domain/ImportRecord.java`
- [ ] T006 创建 FbaShipmentMapper `musheng-business/src/main/java/com/musheng/business/mapper/FbaShipmentMapper.java`
- [ ] T007 [P] 创建 ImportRecordMapper `musheng-business/src/main/java/com/musheng/business/mapper/ImportRecordMapper.java`
- [ ] T008 [P] 创建前端 TypeScript 类型定义 `musheng-tax-web/src/types/fba-import.ts`
- [ ] T009 [P] 创建前端 API 封装 `musheng-tax-web/src/api/fba-import.ts`

**Checkpoint**: 基础设施就绪 - 用户故事实施可以开始

---

## Phase 3: User Story 1 - Excel 文件上传与解析 (Priority: P1) 🎯 MVP

**Goal**: 用户可以上传 Excel 文件，系统解析并验证格式

**Independent Test**: 可以独立测试文件上传、格式验证、数据解析，不依赖数据保存

### Tests for User Story 1 ⚠️

> **NOTE: 先写这些测试，确保它们失败后再实施**

- [ ] T010 [P] [US1] 编写 Excel 解析服务单元测试 `musheng-business/src/test/java/com/musheng/business/service/ExcelParseServiceTest.java`
  ```java
  // 测试用例：
  // - testParseValidExcel() - 解析正确格式的 Excel
  // - testParseInvalidFileType() - 拒绝非 Excel 文件
  // - testParseEmptyFile() - 处理空文件
  // - testParseMissingColumns() - 检测缺失必填列
  // - testParseExceedMaxRows() - 检测超过最大行数
  ```

- [ ] T011 [P] [US1] 编写文件上传控制器集成测试 `musheng-business/src/test/java/com/musheng/business/controller/FbaImportControllerTest.java`
  ```java
  // 测试用例：
  // - testUploadValidFile() - 上传有效文件
  // - testUploadInvalidFileType() - 上传无效文件类型
  // - testUploadExceedSizeLimit() - 上传超大文件
  ```

### Implementation for User Story 1

- [ ] T012 [US1] 实现 ExcelParseService - 文件类型验证
  - 文件: `musheng-business/src/main/java/com/musheng/business/service/ExcelParseService.java`
  - 方法: `validateFileType(MultipartFile file)`
  - 验证: 只接受 .xlsx 和 .xls
  - 运行测试: `mvn test -Dtest=ExcelParseServiceTest#testParseInvalidFileType`
  - 预期: PASS

- [ ] T013 [US1] 实现 ExcelParseService - 文件大小验证
  - 方法: `validateFileSize(MultipartFile file)`
  - 验证: 文件大小 ≤ 10MB
  - 运行测试: `mvn test -Dtest=ExcelParseServiceTest#testParseExceedMaxRows`
  - 预期: PASS

- [ ] T014 [US1] 实现 ExcelParseService - Excel 解析
  - 方法: `parseExcel(MultipartFile file)`
  - 使用 EasyExcel 流式读取
  - 验证必填列存在
  - 运行测试: `mvn test -Dtest=ExcelParseServiceTest#testParseValidExcel`
  - 预期: PASS

- [ ] T015 [US1] 实现 FbaImportController - 上传接口
  - 文件: `musheng-business/src/main/java/com/musheng/business/controller/FbaImportController.java`
  - 接口: `POST /api/fba-import/upload`
  - 添加 Knife4j 注解
  - 运行测试: `mvn test -Dtest=FbaImportControllerTest#testUploadValidFile`
  - 预期: PASS

- [ ] T016 [P] [US1] 实现前端文件上传组件
  - 文件: `musheng-tax-web/src/components/import/FileUpload.vue`
  - 功能: 文件选择、拖拽上传、进度显示
  - 使用 Ant Design Upload 组件

- [ ] T017 [US1] 实现前端批量导入页面
  - 文件: `musheng-tax-web/src/views/fba-shipment/BatchImport.vue`
  - 集成 FileUpload 组件
  - 调用上传 API
  - 显示解析结果

- [ ] T018 [US1] 添加路由配置
  - 文件: `musheng-tax-web/src/router/index.ts`
  - 路由: `/fba-shipment/batch-import`

**Checkpoint**: 用户故事 1 完成 - 可以独立测试文件上传和解析功能

---

## Phase 4: User Story 2 - 数据验证与错误提示 (Priority: P2)

**Goal**: 系统验证导入数据的业务规则，清晰展示验证结果

**Independent Test**: 可以独立测试验证规则，使用模拟的解析数据

### Tests for User Story 2 ⚠️

- [ ] T019 [P] [US2] 编写数据验证单元测试 `musheng-business/src/test/java/com/musheng/business/service/ImportServiceTest.java`
  ```java
  // 测试用例：
  // - testValidateDuplicateShipmentId() - 检测重复货件编号
  // - testValidateNegativeQuantity() - 检测负数数量
  // - testValidateInvalidDate() - 检测无效日期
  // - testValidateAllValid() - 所有数据有效
  ```

### Implementation for User Story 2

- [ ] T020 [US2] 实现 ImportService - 重复检测
  - 文件: `musheng-business/src/main/java/com/musheng/business/service/ImportService.java`
  - 方法: `checkDuplicates(List<FbaShipmentImportDTO> data)`
  - 检测 Excel 内部重复 + 数据库已存在
  - 运行测试: `mvn test -Dtest=ImportServiceTest#testValidateDuplicateShipmentId`
  - 预期: PASS

- [ ] T021 [US2] 实现 ImportService - 数据格式验证
  - 方法: `validateData(List<FbaShipmentImportDTO> data)`
  - 验证: 数量 > 0，日期格式正确
  - 运行测试: `mvn test -Dtest=ImportServiceTest#testValidateNegativeQuantity`
  - 预期: PASS

- [ ] T022 [US2] 实现 ImportService - 预览数据组装
  - 方法: `buildPreviewData(List<FbaShipmentImportDTO> data, List<ValidationError> errors)`
  - 组装预览 DTO，标记错误行
  - 运行测试: `mvn test -Dtest=ImportServiceTest#testValidateAllValid`
  - 预期: PASS

- [ ] T023 [US2] 更新上传接口 - 集成验证逻辑
  - 文件: `FbaImportController.java`
  - 在 upload 方法中调用验证服务
  - 返回预览数据

- [ ] T024 [P] [US2] 实现前端数据预览组件
  - 文件: `musheng-tax-web/src/components/import/DataPreview.vue`
  - 功能: 表格展示、错误高亮、统计信息
  - 使用 VXE-Table 组件

- [ ] T025 [US2] 集成预览组件到导入页面
  - 文件: `BatchImport.vue`
  - 上传成功后显示预览
  - 显示验证统计

**Checkpoint**: 用户故事 2 完成 - 可以独立测试数据验证功能

---

## Phase 5: User Story 3 - 批量保存与结果反馈 (Priority: P3)

**Goal**: 用户确认后批量保存数据，提供详细结果报告

**Independent Test**: 可以独立测试批量保存逻辑，使用模拟的已验证数据

### Tests for User Story 3 ⚠️

- [ ] T026 [P] [US3] 编写批量保存单元测试 `ImportServiceTest.java`
  ```java
  // 测试用例：
  // - testBatchSaveSuccess() - 批量保存成功
  // - testBatchSaveRollback() - 失败时回滚
  // - testBatchSavePartialFail() - 部分失败处理
  ```

- [ ] T027 [P] [US3] 编写导入记录保存测试 `ImportServiceTest.java`
  ```java
  // 测试用例：
  // - testSaveImportRecord() - 保存导入记录
  // - testSaveImportRecordWithErrors() - 保存失败记录
  ```

### Implementation for User Story 3

- [ ] T028 [US3] 实现 ImportService - 批量保存
  - 方法: `batchSave(String batchId, List<FbaShipmentImportDTO> data)`
  - 使用 `@Transactional` 确保事务
  - 分批保存（每 1000 条）
  - 运行测试: `mvn test -Dtest=ImportServiceTest#testBatchSaveSuccess`
  - 预期: PASS

- [ ] T029 [US3] 实现 ImportService - 导入记录保存
  - 方法: `saveImportRecord(ImportRecord record)`
  - 记录导入结果和统计信息
  - 运行测试: `mvn test -Dtest=ImportServiceTest#testSaveImportRecord`
  - 预期: PASS

- [ ] T030 [US3] 实现 ImportService - 文件备份
  - 方法: `backupFile(MultipartFile file, String batchId)`
  - 保存到 `uploads/fba-import/[YYYY-MM-DD]/`
  - 文件名: `[batchId]-[originalName]`

- [ ] T031 [US3] 实现确认导入接口
  - 文件: `FbaImportController.java`
  - 接口: `POST /api/fba-import/confirm`
  - 调用批量保存服务
  - 返回导入结果

- [ ] T032 [P] [US3] 实现前端结果展示组件
  - 文件: `musheng-tax-web/src/components/import/ImportResult.vue`
  - 功能: 成功/失败统计、失败详情、操作按钮
  - 使用 Ant Design Result 组件

- [ ] T033 [US3] 集成结果组件到导入页面
  - 文件: `BatchImport.vue`
  - 确认导入后显示结果
  - 提供"返回列表"和"继续导入"按钮

- [ ] T034 [US3] 更新货件列表页面
  - 文件: `musheng-tax-web/src/views/fba-shipment/index.vue`
  - 添加"批量导入"按钮
  - 跳转到导入页面

**Checkpoint**: 所有用户故事完成 - 完整的导入流程可用

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: 跨用户故事的改进和完善

- [ ] T035 [P] 添加操作日志记录
  - 文件: `ImportService.java`
  - 记录: 操作人、时间、文件名、结果
  - 使用 AOP 或手动记录

- [ ] T036 [P] 添加错误处理和友好提示
  - 文件: `FbaImportController.java`
  - 统一异常处理
  - 返回用户友好的错误信息

- [ ] T037 [P] 性能优化 - 大文件处理
  - 验证 10000 条数据的性能
  - 优化内存使用
  - 添加性能日志

- [ ] T038 [P] 前端加载状态优化
  - 添加上传进度条
  - 添加保存中的 Loading 状态
  - 优化用户体验

- [ ] T039 [P] 编写前端组件测试
  - 文件: `musheng-tax-web/tests/views/fba-shipment/BatchImport.spec.ts`
  - 测试文件上传、预览、确认流程

- [ ] T040 更新 API 文档
  - 确保 Knife4j 注解完整
  - 添加请求/响应示例
  - 访问 `/doc.html` 验证

- [ ] T041 [P] 创建导入模板下载功能（可选）
  - 接口: `GET /api/fba-import/template`
  - 生成标准 Excel 模板

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: 无依赖 - 立即开始
- **Foundational (Phase 2)**: 依赖 Setup - 阻塞所有用户故事
- **User Stories (Phase 3-5)**: 依赖 Foundational
  - US1 → US2 → US3（顺序执行）
  - 或 US1 完成后，US2 和 US3 可并行（如果团队资源允许）
- **Polish (Phase 6)**: 依赖所有用户故事完成

### User Story Dependencies

- **User Story 1 (P1)**: Foundational 完成后可开始 - 无其他依赖
- **User Story 2 (P2)**: 依赖 US1 的解析结果 - 但可以用模拟数据独立测试
- **User Story 3 (P3)**: 依赖 US2 的验证结果 - 但可以用模拟数据独立测试

### Within Each User Story

- 测试必须先写并失败
- 实现前先运行测试确认失败
- 实现后运行测试确认通过
- 每个任务完成后提交代码

### Parallel Opportunities

- Phase 1: T001, T002, T003 可并行
- Phase 2: T005, T007, T008, T009 可并行（T004, T006 顺序）
- Phase 3: T010, T011 可并行；T016 可与后端任务并行
- Phase 4: T019 独立；T024 可与后端任务并行
- Phase 5: T026, T027 可并行；T032 可与后端任务并行
- Phase 6: 所有任务可并行

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: 测试文件上传和解析
5. Demo 给用户

### Incremental Delivery

1. Setup + Foundational → 基础就绪
2. Add User Story 1 → 测试 → Demo (MVP!)
3. Add User Story 2 → 测试 → Demo
4. Add User Story 3 → 测试 → Demo
5. Polish → 完整功能上线

### Parallel Team Strategy

如果有多个开发者:

1. 团队一起完成 Setup + Foundational
2. Foundational 完成后:
   - Developer A: User Story 1 (前端 + 后端)
   - Developer B: User Story 2 (可以先写测试和模拟数据)
   - Developer C: User Story 3 (可以先写测试和模拟数据)
3. US1 完成后，B 和 C 集成真实数据

---

## Notes

- [P] 任务 = 不同文件，无依赖，可并行
- [Story] 标签映射任务到具体用户故事
- 每个用户故事应该可独立完成和测试
- 实现前必须看到测试失败
- 每个任务或逻辑组完成后提交
- 在任何检查点停下来独立验证故事
- 避免: 模糊任务、同文件冲突、破坏独立性的跨故事依赖

---

## Commit Strategy

遵循 Conventional Commits:

```bash
# Phase 1-2
git commit -m "chore: 创建导入相关数据库表"
git commit -m "feat: 添加 FbaShipment 和 ImportRecord 实体"

# Phase 3 (US1)
git commit -m "test: 添加 Excel 解析服务测试"
git commit -m "feat: 实现 Excel 文件解析和验证"
git commit -m "feat: 实现文件上传 API"
git commit -m "feat: 添加前端文件上传组件"

# Phase 4 (US2)
git commit -m "test: 添加数据验证测试"
git commit -m "feat: 实现导入数据验证逻辑"
git commit -m "feat: 添加前端数据预览组件"

# Phase 5 (US3)
git commit -m "test: 添加批量保存测试"
git commit -m "feat: 实现批量保存和事务处理"
git commit -m "feat: 添加前端结果展示组件"

# Phase 6
git commit -m "docs: 更新 API 文档"
git commit -m "perf: 优化大文件导入性能"
git commit -m "test: 添加前端组件测试"
```
