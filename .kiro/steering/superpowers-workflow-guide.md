---
inclusion: manual
---

# Superpowers 风格工作流完整指南

基于 obra/superpowers 的工程纪律框架，适配 QuickCEP 项目群的完整开发工作流。

## 工作流概览

```
需求分析 → 任务拆分 → TDD 开发 → 代码审查 → 测试验证 → 提交代码
   ↓          ↓          ↓          ↓          ↓          ↓
 Spec    tasks.md    RED-GREEN   Checklist   mvn test   git commit
```

## 7 阶段工作流

### 阶段 1：头脑风暴（Brainstorming）

**目标**：明确功能需求和技术方案

**操作**：
```
我想实现一个用户缓存功能，需求如下：
1. 查询用户时先查缓存，未命中再查数据库
2. 创建/更新用户时同步更新缓存
3. 删除用户时清除缓存
4. 使用 Redis 作为缓存，过期时间 1 小时
```

**Kiro 输出**：
- 技术方案建议
- 涉及的模块和类
- 潜在的技术风险
- 预估工作量

### 阶段 2：Git 分支管理（可选）

**选项 A：使用 Git Worktrees（推荐大型功能）**

```bash
# 创建功能分支和隔离工作目录
git worktree add ../quickcem-im-user-cache feature/user-cache
cd ../quickcem-im-user-cache
```

**选项 B：直接在当前分支（推荐小型功能）**

```bash
# 创建功能分支
git checkout -b feature/user-cache
```

**Kiro 命令**：
```
使用 worktree 模式创建 feature/user-cache 分支
```

### 阶段 3：编写计划（Writing Plans）

**目标**：将功能拆分为 2-5 分钟的小任务

**Kiro 命令**：
```
请将"用户缓存功能"拆分为详细的任务列表
```

**Kiro 输出**：`tasks.md`
```markdown
# 用户缓存功能任务列表

## 1. 缓存基础设施（10 分钟）

- [ ] 1.1 创建缓存 Key 常量类（2 分钟）
- [ ] 1.2 创建缓存配置类（3 分钟）
- [ ] 1.3 添加 Redis 依赖和配置（5 分钟）

## 2. 缓存读取逻辑（15 分钟）

- [ ] 2.1 编写缓存读取测试（RED）（3 分钟）
- [ ] 2.2 实现缓存读取逻辑（GREEN）（5 分钟）
- [ ] 2.3 优化缓存读取代码（REFACTOR）（2 分钟）
- [ ] 2.4 添加缓存未命中测试（5 分钟）

## 3. 缓存写入逻辑（10 分钟）

- [ ] 3.1 编写缓存写入测试（RED）（3 分钟）
- [ ] 3.2 实现缓存写入逻辑（GREEN）（4 分钟）
- [ ] 3.3 优化缓存写入代码（REFACTOR）（3 分钟）

## 4. 缓存失效逻辑（10 分钟）

- [ ] 4.1 编写缓存失效测试（RED）（3 分钟）
- [ ] 4.2 实现缓存失效逻辑（GREEN）（4 分钟）
- [ ] 4.3 优化缓存失效代码（REFACTOR）（3 分钟）
```

### 阶段 4：子代理驱动开发（Subagent-Driven Development）

**目标**：使用子代理并行执行任务

**Kiro 命令**：
```
执行所有任务（使用子代理）
```

**工作流程**：
1. Kiro 读取 `tasks.md`
2. 为每个任务创建独立子代理
3. 子代理按顺序执行任务
4. 每个任务完成后更新状态
5. 所有任务完成后汇总报告

**注意**：Kiro 目前只支持串行执行子代理，不支持并行。

### 阶段 5：测试驱动开发（Test-Driven Development）

**目标**：遵循 RED-GREEN-REFACTOR 循环

**每个任务的 TDD 流程**：

#### 5.1 RED（红灯）- 写失败测试

```java
@Test
public void testGetUserFromCache_ShouldReturnCachedUser() {
    // Given
    Long userId = 1L;
    UserVO expectedUser = UserVO.builder().id(userId).name("张三").build();
    
    // 模拟缓存中有数据
    redisTemplate.opsForValue().set("user:detail:1", JSON.toJSONString(expectedUser));
    
    // When
    UserVO actualUser = userService.getUserById(userId);
    
    // Then
    assertNotNull(actualUser);
    assertEquals(expectedUser.getId(), actualUser.getId());
    assertEquals(expectedUser.getName(), actualUser.getName());
}
```

**运行测试**：
```bash
mvn test -Dtest=UserServiceTest#testGetUserFromCache_ShouldReturnCachedUser
# 预期：测试失败（方法未实现缓存逻辑）
```

#### 5.2 GREEN（绿灯）- 写最小实现

