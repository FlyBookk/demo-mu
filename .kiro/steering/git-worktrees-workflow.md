---
inclusion: manual
---

# Git Worktrees 工作流（可选）

基于 Superpowers 的 Git worktrees 隔离开发模式，适用于大型功能开发。

## 什么是 Git Worktrees

Git worktrees 允许你在同一个仓库中同时检出多个分支到不同的目录，实现完全隔离的开发环境。

## 使用场景

### ✅ 适合使用 Worktrees

- 大型功能开发（预计 > 1 天）
- 需要频繁切换分支
- 多个功能并行开发
- 需要保持主分支干净
- 实验性功能开发

### ❌ 不需要 Worktrees

- 小型 bug 修复（< 1 小时）
- 紧急热修复
- 简单配置修改
- 单人单功能开发

## 工作流程

### 1. 创建功能分支和 Worktree

```bash
# 在主项目目录（例如：quickcem-im）
cd quickcem-im

# 创建功能分支并在新目录中检出
git worktree add ../quickcem-im-user-cache feature/user-cache

# 进入新的工作目录
cd ../quickcem-im-user-cache
```

**目录结构**：
```
quickcem/
├── quickcem-im/              # 主分支（main/develop）
└── quickcem-im-user-cache/   # 功能分支（feature/user-cache）
```

### 2. 在 Worktree 中开发

```bash
# 在 quickcem-im-user-cache 目录中工作
cd ../quickcem-im-user-cache

# 正常开发流程
# 1. 拆分任务
# 2. TDD 开发
# 3. 代码审查
# 4. 提交代码

git add .
git commit -m "feat: 实现用户缓存功能"
```

### 3. 测试和验证

```bash
# 编译测试
mvn clean compile
mvn test

# 本地运行验证
mvn spring-boot:run
```

### 4. 合并到主分支

```bash
# 切换回主项目目录
cd ../quickcem-im

# 拉取最新代码
git pull origin develop

# 合并功能分支
git merge feature/user-cache

# 解决冲突（如有）
# 运行测试确保合并正确
mvn test

# 推送到远程
git push origin develop
```

### 5. 清理 Worktree

```bash
# 删除功能分支
git branch -d feature/user-cache

# 删除 worktree
git worktree remove ../quickcem-im-user-cache

# 或者直接删除目录（Git 会自动清理）
rm -rf ../quickcem-im-user-cache
git worktree prune
```

## Kiro 集成使用

### 方式 1：手动创建 Worktree

```bash
# 你手动创建 worktree
git worktree add ../quickcem-im-feature feature/xxx
cd ../quickcem-im-feature

# 然后让 Kiro 在这个目录中工作
# Kiro 会自动识别当前分支
```

### 方式 2：让 Kiro 创建 Worktree

```
请为"用户缓存功能"创建一个 git worktree，分支名为 feature/user-cache
```

Kiro 会执行：
```bash
git worktree add ../quickcem-im-user-cache feature/user-cache
```

### 方式 3：完整自动化流程

```
使用 worktree 模式开发"用户缓存功能"：
1. 创建 feature/user-cache 分支和 worktree
2. 拆分任务
3. 执行所有任务
4. 运行测试
5. 合并回主分支
```

## 常用命令

### 查看所有 Worktrees

```bash
git worktree list
```

输出示例：
```
/path/to/quickcem-im              abc123 [develop]
/path/to/quickcem-im-user-cache   def456 [feature/user-cache]
/path/to/quickcem-im-hotfix       ghi789 [hotfix/critical-bug]
```

### 删除 Worktree

```bash
# 方式 1：使用 git 命令
git worktree remove ../quickcem-im-user-cache

# 方式 2：直接删除目录
rm -rf ../quickcem-im-user-cache
git worktree prune  # 清理 Git 记录
```

### 在 Worktrees 之间切换

```bash
# 不需要 git checkout，直接 cd 到对应目录
cd ../quickcem-im              # 主分支
cd ../quickcem-im-user-cache   # 功能分支
```

## 最佳实践

### 1. 命名规范

```bash
# 功能开发
git worktree add ../quickcem-im-{feature-name} feature/{feature-name}

# Bug 修复
git worktree add ../quickcem-im-{bug-name} bugfix/{bug-name}

# 热修复
git worktree add ../quickcem-im-{hotfix-name} hotfix/{hotfix-name}
```

### 2. 目录组织

```
quickcem/
├── quickcem-im/                    # 主分支（长期保留）
├── quickcem-im-user-cache/         # 功能分支（开发完删除）
├── quickcem-im-redis-upgrade/      # 功能分支（开发完删除）
└── quickcem-im-hotfix-memory/      # 热修复分支（修复完删除）
```

### 3. 定期清理

```bash
# 每周清理一次已合并的 worktrees
git worktree list
git worktree remove <path>
git worktree prune
```

### 4. 避免冲突

- 不要在多个 worktrees 中修改同一个文件
- 定期从主分支同步代码：`git merge develop`
- 功能开发完成后立即合并和清理

## 与简化工作流对比

| 特性 | Worktrees 模式 | 简化模式 |
|-----|--------------|---------|
| 隔离性 | ✅ 完全隔离 | ❌ 同一目录 |
| 切换成本 | ✅ 无需切换 | ❌ 需要 checkout |
| 并行开发 | ✅ 支持 | ❌ 不支持 |
| 磁盘占用 | ❌ 多份代码 | ✅ 单份代码 |
| 学习成本 | ❌ 较高 | ✅ 较低 |
| 适用场景 | 大型功能 | 小型修改 |

## 故障排除

### 问题 1：无法删除 Worktree

```bash
# 错误：worktree 目录不存在
git worktree prune

# 错误：分支被锁定
rm -rf .git/worktrees/{branch-name}
```

### 问题 2：Worktree 中的依赖问题

```bash
# 在新 worktree 中重新安装依赖
cd ../quickcem-im-user-cache
mvn clean install
```

### 问题 3：IDE 识别问题

- IntelliJ IDEA：File → Open → 选择 worktree 目录
- VS Code：File → Open Folder → 选择 worktree 目录
- Kiro：自动识别当前目录的 Git 分支

## 总结

Git worktrees 是一个强大但可选的工具：

- ✅ **推荐使用**：大型功能、并行开发、需要隔离
- ⚠️ **谨慎使用**：小型修改、紧急修复、学习阶段
- ❌ **不要使用**：不熟悉 Git、单人单任务、简单配置

如果不确定，可以先使用简化模式（直接在当前分支开发），等熟悉后再尝试 worktrees。
