# Superpowers 工作流快速参考

## 🚀 快速开始

### 简化模式（推荐小型功能）

```bash
# 1. 创建功能分支
git checkout -b feature/xxx

# 2. 让 Kiro 拆分任务
"请将'XXX功能'拆分为详细任务"

# 3. 执行所有任务
"执行所有任务"

# 4. 合并代码
git checkout develop
git merge feature/xxx
git push origin develop
```

### Worktrees 模式（推荐大型功能）

```bash
# 1. 创建 worktree
git worktree add ../project-feature feature/xxx
cd ../project-feature

# 2. 让 Kiro 拆分任务
"请将'XXX功能'拆分为详细任务"

# 3. 执行所有任务
"执行所有任务"

# 4. 合并代码
cd ../project
git merge feature/xxx
git push origin develop

# 5. 清理 worktree
git worktree remove ../project-feature
```

## 📋 常用 Kiro 命令

### 任务管理

```
# 拆分任务
请将"用户缓存功能"拆分为详细任务

# 执行单个任务
执行任务 1.1

# 执行所有任务
执行所有任务（使用子代理）

# 查看任务状态
显示任务列表
```

### 代码审查

```
# 手动触发审查
请审查刚才修改的代码

# 审查特定文件
请审查 UserServiceImpl.java
```

### Git 操作

```
# 创建 worktree
使用 worktree 模式创建 feature/xxx 分支

# 合并分支
请帮我合并 feature/xxx 分支到 develop

# 清理 worktree
请清理 worktree 和功能分支
```

### 测试

```
# 运行测试
请运行所有测试

# 运行特定测试
请运行 UserServiceTest

# 查看测试覆盖率
请生成测试覆盖率报告
```

## 🔄 TDD 循环

```
1. RED   → 写失败测试
2. GREEN → 写最小实现
3. REFACTOR → 优化代码
4. COMMIT → 提交代码
```

### 示例

```java
// 1. RED - 写测试
@Test
public void testGetUser_ShouldReturnUser() {
    UserVO user = userService.getUserById(1L);
    assertNotNull(user);
}

// 运行测试 → 失败 ✅

// 2. GREEN - 实现
@Override
public UserVO getUserById(Long userId) {
    UserPO po = userMapper.selectById(userId);
    return BeanUtil.toBean(po, UserVO.class);
}

// 运行测试 → 通过 ✅

// 3. REFACTOR - 优化
@Override
public UserVO getUserById(Long userId) {
    return Optional.ofNullable(userMapper.selectById(userId))
            .map(po -> BeanUtil.toBean(po, UserVO.class))
            .orElse(null);
}

// 运行测试 → 仍然通过 ✅

// 4. COMMIT
git add .
git commit -m "feat: 实现 getUserById 方法"
```

## ✅ 代码审查检查点

### 必须检查（Critical）

- [ ] 编译通过
- [ ] 测试通过
- [ ] 使用 `@Resource` 注入
- [ ] 使用 `ResponseResult` 封装响应

### 强烈建议（Major）

- [ ] 添加方法注释（@author、@param、@return）
- [ ] 使用 `StringUtils.isBlank()`
- [ ] 使用 `CollectionUtils.isEmpty()`
- [ ] 添加异常处理和日志

### 可选优化（Minor）

- [ ] 优化命名
- [ ] 减少嵌套层级
- [ ] 提取重复代码

## 📁 文件结构

```
.kiro/
├── steering/                          # 指导文件
│   ├── tdd-workflow.md               # TDD 流程（始终加载）
│   ├── code-review-checklist.md      # 代码审查（始终加载）
│   ├── task-breakdown.md             # 任务拆分（始终加载）
│   ├── git-worktrees-workflow.md     # Git Worktrees（手动加载）
│   └── superpowers-workflow-guide.md # 完整指南（手动加载）
├── hooks/                             # 自动化钩子
│   ├── enforce-tdd.json              # 强制 TDD
│   ├── auto-code-review.json         # 自动审查
│   └── run-tests-on-commit.json      # 提交前测试
└── WORKFLOW-QUICK-REFERENCE.md       # 本文件
```

## 🎯 任务拆分原则

### 时间限制

- 目标：2-5 分钟/任务
- 最长：10 分钟
- 最短：1 分钟

### 任务类型

```markdown
# 创建实体（2-3 分钟）
- [ ] 1.1 创建 UserPO 类
- [ ] 1.2 创建 UserDTO 类
- [ ] 1.3 创建 UserVO 类

# 实现 CRUD（3-5 分钟/方法）
- [ ] 2.1 实现 create 方法（TDD）
- [ ] 2.2 实现 getById 方法（TDD）
- [ ] 2.3 实现 update 方法（TDD）
- [ ] 2.4 实现 delete 方法（TDD）

# 实现 API（3-5 分钟/接口）
- [ ] 3.1 实现 POST /users 接口
- [ ] 3.2 实现 GET /users/{id} 接口
- [ ] 3.3 实现 PUT /users/{id} 接口
- [ ] 3.4 实现 DELETE /users/{id} 接口

# 添加缓存（3-5 分钟/逻辑）
- [ ] 4.1 创建缓存 Key 常量
- [ ] 4.2 实现缓存读取逻辑
- [ ] 4.3 实现缓存写入逻辑
- [ ] 4.4 实现缓存失效逻辑
```

