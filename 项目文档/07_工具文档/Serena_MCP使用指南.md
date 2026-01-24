# Serena MCP 使用指南

## 一、概述

Serena 是一个基于 LSP（Language Server Protocol）的智能代码分析 MCP 服务器，提供符号级别的代码导航、搜索和编辑能力。

### 核心优势
- **Token 高效**：避免读取整个文件，只获取需要的符号信息
- **语义理解**：基于 LSP 的真正代码理解，而非文本搜索
- **精准编辑**：符号级别的代码修改，自动处理引用关系

### 工作原理
```
用户请求 → Cursor Agent → MCP 协议 → Serena → LSP 服务器 → 返回结果
                                            ↓
                                      (Java: jdtls)
                                      (TS/Vue: tsserver)
                                      (Python: pylsp)
```

---

## 二、工具分类

### 2.1 代码导航工具（核心）⭐

| 工具名 | 功能 | 使用场景 |
|--------|------|----------|
| `get_symbols_overview` | 获取文件符号概览 | 了解文件结构，第一步 |
| `find_symbol` | 查找代码符号 | 定位类、方法、函数 |
| `find_referencing_symbols` | 查找引用关系 | 了解谁调用了这个方法 |
| `search_for_pattern` | 正则搜索代码 | 模糊搜索、跨文件搜索 |

### 2.2 文件操作工具

| 工具名 | 功能 | 使用场景 |
|--------|------|----------|
| `list_dir` | 列出目录内容 | 浏览项目结构 |
| `find_file` | 按名称查找文件 | 定位特定文件 |

### 2.3 代码编辑工具

| 工具名 | 功能 | 使用场景 |
|--------|------|----------|
| `replace_symbol_body` | 替换整个符号体 | 重写方法/函数 |
| `replace_content` | 正则替换内容 | 局部修改代码 |
| `insert_after_symbol` | 在符号后插入 | 添加新方法 |
| `insert_before_symbol` | 在符号前插入 | 添加导入语句 |
| `rename_symbol` | 重命名符号 | 全局重命名变量/方法 |

### 2.4 项目管理工具

| 工具名 | 功能 | 使用场景 |
|--------|------|----------|
| `activate_project` | 激活项目 | 切换工作项目 |
| `get_current_config` | 获取当前配置 | 查看项目状态 |
| `check_onboarding_performed` | 检查初始化状态 | 首次使用时 |
| `onboarding` | 项目初始化 | 首次使用时 |

### 2.5 记忆工具

| 工具名 | 功能 | 使用场景 |
|--------|------|----------|
| `list_memories` | 列出所有记忆 | 查看保存的项目信息 |
| `read_memory` | 读取记忆内容 | 获取项目概览等 |
| `write_memory` | 写入记忆 | 保存项目信息 |
| `edit_memory` | 编辑记忆 | 更新项目信息 |
| `delete_memory` | 删除记忆 | 清理过期信息 |

### 2.6 思考辅助工具

| 工具名 | 功能 | 使用场景 |
|--------|------|----------|
| `think_about_collected_information` | 反思收集的信息 | 搜索后整理思路 |
| `think_about_task_adherence` | 反思任务进度 | 编辑前确认方向 |
| `think_about_whether_you_are_done` | 反思是否完成 | 结束前检查 |
| `initial_instructions` | 获取使用说明 | 首次使用时 |

---

## 三、核心概念

### 3.1 Name Path（名称路径）

符号在文件内的层级路径，用 `/` 分隔：

```
类名/方法名
模块名/类名/方法名
```

**示例：**
- `UserController` - 类
- `UserController/login` - UserController 类的 login 方法
- `UserController/login[0]` - 重载方法的第一个版本
- `/UserController/login` - 绝对路径（从文件根开始）

### 3.2 Relative Path（相对路径）

相对于项目根目录的文件路径：

```
musheng-tax-web/src/api/user.ts
musheng-tax-system/musheng-business/src/main/java/.../UserController.java
```

### 3.3 Depth（深度）

获取符号时的层级深度：
- `depth=0` - 只获取符号本身
- `depth=1` - 获取符号及其直接子符号（如类的所有方法）
- `depth=2` - 获取两层嵌套

---

## 四、使用流程

### 4.1 探索代码（推荐流程）

```
Step 1: get_symbols_overview
        ↓ 了解文件有哪些类/方法
Step 2: find_symbol (include_body=False, depth=1)
        ↓ 获取类的方法列表
Step 3: find_symbol (include_body=True)
        ↓ 只读取需要的方法体
Step 4: find_referencing_symbols
        ↓ 了解引用关系
```

### 4.2 修改代码（推荐流程）

