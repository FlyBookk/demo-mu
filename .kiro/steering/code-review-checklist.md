---
inclusion: always
---

# 代码审查检查清单

每完成一个任务后，自动执行代码审查。

## 自动审查流程

### 阶段 1：规格合规性检查

检查代码是否符合任务规格：

- [ ] **功能完整性**：是否实现了任务要求的所有功能？
- [ ] **文件路径正确**：是否在正确的模块和包中？
- [ ] **接口一致性**：方法签名是否与设计一致？
- [ ] **测试覆盖**：是否有对应的单元测试？

### 阶段 2：代码质量检查

#### 2.1 QuickCEP 规范合规

**注解规范**：
- [ ] Service 类使用 `@Service` + `@Slf4j`
- [ ] Controller 类使用 `@RestController` + `@Slf4j` + `@CrossOrigin` + `@Api`
- [ ] 配置类使用 `@Configuration` + `@RefreshScope`
- [ ] DTO/VO 使用 `@Data` + `@Builder` + `@NoArgsConstructor` + `@AllArgsConstructor`

**依赖注入**：
- [ ] 优先使用 `@Resource` 而非 `@Autowired`
- [ ] 避免字段注入，考虑构造器注入（必需依赖）

**工具类使用**：
- [ ] 字符串判断使用 `StringUtils.isBlank()` / `isNotBlank()`
- [ ] 集合判断使用 `CollectionUtils.isEmpty()` / `isNotEmpty()`
- [ ] JSON 处理使用 `JSON.toJSONString()` / `parseObject()`

**响应封装**：
- [ ] API 返回使用 `ResponseResult.success()` / `failed()`
- [ ] 不直接返回裸对象

**方法注释**：
- [ ] 包含 `@author` 和时间
- [ ] 包含 `@param` 说明
- [ ] 包含 `@return` 说明

#### 2.2 代码质量

**命名规范**：
- [ ] 类名使用大驼峰（PascalCase）
- [ ] 方法名使用小驼峰（camelCase）
- [ ] 常量使用全大写下划线（UPPER_SNAKE_CASE）
- [ ] 包名使用小写

**代码结构**：
- [ ] 方法长度合理（建议 < 50 行）
- [ ] 避免深层嵌套（建议 < 3 层）
- [ ] 单一职责原则
- [ ] 避免重复代码

**异常处理**：
- [ ] 捕获具体异常，避免 `catch (Exception e)`
- [ ] 记录异常日志 `log.error("错误信息", e)`
- [ ] 不吞异常

**日志规范**：
- [ ] 关键操作记录日志
- [ ] 使用合适的日志级别（debug/info/warn/error）
- [ ] 敏感信息脱敏

#### 2.3 中间件使用

**Redis/Redisson**：
- [ ] Key 命名符合规范：`{module}:{type}:{id}`
- [ ] 分布式锁正确释放（try-finally）
- [ ] 缓存设置过期时间

**Kafka/QMQ**：
- [ ] 消息有异常处理和重试机制
- [ ] Topic 命名符合规范
- [ ] 消费者有日志记录

**Caffeine**：
- [ ] 设置最大缓存数量
- [ ] 设置过期策略

**MyBatis-Plus**：
- [ ] 使用 Lambda 查询而非 XML
- [ ] PO 类使用 `@TableName` 和 `@TableId`
- [ ] 逻辑删除使用 `@TableLogic`

#### 2.4 测试质量

- [ ] 测试方法命名清晰：`test{Method}_{Scenario}_Should{ExpectedResult}`
- [ ] 使用 Given-When-Then 结构
- [ ] 测试覆盖正常场景和异常场景
- [ ] 测试独立，不依赖执行顺序
- [ ] 测试数据清理

## 问题严重性分级

### 🔴 Critical（阻断）- 必须修复

- 未通过编译
- 测试失败
- 违反核心规范（如未使用 @Resource）
- 安全漏洞
- 数据丢失风险

### 🟡 Major（重要）- 强烈建议修复

- 缺少必要注释
- 代码重复
- 性能问题
- 缺少异常处理
- 日志不完整

### 🟢 Minor（次要）- 可选修复

- 命名不够清晰
- 代码格式问题
- 可优化的逻辑
- 注释可以更详细

## 审查报告格式

```markdown
## 代码审查报告 - 任务 X.X

### ✅ 通过项
- 使用了 @Resource 注入
- 添加了单元测试
- 响应使用 ResponseResult 封装

### 🔴 Critical 问题
无

### 🟡 Major 问题
1. UserService.getUserById 缺少异常处理
   - 位置：UserServiceImpl.java:45
   - 建议：添加 try-catch 并记录日志

### 🟢 Minor 问题
1. 方法注释可以更详细
   - 位置：UserServiceImpl.java:42
   - 建议：补充参数说明

### 总结
- Critical: 0 个（✅ 可以继续）
- Major: 1 个（建议修复）
- Minor: 1 个（可选）
```

## 审查触发时机

1. **每个任务完成后**：自动触发
2. **提交代码前**：手动触发
3. **合并分支前**：完整审查

## 使用方式

Kiro 会在任务完成后自动执行审查，你也可以手动触发：

```
请审查刚才修改的代码
```
