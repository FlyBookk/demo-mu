# Claude Code 本地配置指南

> 本文档详细说明 Claude Code CLI 工具的本地配置文件位置、结构和使用方法。

## 目录

- [配置文件总览](#配置文件总览)
- [核心配置文件详解](#核心配置文件详解)
- [目录结构说明](#目录结构说明)
- [API 配置指南](#api-配置指南)
- [插件管理](#插件管理)
- [常用命令](#常用命令)
- [故障排除](#故障排除)

---

## 配置文件总览

Claude Code 的配置文件主要存放在以下位置：

| 文件/目录 | 路径 | 用途 |
|----------|------|------|
| 主配置文件 | `~/.claude.json` | 全局设置、统计、功能开关 |
| 设置文件 | `~/.claude/settings.json` | **API Key、Base URL、模型配置** |
| 本地设置 | `~/.claude/settings.local.json` | 权限设置、已启用插件 |
| 基础配置 | `~/.claude/config.json` | API Key 优先级配置 |
| 配置备份 | `~/.claude.json.backup` | 主配置文件的自动备份 |

---

## 核心配置文件详解

### 1. `~/.claude/settings.json` - API 配置（最重要）

这是配置第三方 API 代理的核心文件：

```json
{
  "env": {
    "ANTHROPIC_AUTH_TOKEN": "你的API_KEY",
    "ANTHROPIC_BASE_URL": "https://你的代理地址",
    "ANTHROPIC_MODEL": "claude-sonnet-4-5-20250929",
    "ANTHROPIC_DEFAULT_SONNET_MODEL": "claude-sonnet-4-5-20250929",
    "ANTHROPIC_DEFAULT_HAIKU_MODEL": "claude-sonnet-4-5-20250929",
    "ANTHROPIC_DEFAULT_OPUS_MODEL": "claude-sonnet-4-5-20250929"
  }
}
```

**配置项说明：**

| 环境变量 | 说明 |
|---------|------|
| `ANTHROPIC_AUTH_TOKEN` | API 密钥（第三方代理常用） |
| `ANTHROPIC_API_KEY` | API 密钥（官方标准格式） |
| `ANTHROPIC_BASE_URL` | API 基础地址（默认为官方地址） |
| `ANTHROPIC_MODEL` | 默认使用的模型 |
| `ANTHROPIC_DEFAULT_SONNET_MODEL` | Sonnet 模型指定 |
| `ANTHROPIC_DEFAULT_HAIKU_MODEL` | Haiku 模型指定 |
| `ANTHROPIC_DEFAULT_OPUS_MODEL` | Opus 模型指定 |

### 2. `~/.claude/settings.local.json` - 权限和插件

```json
{
  "permissions": {
    "allow": [
      "Bash(brew install:*)",
      "Bash(mysql:*)",
      "WebSearch"
    ]
  },
  "enabledPlugins": {
    "superpowers@claude-plugins-official": true,
    "claude-hud@claude-hud": true
  }
}
```

**说明：**
- `permissions.allow`: 已授权的命令模式列表
- `enabledPlugins`: 已启用的插件

### 3. `~/.claude/config.json` - 基础配置

```json
{
  "primaryApiKey": "any"
}
```

### 4. `~/.claude.json` - 主配置文件

包含全局设置、使用统计、功能开关等。主要字段：

```json
{
  "numStartups": 92,                    // 启动次数
  "installMethod": "global",            // 安装方式
  "theme": "dark-ansi",                 // 主题
  "customApiKeyResponses": {            // API Key 审批状态
    "approved": [],
    "rejected": ["sk-xxx"]
  },
  "tipsHistory": { ... },               // 提示历史
  "cachedStatsigGates": { ... }         // 功能开关缓存
}
```

---

## 目录结构说明

```
~/.claude/
├── settings.json          # API 配置（重要）
├── settings.local.json    # 权限和插件配置
├── config.json            # 基础配置
├── history.jsonl          # 命令历史记录
├── cache/                 # 缓存目录
├── debug/                 # 调试日志
├── file-history/          # 文件操作历史
├── ide/                   # IDE 集成相关
├── paste-cache/           # 粘贴缓存
├── plugins/               # 插件目录
│   ├── cache/             # 插件缓存
│   ├── installed_plugins.json  # 已安装插件列表
│   ├── known_marketplaces.json # 已知市场列表
│   └── marketplaces/      # 市场数据
├── projects/              # 项目配置和会话
│   └── -Users-xxx-project-xxx/  # 每个项目的会话数据
├── session-env/           # 会话环境变量
├── shell-snapshots/       # Shell 快照
├── skills/                # 技能目录
├── stats-cache.json       # 统计缓存
├── statsig/               # 功能开关数据
├── telemetry/             # 遥测数据
└── todos/                 # 待办事项
```

---

## API 配置指南

### 使用官方 API

1. 访问 [Anthropic Console](https://console.anthropic.com/) 获取 API Key
2. 编辑 `~/.claude/settings.json`：

```json
{
  "env": {
    "ANTHROPIC_API_KEY": "sk-ant-api03-你的密钥"
  }
}
```

### 使用第三方代理

编辑 `~/.claude/settings.json`：

```json
{
  "env": {
    "ANTHROPIC_AUTH_TOKEN": "你的代理密钥",
    "ANTHROPIC_BASE_URL": "https://你的代理地址",
    "ANTHROPIC_MODEL": "claude-sonnet-4-5-20250929"
  }
}
```

**常见代理配置示例：**

```json
{
  "env": {
    "ANTHROPIC_AUTH_TOKEN": "sk-XSSnZymXbTFJrURg",
    "ANTHROPIC_BASE_URL": "https://stan.ai678.top",
    "ANTHROPIC_MODEL": "claude-sonnet-4-5-20250929"
  }
}
```

### 注意事项

1. **URL 路径问题**：某些代理需要添加 `/v1`，某些不需要。如果报错 `/v1/v1/messages`，说明路径重复，需要调整。

2. **环境变量优先级**：
   - 系统环境变量 > 配置文件
   - 如果同时设置了多种认证方式，会产生冲突

3. **认证冲突解决**：
   ```bash
   # 清除系统环境变量
   unset ANTHROPIC_AUTH_TOKEN
   unset ANTHROPIC_API_KEY
   
   # 登出官方认证
   claude /logout
   ```

---

## 插件管理

### /plugin 命令详解

在 Claude Code 中使用 `/plugin` 命令可以进入插件管理界面，包含四个主要标签页：

| 标签页 | 功能 | 说明 |
|--------|------|------|
| **Discover** | 发现插件 | 从已添加的 Marketplaces 中展示可安装的插件 |
| **Installed** | 已安装 | 显示当前作用域内已安装的插件 |
| **Marketplaces** | 市场管理 | 添加/移除插件市场源 |

---

### Discover（发现插件）

**Q: Discover 中的插件从哪里来？**

Discover 列表中的插件来自你添加的 **Marketplaces（插件市场）**，而不是从本机检测。

- 默认添加的市场是 `claude-plugins-official`（Anthropic 官方插件市场）
- 市场数据存储在 `~/.claude/plugins/marketplaces/` 目录
- 每个市场是一个 GitHub 仓库，通过 git clone 到本地

**市场包含两类插件：**

1. **内置插件**（`plugins/` 目录）：如 `jdtls-lsp`、`code-review`、`frontend-design` 等
2. **外部插件**（`external_plugins/` 目录）：如 `context7`、`github`、`playwright` 等，这些通常是第三方 MCP Server 的封装

**Q: MCP 和插件的关系？**

- **MCP 不会自动变成 Installed**：你在项目中配置的 MCP Server（如 `.mcp.json`）是独立于插件系统的
- **插件可以包含 MCP**：部分插件（如 `context7`）本质上是对 MCP Server 的封装
- **Discover 中的插件需要手动安装**：按 Space 键切换选中状态，然后按 Enter 安装

---

### Installed（已安装插件）

**Q: 如何区分全局安装和项目安装？**

插件有两种作用域（scope）：

| Scope | 作用范围 | 显示位置 |
|-------|---------|---------|
| `user` | 全局安装，所有项目可用 | 在任何目录都能看到 |
| `local` | 项目安装，仅当前项目可用 | 只在指定项目目录下能看到 |

**实际示例**（来自 `~/.claude/plugins/installed_plugins.json`）：

```json
{
  "version": 2,
  "plugins": {
    "jdtls-lsp@claude-plugins-official": [
      {
        "scope": "user",           // 全局安装
        "installPath": "~/.claude/plugins/cache/.../jdtls-lsp/1.0.0",
        "version": "1.0.0"
      }
    ],
    "superpowers@claude-plugins-official": [
      {
        "scope": "local",          // 项目安装
        "projectPath": "/Users/wanhua",  // 绑定的项目路径
        "installPath": "~/.claude/plugins/cache/.../superpowers/4.1.0",
        "version": "4.1.0"
      }
    ]
  }
}
```

**为什么不同目录看到的 Installed 不一样？**

- `scope: user` 的插件：在任何目录都显示
- `scope: local` 的插件：只在 `projectPath` 及其子目录下显示

**如何选择安装作用域？**

安装插件时，Claude Code 会询问你选择作用域：
- 选择 **User** → 全局可用
- 选择 **Local** → 仅当前项目可用

**⚠️ 注意：UI 不显示 scope**

在 `/plugin` → `Installed` 界面中，**无法直接看到插件的 scope**（这是 Claude Code 的 UI 缺陷）。

查看插件作用域的方法：

```bash
# 方法 1：直接查看配置文件
cat ~/.claude/plugins/installed_plugins.json

# 方法 2：用 jq 格式化查看（如果安装了 jq）
cat ~/.claude/plugins/installed_plugins.json | jq '.plugins | to_entries[] | {name: .key, scope: .value[0].scope, projectPath: .value[0].projectPath}'
```

示例输出：
```json
{"name": "superpowers@claude-plugins-official", "scope": "local", "projectPath": "/Users/wanhua"}
{"name": "jdtls-lsp@claude-plugins-official", "scope": "user", "projectPath": null}
```

---

### Marketplaces（插件市场）

**Q: Marketplaces 是什么？里面的东西怎么来的？**

Marketplaces 是插件的来源仓库，本质上是 **GitHub 仓库**。

**已知的官方/社区市场：**

| 市场名称 | GitHub 仓库 | 说明 |
|---------|------------|------|
| `claude-plugins-official` | `anthropics/claude-plugins-official` | Anthropic 官方插件市场（默认） |
| `superpowers-marketplace` | `obra/superpowers-marketplace` | 社区市场 |
| `claude-hud` | `jarrodwatts/claude-hud` | HUD 插件市场 |

**市场数据存储：**

```
~/.claude/plugins/
├── known_marketplaces.json    # 已添加的市场列表
└── marketplaces/              # 市场仓库的本地克隆
    ├── claude-plugins-official/
    │   ├── plugins/           # 内置插件
    │   │   ├── jdtls-lsp/
    │   │   ├── code-review/
    │   │   └── ...
    │   └── external_plugins/  # 外部插件（MCP封装）
    │       ├── context7/
    │       ├── github/
    │       └── ...
    ├── superpowers-marketplace/
    └── claude-hud/
```

**添加新市场：**

1. 在 `/plugin` → `Marketplaces` 标签页
2. 选择 `+ Add Marketplace`
3. 输入 GitHub 仓库地址（格式：`owner/repo`）

---

### 插件启用状态

已安装的插件不一定是启用状态。启用状态存储在：

- **全局设置**：`~/.claude/settings.local.json`
- **项目设置**：`项目目录/.claude/settings.local.json`

```json
{
  "enabledPlugins": {
    "superpowers@claude-plugins-official": true,
    "claude-hud@claude-hud": true,
    "jdtls-lsp@claude-plugins-official": false  // 已安装但未启用
  }
}
```

---

### 插件文件结构

```
~/.claude/plugins/
├── cache/                        # 已安装插件的实际文件
│   ├── claude-plugins-official/
│   │   ├── superpowers/4.1.0/
│   │   └── jdtls-lsp/1.0.0/
│   └── claude-hud/
│       └── claude-hud/0.0.6/
├── installed_plugins.json        # 已安装插件索引
├── known_marketplaces.json       # 已添加的市场列表
├── install-counts-cache.json     # 安装统计缓存
└── marketplaces/                 # 市场仓库克隆
    ├── claude-plugins-official/
    ├── superpowers-marketplace/
    └── claude-hud/
```

---

### 常见问题总结

| 问题 | 答案 |
|------|------|
| Discover 里的插件从哪来？ | 来自已添加的 Marketplaces（GitHub 仓库） |
| 配置了 MCP 会自动安装插件吗？ | 不会，MCP 和插件是独立的系统 |
| 为什么不同目录看到的 Installed 不一样？ | 因为有 `user`（全局）和 `local`（项目）两种作用域 |
| Marketplaces 里的东西怎么来的？ | 是 GitHub 仓库，通过 git clone 到本地 |
| 如何安装插件到全局？ | 安装时选择 `User` 作用域 |
| 如何安装插件到当前项目？ | 安装时选择 `Local` 作用域 |

---

### 插件 Token 消耗分析

以下分析基于三个常见插件：`superpowers`、`claude-hud`、`jdtls-lsp`。

#### 1. superpowers（Token 消耗：中高）

**工作机制：**
- 类型：Skills（技能库）+ Hooks + Commands + Agents
- 触发时机：每次会话启动（SessionStart hook）

**Token 消耗明细：**

| 消耗来源 | 时机 | 预估 Token | 说明 |
|---------|------|-----------|------|
| SessionStart Hook | 每次启动/恢复/清除会话 | ~1,000 tokens | 注入 `using-superpowers` skill (~3.8KB) 到上下文 |
| Skill 调用 | 按需，模型主动调用 | 每个 skill 500-2000 tokens | 有 14 个 skills，模型判断是否需要 |
| Commands | 用户手动触发 | 取决于命令 | `/brainstorm`、`/execute-plan`、`/write-plan` |
| Agents | 按需调用 | 额外 API 调用 | `code-reviewer` agent 会启动子对话 |

**总结：**
- **固定消耗**：每次会话启动 ~1,000 tokens
- **变动消耗**：取决于模型调用多少 skills（模型会主动判断是否需要调用）
- **Agent 调用会产生额外 API 调用**（独立计费）

**SessionStart 注入的内容：**
```
<EXTREMELY_IMPORTANT>
You have superpowers.
[using-superpowers skill 完整内容，约 3.8KB]
</EXTREMELY_IMPORTANT>
```

---

#### 2. claude-hud（Token 消耗：零）

**工作机制：**
- 类型：StatusLine（状态栏显示）
- 触发时机：Claude Code 每 ~300ms 调用一次

**Token 消耗明细：**

| 消耗来源 | Token 消耗 | 说明 |
|---------|-----------|------|
| 状态栏渲染 | **0** | 纯本地计算，不调用模型 |
| Transcript 解析 | **0** | 读取本地 JSONL 文件 |
| Usage API 调用 | **0** | 调用 Anthropic API 获取用量（不消耗 token） |

**工作流程：**
```
Claude Code → stdin JSON → Node.js 解析 → 渲染状态栏 → stdout → 显示
           ↘ 读取 transcript JSONL → 解析 tools/agents/todos
```

**总结：**
- **完全不消耗 token**
- 只是一个本地运行的 Node.js 程序，读取 Claude Code 提供的数据并渲染显示
- 状态栏显示的 token 使用量是**读取**当前会话的统计，不是消耗

---

#### 3. jdtls-lsp（Token 消耗：零/间接）

**工作机制：**
- 类型：LSP（语言服务器协议）插件
- 触发时机：编辑 Java 文件时

**Token 消耗明细：**

| 消耗来源 | Token 消耗 | 说明 |
|---------|-----------|------|
| LSP 服务器运行 | **0** | 本地运行 Eclipse JDT.LS |
| 代码补全/诊断 | **0** | LSP 协议通信，不涉及模型 |
| 模型使用 LSP 结果 | 间接 | LSP 结果可能被模型读取到上下文 |

**总结：**
- **LSP 本身不消耗 token**
- 但 LSP 提供的代码智能结果（如诊断信息、类型信息）可能会被模型作为上下文使用
- 这种间接消耗取决于 Claude Code 如何集成 LSP 数据

---

#### 插件 Token 消耗对比总览

| 插件 | 固定消耗 | 变动消耗 | 是否调用模型 |
|------|---------|---------|-------------|
| **superpowers** | ~1,000 tokens/会话 | 500-2000 tokens/skill | 是（注入上下文 + Skill 调用） |
| **claude-hud** | 0 | 0 | 否（纯本地渲染） |
| **jdtls-lsp** | 0 | 间接 | 否（LSP 是本地服务） |

---

#### 如何监控 Token 消耗

**方法 1：使用 `/cost` 命令**
```
/cost
```
显示当前会话的 token 使用量和费用估算。

**方法 2：使用 claude-hud 状态栏**

如果启用了 claude-hud，状态栏会实时显示：
```
[Opus | Pro] █████░░░░░ 45% | my-project git:(main) | 5h: 25%
```
- `45%` 是上下文窗口使用率
- `5h: 25%` 是 5 小时内的配额使用率（如果配置了 OAuth）

**方法 3：查看 transcript 文件**

每个会话的详细记录在：
```
~/.claude/projects/<project-path>/sessions/<session-id>.jsonl
```

---

#### 优化建议

1. **superpowers**：如果你不需要 TDD/调试等工作流，可以禁用以节省 ~1,000 tokens/会话
2. **claude-hud**：放心使用，不消耗任何 token
3. **jdtls-lsp**：只在需要 Java 开发时启用

**禁用插件的方法：**
- `/plugin` → `Installed` → 选择插件 → `Disable plugin`
- 或编辑 `~/.claude/settings.local.json` 设置 `enabledPlugins`

---

## 常用命令

### 在 Claude Code 中使用

| 命令 | 说明 |
|------|------|
| `/help` | 显示帮助信息 |
| `/logout` | 登出当前认证 |
| `/login` | 登录 Anthropic 账号 |
| `/init` | 初始化项目，创建 CLAUDE.md |
| `/clear` | 清除当前对话 |
| `/model` | 切换模型（如 `/model opus`） |
| `/config` | 查看/编辑配置 |
| `/memory` | 管理记忆 |
| `/cost` | 查看使用成本 |

### 在系统终端中使用

```bash
# 启动 Claude Code
claude

# 在指定目录启动
claude /path/to/project

# 查看版本
claude --version

# 查看帮助
claude --help
```

---

## 故障排除

### 问题 1：认证冲突

**症状：**
```
Auth conflict: Both a token (ANTHROPIC_AUTH_TOKEN) and an API key are set.
```

**解决：**
```bash
# 方法 1：清除环境变量
unset ANTHROPIC_AUTH_TOKEN
unset ANTHROPIC_API_KEY

# 方法 2：只在配置文件中设置一种认证方式
# 编辑 ~/.claude/settings.json
```

### 问题 2：无效令牌 (401 错误)

**症状：**
```
401 {"error":{"message":"无效的令牌"}}
```

**解决：**
1. 检查 API Key 是否正确
2. 检查 API Key 是否过期
3. 联系代理服务商确认 Key 状态

### 问题 3：URL 路径重复

**症状：**
```
Invalid URL (POST /v1/v1/messages)
```

**解决：**
调整 `ANTHROPIC_BASE_URL`：
- 如果当前是 `https://xxx.com/v1`，改为 `https://xxx.com`
- 或反之

### 问题 4：需要订阅

**症状：**
```
Claude Max or Pro is required to connect to Claude Code
```

**解决：**
- 使用第三方代理（配置 `ANTHROPIC_BASE_URL`）
- 或升级 Anthropic 订阅
- 或使用 API Key 按量付费

---

## 配置文件快速编辑

### 使用命令行编辑

```bash
# 使用 nano
nano ~/.claude/settings.json

# 使用 vim
vim ~/.claude/settings.json

# 使用 VS Code
code ~/.claude/settings.json
```

### 快速备份配置

```bash
# 备份所有配置
cp -r ~/.claude ~/.claude.backup.$(date +%Y%m%d)
cp ~/.claude.json ~/.claude.json.backup.$(date +%Y%m%d)
```

### 重置配置

```bash
# 删除配置（谨慎操作）
rm -rf ~/.claude
rm ~/.claude.json

# 重新启动 Claude Code 会重新生成默认配置
claude
```

---

## 参考链接

- [Claude Code 官方文档](https://docs.anthropic.com/en/docs/claude-code)
- [Anthropic Console](https://console.anthropic.com/)
- [Claude Code GitHub](https://github.com/anthropics/claude-code)

---

*文档更新时间：2026-01-24*
*Claude Code 版本：v2.1.17*