```
Step 1: find_symbol 定位目标符号
        ↓
Step 2: find_referencing_symbols 检查影响范围
        ↓
Step 3: replace_symbol_body 或 replace_content 执行修改
        ↓
Step 4: 更新所有受影响的引用（如需要）
```

---

## 五、工具详解与示例

### 5.1 get_symbols_overview

**功能**：获取文件中所有符号的概览

**参数**：
- `relative_path` (必需): 文件相对路径
- `depth` (可选): 层级深度，默认 0

**示例**：
```json
{
  "relative_path": "musheng-tax-web/src/api/user.ts",
  "depth": 1
}
```

**输出**：
```
Functions: login, logout, getUserInfo, updateUser
Interfaces: LoginParams, UserInfo
```

---

### 5.2 find_symbol

**功能**：查找符号并获取详细信息

**参数**：
- `name_path_pattern` (必需): 符号名称路径模式
- `relative_path` (可选): 限制搜索范围
- `include_body` (可选): 是否包含代码体
- `include_info` (可选): 是否包含文档/签名
- `depth` (可选): 子符号深度
- `substring_matching` (可选): 是否启用子串匹配

**示例 1**：查找类的所有方法
```json
{
  "name_path_pattern": "UserController",
  "relative_path": "musheng-tax-system",
  "include_body": false,
  "depth": 1
}
```

**示例 2**：获取特定方法的代码
```json
{
  "name_path_pattern": "UserController/login",
  "include_body": true
}
```

**示例 3**：模糊搜索
```json
{
  "name_path_pattern": "get",
  "substring_matching": true,
  "relative_path": "musheng-tax-web/src/api"
}
```

---

### 5.3 find_referencing_symbols

**功能**：查找所有引用某符号的位置

**参数**：
- `name_path` (必需): 符号名称路径
- `relative_path` (必需): 符号所在文件
- `include_info` (可选): 是否包含引用处信息

**示例**：
```json
{
  "name_path": "login",
  "relative_path": "musheng-tax-web/src/api/auth.ts",
  "include_info": true
}
```

---

### 5.4 search_for_pattern

**功能**：正则表达式搜索代码

**参数**：
- `substring_pattern` (必需): 正则表达式
- `relative_path` (可选): 限制搜索范围
- `paths_include_glob` (可选): 文件类型过滤
- `paths_exclude_glob` (可选): 排除文件
- `context_lines_before/after` (可选): 上下文行数

**示例 1**：搜索所有 TODO 注释
```json
{
  "substring_pattern": "TODO.*",
  "relative_path": "musheng-tax-web/src"
}
```

**示例 2**：搜索特定 API 调用
```json
{
  "substring_pattern": "axios\\.post\\(",
  "paths_include_glob": "*.ts",
  "context_lines_before": 2,
  "context_lines_after": 2
}
```

---

### 5.5 replace_symbol_body

**功能**：替换整个符号的定义体

**参数**：
- `name_path` (必需): 符号名称路径
- `relative_path` (必需): 文件路径
- `body` (必需): 新的符号体（包含签名）

**示例**：
```json
{
  "name_path": "UserController/login",
  "relative_path": "src/controllers/UserController.java",
  "body": "public Result login(LoginDTO dto) {\n    // 新的实现\n    return userService.login(dto);\n}"
}
```

---

### 5.6 replace_content

**功能**：正则替换文件内容

**参数**：
- `relative_path` (必需): 文件路径
- `needle` (必需): 搜索模式
- `repl` (必需): 替换内容
- `mode` (必需): "literal" 或 "regex"
- `allow_multiple_occurrences` (可选): 是否替换多处

**示例 1**：精确替换
```json
{
  "relative_path": "src/config.ts",
  "needle": "const API_URL = 'http://localhost:8080'",
  "repl": "const API_URL = 'https://api.example.com'",
  "mode": "literal"
}
```

**示例 2**：正则替换（使用通配符避免写完整内容）
```json
{
  "relative_path": "src/api/user.ts",
  "needle": "export async function login.*?\\}",
  "repl": "export async function login(params: LoginParams) {\n  return request.post('/auth/login', params)\n}",
  "mode": "regex"
}
```

**技巧**：使用 `.*?` 通配符可以避免写出完整的原始内容！

---

### 5.7 insert_after_symbol / insert_before_symbol

**功能**：在符号前后插入代码

**参数**：
- `name_path` (必需): 参考符号
- `relative_path` (必需): 文件路径
- `body` (必需): 要插入的代码

**示例**：在类末尾添加新方法
```json
{
  "name_path": "UserController/lastMethod",
  "relative_path": "src/controllers/UserController.java",
  "body": "\n\npublic Result newMethod() {\n    return Result.ok();\n}"
}
```

---

### 5.8 rename_symbol

**功能**：全局重命名符号

