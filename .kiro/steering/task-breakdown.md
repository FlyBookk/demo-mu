---
inclusion: always
---

# 任务拆分规范

将大型功能拆分为可管理的小任务，每个任务 2-5 分钟完成。

## 拆分原则

### 1. 时间限制
- **目标**：每个任务 2-5 分钟
- **最长**：不超过 10 分钟
- **最短**：不少于 1 分钟

### 2. 独立性
- 每个任务可以独立完成
- 不依赖未完成的其他任务（除非明确标注依赖）
- 可以独立测试和验证

### 3. 可验证性
- 每个任务有明确的完成标准
- 有具体的验证步骤
- 可以通过测试确认完成

### 4. 原子性
- 一个任务只做一件事
- 避免"顺便"做其他事情
- 遵循单一职责原则

## 任务模板

```markdown
### 任务 X.X：{任务名称}

**目标**：{一句话描述任务目标}

**文件**：
- 创建：`path/to/NewFile.java`
- 修改：`path/to/ExistingFile.java`

**实现步骤**：
1. {具体步骤 1}
2. {具体步骤 2}
3. {具体步骤 3}

**验证**：
- [ ] 编译通过
- [ ] 测试通过：`mvn test -Dtest=XxxTest`
- [ ] 符合规范检查

**依赖**：
- 依赖任务 X.Y（如有）

**预计时间**：3 分钟
```

## 拆分示例

### ❌ 错误拆分（任务过大）

```markdown
- [ ] 1. 实现用户管理功能
```

**问题**：
- 太宽泛，无法估算时间
- 包含多个子功能
- 难以验证完成

### ✅ 正确拆分

```markdown
- [ ] 1.1 创建 UserDTO 和 UserVO 类（2 分钟）
- [ ] 1.2 创建 UserMapper 接口（2 分钟）
- [ ] 1.3 实现 UserService.getUserById 方法（3 分钟）
- [ ] 1.4 添加 getUserById 单元测试（3 分钟）
- [ ] 1.5 实现 UserController.getUser 接口（3 分钟）
- [ ] 1.6 添加 Swagger 注解（2 分钟）
```

## 常见任务类型及拆分

### 类型 1：创建新实体

```markdown
- [ ] X.1 创建 PO 类（数据库实体）
  - 文件：`domain/repository/po/UserPO.java`
  - 包含：字段、注解、getter/setter
  - 验证：编译通过

- [ ] X.2 创建 DTO 类（数据传输对象）
  - 文件：`domain/dto/UserDTO.java`
  - 包含：字段、Builder 注解
  - 验证：编译通过

- [ ] X.3 创建 VO 类（视图对象）
  - 文件：`domain/vo/UserVO.java`
  - 包含：字段、Swagger 注解
  - 验证：编译通过
```

### 类型 2：实现 CRUD 服务

```markdown
- [ ] X.1 创建 Mapper 接口
  - 文件：`domain/repository/mapper/UserMapper.java`
  - 继承：BaseMapper<UserPO>
  - 验证：编译通过

- [ ] X.2 实现 Service.create 方法
  - 文件：`service/impl/UserServiceImpl.java`
  - TDD：先写测试，再实现
  - 验证：测试通过

- [ ] X.3 实现 Service.getById 方法
  - TDD：先写测试，再实现
  - 验证：测试通过

- [ ] X.4 实现 Service.update 方法
  - TDD：先写测试，再实现
  - 验证：测试通过

- [ ] X.5 实现 Service.delete 方法
  - TDD：先写测试，再实现
  - 验证：测试通过
```

### 类型 3：实现 RESTful API

```markdown
- [ ] X.1 创建 Controller 类框架
  - 文件：`controller/UserApi.java`
  - 包含：类注解、依赖注入
  - 验证：编译通过

- [ ] X.2 实现 GET /users/{id} 接口
  - 方法：getUser(Long id)
  - 包含：Swagger 注解、ResponseResult 封装
  - 验证：编译通过

- [ ] X.3 实现 POST /users 接口
  - 方法：createUser(UserDTO dto)
  - 包含：@Valid 校验
  - 验证：编译通过

- [ ] X.4 实现 PUT /users/{id} 接口
- [ ] X.5 实现 DELETE /users/{id} 接口
```

### 类型 4：添加缓存

```markdown
- [ ] X.1 创建缓存 Key 常量类
  - 文件：`constant/CacheKeyConstant.java`
  - 定义：USER_CACHE_KEY = "user:detail:{id}"
  - 验证：编译通过

- [ ] X.2 实现缓存读取逻辑
  - 位置：UserService.getUserById
  - 先查缓存，未命中再查数据库
  - 验证：单元测试通过

- [ ] X.3 实现缓存写入逻辑
  - 位置：UserService.createUser
  - 创建后写入缓存
  - 验证：单元测试通过

- [ ] X.4 实现缓存失效逻辑
  - 位置：UserService.updateUser/deleteUser
  - 更新/删除后清除缓存
  - 验证：单元测试通过
```

### 类型 5：重构现有代码

```markdown
- [ ] X.1 添加测试覆盖（重构前）
  - 为现有代码添加测试
  - 确保测试通过
  - 验证：测试覆盖率 > 80%

- [ ] X.2 提取公共方法
  - 识别重复代码
  - 提取为独立方法
  - 验证：测试仍然通过

- [ ] X.3 优化命名
  - 重命名不清晰的变量/方法
  - 验证：测试仍然通过

- [ ] X.4 简化逻辑
  - 减少嵌套层级
  - 使用 Optional/Stream
  - 验证：测试仍然通过
```

## 任务依赖管理

### 标注依赖

```markdown
- [ ] 1.1 创建 UserPO 类
- [ ] 1.2 创建 UserMapper 接口（依赖 1.1）
- [ ] 1.3 实现 UserService（依赖 1.2）
- [ ] 1.4 添加单元测试（依赖 1.3）
```

### 并行任务

```markdown
# 这些任务可以并行（无依赖）
- [ ] 2.1 创建 UserDTO 类
- [ ] 2.2 创建 OrderDTO 类
- [ ] 2.3 创建 ProductDTO 类
```

## 任务完成标准

每个任务完成后必须满足：

1. ✅ **编译通过**：`mvn compile`
2. ✅ **测试通过**：相关单元测试全部通过
3. ✅ **规范检查**：通过代码审查清单
4. ✅ **提交代码**：`git commit -m "feat: 任务描述"`

## 使用 Kiro 执行任务

### 自动拆分

```
请将"实现用户管理功能"拆分为 2-5 分钟的小任务
```

### 执行任务

```
执行任务 1.1
```

### 批量执行

```
执行所有任务（使用子代理）
```

Kiro 会自动：
1. 读取 tasks.md
2. 按顺序执行每个任务
3. 每个任务派发独立子代理
4. 执行代码审查
5. 更新任务状态