```java
@Override
public UserVO getUserById(Long userId) {
    // 1. 先查缓存
    String cacheKey = String.format(CacheKeyConstant.USER_DETAIL, userId);
    String cachedUser = redisTemplate.opsForValue().get(cacheKey);
    
    if (StringUtils.isNotBlank(cachedUser)) {
        return JSON.parseObject(cachedUser, UserVO.class);
    }
    
    // 2. 缓存未命中，查数据库
    UserPO userPO = userMapper.selectById(userId);
    if (userPO == null) {
        return null;
    }
    
    // 3. 写入缓存
    UserVO userVO = BeanUtil.toBean(userPO, UserVO.class);
    redisTemplate.opsForValue().set(cacheKey, JSON.toJSONString(userVO), 1, TimeUnit.HOURS);
    
    return userVO;
}
```

**运行测试**：
```bash
mvn test -Dtest=UserServiceTest#testGetUserFromCache_ShouldReturnCachedUser
# 预期：测试通过
```

#### 5.3 REFACTOR（重构）- 优化代码

```java
@Override
public UserVO getUserById(Long userId) {
    return Optional.ofNullable(getCachedUser(userId))
            .orElseGet(() -> loadAndCacheUser(userId));
}

private UserVO getCachedUser(Long userId) {
    String cacheKey = String.format(CacheKeyConstant.USER_DETAIL, userId);
    String cachedUser = redisTemplate.opsForValue().get(cacheKey);
    return StringUtils.isNotBlank(cachedUser) 
        ? JSON.parseObject(cachedUser, UserVO.class) 
        : null;
}

private UserVO loadAndCacheUser(Long userId) {
    UserPO userPO = userMapper.selectById(userId);
    if (userPO == null) {
        return null;
    }
    
    UserVO userVO = BeanUtil.toBean(userPO, UserVO.class);
    cacheUser(userId, userVO);
    return userVO;
}

private void cacheUser(Long userId, UserVO userVO) {
    String cacheKey = String.format(CacheKeyConstant.USER_DETAIL, userId);
    redisTemplate.opsForValue().set(cacheKey, JSON.toJSONString(userVO), 1, TimeUnit.HOURS);
}
```

**再次运行测试**：
```bash
mvn test -Dtest=UserServiceTest
# 预期：所有测试通过
```

#### 5.4 COMMIT（提交）

```bash
git add .
git commit -m "feat: 实现用户缓存读取逻辑"
```

### 阶段 6：请求代码审查（Requesting Code Review）

**目标**：自动执行代码审查，确保代码质量

**触发方式**：

**方式 1：自动触发（通过 Hook）**
- 保存 Java 文件时自动触发
- Hook 配置：`.kiro/hooks/auto-code-review.json`

**方式 2：手动触发**
```
请审查刚才修改的代码
```

**审查内容**：

#### 6.1 规格合规性检查
- ✅ 功能完整性：是否实现了任务要求的所有功能？
- ✅ 文件路径正确：是否在正确的模块和包中？
- ✅ 接口一致性：方法签名是否与设计一致？
- ✅ 测试覆盖：是否有对应的单元测试？

#### 6.2 QuickCEP 规范合规
- ✅ 使用 `@Resource` 注入依赖
- ✅ 使用 `StringUtils.isBlank()` 判断字符串
- ✅ 使用 `JSON.toJSONString()` 处理 JSON
- ✅ 添加标准方法注释（@author、@param、@return）

#### 6.3 代码质量检查
- ✅ 命名清晰（类名、方法名、变量名）
- ✅ 方法长度合理（< 50 行）
- ✅ 避免深层嵌套（< 3 层）
- ✅ 异常处理完善
- ✅ 日志记录完整

#### 6.4 中间件使用规范
- ✅ Redis Key 命名：`user:detail:{id}`
- ✅ 缓存设置过期时间：1 小时
- ✅ 分布式锁正确释放（如使用）

**审查报告示例**：

```markdown
## 代码审查报告 - 任务 2.2

### ✅ 通过项
- 使用了 @Resource 注入 RedisTemplate
- 添加了完整的单元测试
- Redis Key 命名符合规范
- 缓存设置了过期时间

### 🔴 Critical 问题
无

### 🟡 Major 问题
1. getUserById 方法缺少方法注释
   - 位置：UserServiceImpl.java:45
   - 建议：添加 @author、@param、@return 注释

### 🟢 Minor 问题
1. 可以使用 Optional 简化逻辑
   - 位置：UserServiceImpl.java:50-55
   - 建议：使用 Optional.ofNullable() 链式调用

### 总结
- Critical: 0 个（✅ 可以继续）
- Major: 1 个（建议修复）
- Minor: 1 个（可选）
```

### 阶段 7：完成开发分支（Finishing a Development Branch）

**目标**：合并代码、清理分支、更新文档

#### 7.1 最终测试

```bash
# 运行所有测试
mvn clean test

# 运行集成测试
mvn verify

# 检查测试覆盖率
mvn jacoco:report
```

#### 7.2 合并代码