**参数**：
- `name_path` (必需): 符号名称路径
- `relative_path` (必需): 符号所在文件
- `new_name` (必需): 新名称

**示例**：
```json
{
  "name_path": "getUserInfo",
  "relative_path": "src/api/user.ts",
  "new_name": "fetchUserProfile"
}
```

---

## 六、优化建议

### 6.1 工具精简建议

根据使用频率，建议保留以下工具：

**必须保留（核心能力）**：
- ✅ `get_symbols_overview`
- ✅ `find_symbol`
- ✅ `find_referencing_symbols`
- ✅ `search_for_pattern`
- ✅ `list_dir`
- ✅ `find_file`

**按需保留（编辑能力）**：
- ⚠️ `replace_symbol_body`
- ⚠️ `replace_content`
- ⚠️ `insert_after_symbol`
- ⚠️ `insert_before_symbol`
- ⚠️ `rename_symbol`

**可以关闭（辅助功能）**：
- ❌ `read_memory` / `write_memory` / `edit_memory` / `delete_memory` / `list_memories`
- ❌ `activate_project` / `get_current_config`
- ❌ `check_onboarding_performed` / `onboarding`
- ❌ `think_about_*` 系列
- ❌ `initial_instructions`

### 6.2 Token 节省效果

| 配置 | 工具数 | 预估 Token |
|------|--------|------------|
| 全部开启 | ~20 | ~7,000 |
| 推荐配置（核心+编辑） | ~11 | ~4,000 |
| 最小配置（仅核心） | ~6 | ~2,000 |

### 6.3 最佳实践

1. **先概览，后深入**
   - 先用 `get_symbols_overview` 了解结构
   - 再用 `find_symbol` 获取具体内容

2. **控制 include_body**
   - 不需要代码时设为 `false`
   - 只在真正需要阅读/编辑时设为 `true`

3. **善用 relative_path**
   - 总是尽可能限制搜索范围
   - 减少不必要的全局搜索

4. **正则替换用通配符**
   - 使用 `.*?` 避免写完整原文
   - 减少 token 消耗和出错概率

---

## 七、项目配置

### 7.1 配置文件位置

```
项目根目录/
├── .serena/
│   ├── config.yml          # Serena 配置
│   └── memories/           # 项目记忆存储
│       ├── project_overview.md
│       ├── code_style.md
│       └── ...
```

### 7.2 当前项目记忆

本项目已保存的记忆：
- `project_overview` - 项目概览（技术栈、结构）
- `suggested_commands` - 推荐命令
- `code_style` - 代码风格规范
- `task_completion` - 任务完成记录

---

## 八、常见问题

### Q1: Serena 和 Cursor 内置工具有什么区别？

| 功能 | Cursor 内置 | Serena |
|------|-------------|--------|
| 文件读取 | 读取整个文件 | 只读取符号 |
| 代码搜索 | 文本搜索 | 语义搜索 |
| 引用查找 | 需要 grep | LSP 级别精准 |
| 重命名 | 文本替换 | 全局安全重命名 |

### Q2: 什么时候用 Serena，什么时候用 Cursor 内置？

- **用 Serena**：代码导航、重构、查找引用、理解代码结构
- **用 Cursor 内置**：读取配置文件、非代码文件、简单文本搜索

### Q3: 关闭部分工具后会影响使用吗？

不会影响核心功能。只要保留 `find_symbol`、`get_symbols_overview`、`search_for_pattern` 这几个，代码分析能力就完整保留。

---

## 九、附录：LSP Symbol Kind 对照表

在 `include_kinds` / `exclude_kinds` 参数中使用：

| Kind | 值 | 说明 |
|------|-----|------|
| File | 1 | 文件 |
| Module | 2 | 模块 |
| Namespace | 3 | 命名空间 |
| Package | 4 | 包 |
| Class | 5 | 类 |
| Method | 6 | 方法 |
| Property | 7 | 属性 |
| Field | 8 | 字段 |
| Constructor | 9 | 构造函数 |
| Enum | 10 | 枚举 |
| Interface | 11 | 接口 |
| Function | 12 | 函数 |
| Variable | 13 | 变量 |
| Constant | 14 | 常量 |
| String | 15 | 字符串 |
| Number | 16 | 数字 |
| Boolean | 17 | 布尔 |
| Array | 18 | 数组 |
| Object | 19 | 对象 |
| Key | 20 | 键 |
| Null | 21 | 空 |
| EnumMember | 22 | 枚举成员 |
| Struct | 23 | 结构体 |
| Event | 24 | 事件 |
| Operator | 25 | 操作符 |
| TypeParameter | 26 | 类型参数 |

---

*文档版本: 1.0*
*更新日期: 2026-01-22*
*适用项目: 慕声报税管理系统*
