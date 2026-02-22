---
inclusion: always
---

# TDD 工作流规范

当实现新功能或修改现有代码时，遵循测试驱动开发（TDD）流程。

## RED-GREEN-REFACTOR 循环

### 1. RED（红灯）- 写失败测试
```java
@Test
public void testGetUserById_ShouldReturnUser() {
    // Given
    Long userId = 1L;
    
    // When
    UserVO user = userService.getUserById(userId);
    
    // Then
    assertNotNull(user);
    assertEquals(userId, user.getId());
}
```

**运行测试，确认失败**：
```bash
mvn test -Dtest=UserServiceTest#testGetUserById_ShouldReturnUser
# 预期：测试失败（方法未实现或逻辑错误）
```

### 2. GREEN（绿灯）- 写最小实现

```java
@Service
@Slf4j
public class UserServiceImpl implements IUserService {
    
    @Resource
    private UserMapper userMapper;
    
    @Override
    public UserVO getUserById(Long userId) {
        UserPO userPO = userMapper.selectById(userId);
        if (userPO == null) {
            return null;
        }
        return BeanUtil.toBean(userPO, UserVO.class);
    }
}
```

**运行测试，确认通过**：
```bash
mvn test -Dtest=UserServiceTest#testGetUserById_ShouldReturnUser
# 预期：测试通过
```

### 3. REFACTOR（重构）- 优化代码

```java
@Override
public UserVO getUserById(Long userId) {
    return Optional.ofNullable(userMapper.selectById(userId))
            .map(po -> BeanUtil.toBean(po, UserVO.class))
            .orElse(null);
}
```

**再次运行测试，确保仍然通过**。

### 4. COMMIT（提交）

```bash
git add .
git commit -m "feat: 实现 getUserById 方法"
```

## TDD 原则

1. **测试先行**：永远先写测试，再写实现
2. **最小实现**：只写让测试通过的最少代码
3. **小步前进**：每次只实现一个小功能
4. **频繁提交**：每个 RED-GREEN-REFACTOR 循环后提交

## 测试覆盖要求

- ✅ 正常场景（Happy Path）
- ✅ 边界条件（null、空集合、边界值）
- ✅ 异常场景（数据不存在、参数错误）

## 禁止行为

❌ **先写实现代码，后补测试**
❌ **跳过测试失败阶段**
❌ **一次实现多个功能**
❌ **提交未测试的代码**

## 示例：完整 TDD 流程

```
任务：实现用户缓存功能

1. RED: 写测试 testGetUserFromCache_ShouldReturnCachedUser
   运行 → 失败（方法不存在）

2. GREEN: 实现 getUserFromCache 方法
   运行 → 通过

3. REFACTOR: 优化缓存 Key 命名
   运行 → 仍然通过

4. COMMIT: git commit -m "feat: 添加用户缓存"

5. RED: 写测试 testGetUserFromCache_WhenCacheMiss_ShouldLoadFromDB
   运行 → 失败（缓存未命中逻辑未实现）

6. GREEN: 实现缓存未命中时从数据库加载
   运行 → 通过

7. COMMIT: git commit -m "feat: 缓存未命中时从数据库加载"
```

## 与慕声税务系统规范集成

TDD 过程中仍需遵守：
- 使用 `@Resource` 或 `@Autowired` 注入依赖
- 使用 `StringUtils`/`CollectionUtils` 判断
- 使用 `Result` 封装响应
- 添加标准方法注释（包含 @author 和时间）
- 使用中文注释