**如果使用 Worktrees**：

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

# 清理 worktree
git worktree remove ../quickcem-im-user-cache
git branch -d feature/user-cache
```

**如果直接在当前分支**：

```bash
# 切换回主分支
git checkout develop

# 合并功能分支
git merge feature/user-cache

# 推送到远程
git push origin develop

# 删除功能分支
git branch -d feature/user-cache
```

#### 7.3 更新文档

```bash
# 更新 CHANGELOG.md
echo "## [1.2.0] - 2024-01-29
### Added
- 用户缓存功能，提升查询性能 50%" >> CHANGELOG.md

# 更新 README.md（如需要）
```

#### 7.4 通知团队

```bash
# 发送通知（根据团队流程）
# - Slack/钉钉消息
# - 邮件通知
# - 代码审查请求
```

## 完整示例：用户缓存功能

### 1. 启动工作流

```
我想实现用户缓存功能，使用 Redis，过期时间 1 小时
```

### 2. Kiro 拆分任务

```
请将"用户缓存功能"拆分为详细任务
```

### 3. 创建 Worktree（可选）

```
使用 worktree 模式创建 feature/user-cache 分支
```

### 4. 执行所有任务

```
执行所有任务（使用子代理）
```

### 5. Kiro 自动执行

- 任务 1.1：创建 CacheKeyConstant.java
- 任务 1.2：创建 CacheConfig.java
- 任务 1.3：添加 Redis 配置
- 任务 2.1：编写缓存读取测试（RED）
- 任务 2.2：实现缓存读取逻辑（GREEN）
- 任务 2.3：优化代码（REFACTOR）
- 任务 2.4：添加缓存未命中测试
- ...（继续执行所有任务）

### 6. 自动代码审查

每个任务完成后，Kiro 自动执行代码审查，输出报告。

### 7. 最终验证

```bash
mvn clean test
# 所有测试通过
```

### 8. 合并代码

```
请帮我合并 feature/user-cache 分支到 develop
```

### 9. 清理

```
请清理 worktree 和功能分支
```

## 工作流配置

### Steering Files（指导文件）

| 文件 | 用途 | 加载方式 |
|-----|------|---------|
| `tdd-workflow.md` | TDD 流程规范 | 始终加载 |
| `code-review-checklist.md` | 代码审查清单 | 始终加载 |
| `task-breakdown.md` | 任务拆分规范 | 始终加载 |
| `git-worktrees-workflow.md` | Git Worktrees 指南 | 手动加载 |
| `superpowers-workflow-guide.md` | 完整工作流指南 | 手动加载 |

### Hooks（自动化钩子）

| Hook | 触发时机 | 作用 |
|------|---------|------|
| `enforce-tdd.json` | 保存 Java 文件 | 提醒遵循 TDD |
| `auto-code-review.json` | 保存 Java 文件 | 自动代码审查 |
| `run-tests-on-commit.json` | Agent 停止 | 提醒运行测试和提交 |

### 启用/禁用 Hooks

如果觉得 Hooks 太频繁，可以临时禁用：

```bash
# 重命名 Hook 文件（禁用）
mv .kiro/hooks/enforce-tdd.json .kiro/hooks/enforce-tdd.json.disabled

# 恢复 Hook（启用）
mv .kiro/hooks/enforce-tdd.json.disabled .kiro/hooks/enforce-tdd.json
```

## 常见问题

### Q1：必须使用 Git Worktrees 吗？

**A**：不是必须的。Worktrees 适合大型功能开发，小型修改可以直接在当前分支工作。

### Q2：Kiro 支持并行执行任务吗？

**A**：目前 Kiro 只支持串行执行子代理，不支持像 Claude Code 那样的并行执行。

### Q3：如何跳过某个任务？

**A**：在 `tasks.md` 中将任务标记为可选：
```markdown
- [ ]* 1.5 添加性能监控（可选）
```

### Q4：代码审查太严格怎么办？

**A**：可以调整 `code-review-checklist.md` 中的检查项，或者临时禁用 `auto-code-review.json` Hook。

### Q5：如何在 Supervised 模式下工作？

**A**：Kiro 的 Supervised 模式会自动执行操作，但提供撤销按钮。这样可以避免频繁的授权确认。

## 总结

Superpowers 风格工作流的核心价值：

1. **工程纪律**：强制 TDD、代码审查、任务拆分
2. **质量保证**：每个任务都有测试和审查
3. **可追溯性**：清晰的任务列表和提交历史
4. **团队协作**：统一的开发流程和代码风格

适用场景：

- ✅ 中大型功能开发
- ✅ 团队协作项目
- ✅ 需要高质量代码
- ✅ 需要详细文档

不适用场景：

- ❌ 紧急热修复
- ❌ 简单配置修改
- ❌ 原型验证
- ❌ 学习阶段

根据项目实际情况，灵活选择使用完整工作流或简化版本。