## 🛠️ 常用 Maven 命令

```bash
# 编译
mvn clean compile

# 运行测试
mvn test

# 运行特定测试
mvn test -Dtest=UserServiceTest

# 运行特定测试方法
mvn test -Dtest=UserServiceTest#testGetUser

# 跳过测试
mvn clean install -DskipTests

# 生成测试覆盖率报告
mvn jacoco:report

# 运行集成测试
mvn verify

# 本地运行
mvn spring-boot:run
```

## 🔧 Hooks 管理

### 启用 Hook

```bash
# Hook 文件存在即启用
ls .kiro/hooks/*.json
```

### 禁用 Hook

```bash
# 重命名为 .disabled
mv .kiro/hooks/enforce-tdd.json .kiro/hooks/enforce-tdd.json.disabled
```

### 恢复 Hook

```bash
# 去掉 .disabled 后缀
mv .kiro/hooks/enforce-tdd.json.disabled .kiro/hooks/enforce-tdd.json
```

## 📊 工作流对比

| 特性 | 简化模式 | Worktrees 模式 |
|-----|---------|---------------|
| 适用场景 | 小型功能 | 大型功能 |
| 学习成本 | 低 | 中 |
| 隔离性 | 低 | 高 |
| 并行开发 | 不支持 | 支持 |
| 磁盘占用 | 低 | 高 |
| 切换成本 | 高（需 checkout） | 低（直接 cd） |

## 💡 最佳实践

### 1. 任务拆分

- ✅ 每个任务 2-5 分钟
- ✅ 任务独立可测试
- ✅ 明确标注依赖关系
- ❌ 避免任务过大或过小

### 2. TDD 开发

- ✅ 先写测试，再写实现
- ✅ 测试失败后再实现
- ✅ 实现通过后再重构
- ❌ 不要跳过测试阶段

### 3. 代码审查

- ✅ 每个任务完成后审查
- ✅ 修复 Critical 问题
- ✅ 考虑 Major 问题
- ⚠️ Minor 问题可选

### 4. Git 提交

- ✅ 每个任务提交一次
- ✅ 提交信息清晰
- ✅ 提交前运行测试
- ❌ 不要提交未测试代码

### 5. 分支管理

- ✅ 功能分支命名清晰：`feature/xxx`
- ✅ 及时合并到主分支
- ✅ 合并后删除功能分支
- ❌ 不要长期保留功能分支

## 🆘 故障排除

### 问题 1：测试失败

```bash
# 查看详细错误
mvn test -X

# 只运行失败的测试
mvn test -Dsurefire.rerunFailingTestsCount=2
```

### 问题 2：编译错误

```bash
# 清理并重新编译
mvn clean compile

# 更新依赖
mvn clean install -U
```

### 问题 3：Worktree 问题

```bash
# 查看所有 worktrees
git worktree list

# 清理无效 worktrees
git worktree prune

# 强制删除 worktree
rm -rf ../project-feature
git worktree prune
```

### 问题 4：Hook 不生效

```bash
# 检查 Hook 文件格式
cat .kiro/hooks/enforce-tdd.json | jq .

# 重启 Kiro（如需要）
```

## 📚 相关文档

- 完整指南：`.kiro/steering/superpowers-workflow-guide.md`
- TDD 流程：`.kiro/steering/tdd-workflow.md`
- 代码审查：`.kiro/steering/code-review-checklist.md`
- 任务拆分：`.kiro/steering/task-breakdown.md`
- Git Worktrees：`.kiro/steering/git-worktrees-workflow.md`
- QuickCEP 规范：`develop-agent.md`

## 🎓 学习路径

### 第 1 周：基础流程

1. 学习 TDD 循环（RED-GREEN-REFACTOR）
2. 练习任务拆分（2-5 分钟/任务）
3. 熟悉代码审查清单
4. 使用简化模式开发小功能

### 第 2 周：进阶技巧

1. 学习 Git Worktrees
2. 使用子代理批量执行任务
3. 自定义 Hooks
4. 优化工作流程

### 第 3 周：团队协作

1. 统一团队工作流
2. 制定代码审查标准
3. 建立任务模板库
4. 持续改进流程

---

**快速开始**：复制以下命令开始你的第一个功能

```bash
# 1. 创建分支
git checkout -b feature/my-first-feature

# 2. 让 Kiro 帮你
"请将'我的第一个功能'拆分为详细任务，并执行所有任务"

# 3. 完成后合并
git checkout develop
git merge feature/my-first-feature
git push origin develop
```

祝你开发愉快！🎉
