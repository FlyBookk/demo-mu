# 规格文档目录

本目录用于存放 Spec-Kit 规格驱动开发的所有规格文档。

## 目录结构

```
.specify/specs/
├── active/          # 活跃的功能规格（正在开发或计划中）
├── archive/         # 已完成或废弃的规格
├── templates/       # 自定义规格模板（可选）
└── README.md        # 本文件
```

## 规格文档命名规范

每个功能规格应该创建独立的目录，命名格式：

```
[编号]-[功能名称]/
```

例如：
- `001-fba-shipment-import/` - FBA 货件导入功能
- `002-tax-calculation-engine/` - 税务计算引擎
- `003-user-permission-management/` - 用户权限管理

## 规格文档内容

每个功能目录应包含：

```
001-feature-name/
├── spec.md          # 功能规格（必需）
├── plan.md          # 技术实施计划（必需）
├── tasks.md         # 任务清单（必需）
├── research.md      # 技术调研（可选）
├── data-model.md    # 数据模型（可选）
├── quickstart.md    # 快速开始指南（可选）
└── contracts/       # API 契约文档（可选）
    ├── api-spec.yaml
    └── examples.json
```

## 工作流程

### 1. Specify 阶段
创建 `spec.md`，定义：
- 用户场景和测试用例
- 功能需求
- 成功标准

### 2. Plan 阶段
创建 `plan.md`，定义：
- 技术上下文
- 项目结构
- 实施方案

### 3. Tasks 阶段
创建 `tasks.md`，定义：
- 具体任务清单
- 任务依赖关系
- 并行执行策略

### 4. Implement 阶段
按照任务清单执行实施，遵循 TDD 原则。

## 规格状态管理

### Active（活跃）
- 正在开发的功能
- 计划开发的功能
- 需要维护的功能

### Archive（归档）
- 已完成并上线的功能
- 被废弃的功能
- 被其他方案替代的功能

归档时保留完整的规格文档，以便：
- 追溯历史决策
- 理解系统演进
- 参考实施经验

## 使用 Spec-Kit CLI

```bash
# 检查工具是否就绪
specify check

# 初始化新项目（如需要）
specify init

# 查看版本信息
specify version
```

## 与 Superpowers 集成

本项目同时使用 Superpowers 工作流程（见 `.cursorrules`）：

- **Spec-Kit**: 用于重要功能的规格管理和审查
- **Superpowers**: 用于日常开发的 TDD、调试、计划流程

两者配合使用：
1. 重要功能先用 Spec-Kit 创建规格
2. 实施时由 Superpowers 自动执行 TDD 流程
3. 完成后归档规格文档

## 参考

- Constitution: `.specify/memory/constitution.md`
- 模板: `.specify/templates/`
- Superpowers 规则: `.cursorrules`
