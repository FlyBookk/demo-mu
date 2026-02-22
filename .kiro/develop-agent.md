# 慕声税务系统研发规范 (Musheng Tax System Development Standards)

本规范用于指导 Kiro 在慕声亚马逊转口贸易报税系统中进行代码生成、修改和审查时保持风格一致性。

## 项目概述

**项目名称**: 慕声亚马逊转口贸易报税管理系统  
**技术栈**: Spring Boot 3.2.2 + Java 17 + Vue 3 + TypeScript  
**GroupId**: `com.musheng`  
**包结构**: 标准 Spring Boot 多模块架构

## 核心原则

1. **模块化架构** - 使用 Maven 多模块管理，清晰的职责划分
2. **统一代码风格** - 使用项目约定的注解、命名和注释规范
3. **中文注释** - 所有代码注释、文档、日志消息使用中文
4. **现代化技术栈** - 使用 Spring Boot 3.x、Java 17、Sa-Token 等现代框架

---

## 一、项目架构规范

### 1.1 Maven 模块结构

```
musheng-tax-system/         # 后端项目根目录
├── musheng-common/         # 公共模块（工具类、异常、结果封装）
├── musheng-system/         # 系统模块（用户、角色、权限、日志）
├── musheng-config/         # 配置模块（Swagger、CORS、Sa-Token）
├── musheng-business/       # 业务模块（销售、配送、FBA、广告数据）
└── musheng-web/            # Web 模块（启动类、Controller）

musheng-tax-web/            # 前端项目（Vue 3 + TypeScript）
```

### 1.2 包命名规范

| 层级 | 包名 | 说明 |
|-----|------|------|
| 控制器 | `controller` | REST API 控制器 |
| 服务层 | `service` / `service.impl` | 业务逻辑层 |
| 实体 | `entity` | 数据库实体（PO） |
| DTO | `dto` | 数据传输对象 |
| VO | `vo` | 视图对象 |
| 数据访问 | `mapper` | MyBatis-Plus Mapper |
| 枚举 | `enums` | 枚举类 |
| 配置 | `config` | 配置类 |
| 工具 | `utils` | 工具类 |
| 异常 | `exception` | 自定义异常 |
| 结果 | `result` | 统一响应结果 |
| 上下文 | `context` | 上下文管理（如店铺上下文） |

### 1.3 GroupId 规范

- 项目 GroupId: `com.musheng`
- 所有模块统一使用此 GroupId

---

## 二、技术栈版本

### 2.1 后端技术栈

| 组件 | 版本 | 说明 |
|-----|------|------|
| JDK | 17 | Java 17 LTS |
| Spring Boot | 3.2.2 | Spring Boot 3.x |
| MyBatis-Plus | 3.5.5 | ORM 框架 |
| Sa-Token | 1.37.0 | 认证授权框架 |
| MySQL | 8.0.33 | 数据库 |
| Hutool | 5.8.25 | Java 工具集 |
| EasyExcel | 3.3.3 | Excel 处理 |
| Knife4j | 4.3.0 | API 文档（OpenAPI 3） |
| Lombok | 1.18.30 | 代码简化 |
| Caffeine | 3.1.8 | 本地缓存 |

### 2.2 前端技术栈

| 组件 | 版本 | 说明 |
|-----|------|------|
| Vue | 3.4.21 | 前端框架 |
| TypeScript | 5.4.3 | 类型系统 |
| Ant Design Vue | 4.1.2 | UI 组件库 |
| Vite | 5.2.0 | 构建工具 |
| Axios | 1.6.8 | HTTP 客户端 |
| Pinia | 2.1.7 | 状态管理 |
| Vue Router | 4.3.0 | 路由管理 |
| ECharts | 5.5.0 | 图表库 |
| VXE Table | 4.6.8 | 表格组件 |

---

## 三、代码风格规范

### 3.1 类注解规范

#### 启动类
```java
@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableTransactionManagement
@MapperScan("com.musheng.**.mapper")
public class MushengApplication {
    public static void main(String[] args) {
        SpringApplication.run(MushengApplication.class, args);
        System.out.println("============================================");
        System.out.println("  慕声税务系统启动成功!                      ");
        System.out.println("============================================");
    }
}
```

#### Controller 类
```java
@RestController
@RequestMapping("/v1/{resource}")
@Tag(name = "{资源}管理")
@Slf4j
@CrossOrigin
public class {Resource}Controller {
    @Resource
    private {Service} service;
    // ...
}
```

#### Service 实现类
```java
@Service
@Slf4j
public class {Service}Impl implements {Service} {
    @Autowired  // 或 @Resource
    private {Mapper} mapper;
    // ...
}
```

#### 配置类
```java
@Configuration
public class {Config}Configuration {
    // 配置 Bean
}
```

### 3.2 实体类注解规范

#### DTO/VO 类
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class {Entity}DTO {
    // 字段定义
}
```

#### PO 类（数据库实体）
```java
@Data
@TableName("{table_name}")
public class {Entity} {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    
    @TableLogic
    private Boolean deleted;
}
```

#### 枚举类
```java
@Getter
@AllArgsConstructor
public enum {Name}Enum {
    TYPE_A("A", "类型A"),
    TYPE_B("B", "类型B");
    
    private final String code;
    private final String desc;
}
```

### 3.3 方法注释规范

```java
/**
 * 方法功能描述
 * 
 * @param paramName 参数说明
 * @return 返回值说明
 * @author wanhua
 * 10:30 2026年01月29日
 */
public ReturnType methodName(ParamType paramName) {
    // ...
}
```

### 3.4 依赖注入规范

- 优先使用 `@Autowired` 或 `@Resource`
- 构造器注入用于必需依赖（推荐）
- 字段注入用于可选依赖

```java
@Service
public class ExampleService {
    // 字段注入
    @Autowired
    private UserMapper userMapper;
    
    // 或使用 @Resource
    @Resource
    private RoleMapper roleMapper;
}
```

---

## 四、API 设计规范

### 4.1 RESTful 接口规范

```java
@RestController
@RequestMapping("/v1/{resource}")
@Tag(name = "{资源}管理")
@Slf4j
@CrossOrigin
public class {Resource}Controller {

    @Resource
    private {Service} service;

    @Operation(summary = "查询列表")
    @GetMapping
    public Result<PageResult<{Entity}>> list(
            @RequestParam(required = false, defaultValue = "1") Integer current,
            @RequestParam(required = false, defaultValue = "10") Integer size) {
        // ...
        return Result.success(data);
    }

    @Operation(summary = "查询详情")
    @GetMapping("/{id}")
    public Result<{Entity}> getById(@PathVariable Long id) {
        // ...
        return Result.success(data);
    }

    @Operation(summary = "创建")
    @PostMapping
    public Result<{Entity}> create(@RequestBody @Valid {Entity}DTO dto) {
        // ...
        return Result.success(data);
    }

    @Operation(summary = "更新")
    @PutMapping("/{id}")
    public Result<{Entity}> update(
            @PathVariable Long id,
            @RequestBody @Valid {Entity}DTO dto) {
        // ...
        return Result.success(data);
    }

    @Operation(summary = "删除")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        // ...
        return Result.success();
    }
}
```

### 4.2 统一响应格式

使用 `com.musheng.common.result.Result` 封装响应：

```java
// 成功响应
return Result.success(data);
return Result.success();
return Result.success("操作成功", data);

// 失败响应
return Result.error(ErrorCode.PARAM_ERROR);
return Result.error(ErrorCode.PARAM_ERROR, "自定义错误消息");
return Result.error(500, "错误消息");
```

**Result 结构**：
```java
{
    "code": 0,           // 0=成功, 其他=失败
    "message": "success",
    "data": {...},
    "timestamp": 1705660800000,
    "requestId": "uuid"
}
```

### 4.3 权限校验

使用 Sa-Token 进行权限校验：

```java
// 角色校验
@SaCheckRole("admin")
@DeleteMapping("/{id}")
public Result<Void> delete(@PathVariable Long id) {
    // 仅 admin 角色可访问
}

// 权限校验
@SaCheckPermission("user:delete")
@DeleteMapping("/{id}")
public Result<Void> delete(@PathVariable Long id) {
    // 需要 user:delete 权限
}

// 登录校验（默认所有接口都需要登录）
@SaCheckLogin
@GetMapping("/profile")
public Result<UserVO> getProfile() {
    // 需要登录
}
```

---

## 五、Maven 配置规范

### 5.1 父 POM 配置

```xml
<properties>
    <java.version>17</java.version>
    <maven.compiler.source>17</maven.compiler.source>
    <maven.compiler.target>17</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>

    <!-- Spring Boot -->
    <spring-boot.version>3.2.2</spring-boot.version>

    <!-- MyBatis-Plus -->
    <mybatis-plus.version>3.5.5</mybatis-plus.version>

    <!-- Sa-Token -->
    <sa-token.version>1.37.0</sa-token.version>

    <!-- Database -->
    <mysql.version>8.0.33</mysql.version>

    <!-- Tools -->
    <hutool.version>5.8.25</hutool.version>
    <easyexcel.version>3.3.3</easyexcel.version>
    <knife4j.version>4.3.0</knife4j.version>
    <lombok.version>1.18.30</lombok.version>
</properties>
```

### 5.2 依赖管理

```xml
<dependencyManagement>
    <dependencies>
        <!-- Spring Boot Dependencies -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>${spring-boot.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>

        <!-- MyBatis-Plus -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
            <version>${mybatis-plus.version}</version>
        </dependency>

        <!-- Sa-Token -->
        <dependency>
            <groupId>cn.dev33</groupId>
            <artifactId>sa-token-spring-boot3-starter</artifactId>
            <version>${sa-token.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### 5.3 编译配置

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.11.0</version>
            <configuration>
                <source>${java.version}</source>
                <target>${java.version}</target>
                <encoding>${project.build.sourceEncoding}</encoding>
                <parameters>true</parameters>
            </configuration>
        </plugin>
    </plugins>
</build>
```

---

## 六、数据库规范

### 6.1 表命名

- 使用小写字母和下划线
- 业务模块前缀：`sales_`, `shipping_`, `fba_`, `advertising_`
- 示例：`sales_data`, `shipping_data`, `fba_shipment`

### 6.2 字段命名

- 使用小写字母和下划线
- 主键：`id`
- 外键：`{table}_id`
- 时间字段：`create_time`, `update_time`
- 状态字段：`status`, `deleted`
- 店铺ID：`shop_id`（用于数据隔离）

### 6.3 MyBatis-Plus 配置

```java
@Data
@TableName("sales_data")
public class SalesData {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long shopId;  // 店铺ID（数据隔离）
    
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    
    @TableLogic
    private Boolean deleted;
}
```

### 6.4 逻辑删除

```java
// 实体类配置
@TableLogic
private Boolean deleted;  // 0=未删除, 1=已删除

// 删除操作（自动变为逻辑删除）
mapper.deleteById(id);

// 物理删除（需要自定义方法）
@Delete("DELETE FROM table_name WHERE id = #{id}")
int physicalDeleteById(@Param("id") Long id);
```

---

## 七、业务规范

### 7.1 店铺数据隔离

所有业务数据必须关联店铺ID，实现多租户数据隔离：

```java
// 使用 ShopContext 获取当前店铺ID
Long shopId = ShopContext.requireShopId();

// 查询时自动过滤店铺
LambdaQueryWrapper<SalesData> wrapper = new LambdaQueryWrapper<>();
wrapper.eq(SalesData::getShopId, shopId);
```

### 7.2 用户信息获取

使用 Sa-Token 获取当前登录用户信息：

```java
// 判断是否登录
if (StpUtil.isLogin()) {
    // 获取用户ID
    Long userId = StpUtil.getLoginIdAsLong();
    String userIdStr = StpUtil.getLoginIdAsString();
    
    // 获取角色
    List<String> roles = StpUtil.getRoleList();
    
    // 判断角色
    boolean isAdmin = StpUtil.hasRole("admin");
}
```

### 7.3 异常处理

```java
// 使用自定义业务异常
throw new BusinessException(ErrorCode.DATA_NOT_EXIST, "数据不存在");

// 参数校验异常
throw new BusinessException(ErrorCode.PARAM_ERROR, "参数错误");

// 系统异常
throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统错误");
```

### 7.4 事务管理

```java
@Service
public class ExampleService {
    
    @Transactional(rollbackFor = Exception.class)
    public void doSomething() {
        // 事务操作
        // 任何异常都会回滚
    }
}
```

---

## 八、前端规范

### 8.1 API 调用

```typescript
// 使用封装的 request 工具
import { request } from '@/utils/request'

// GET 请求
export function getList(params: QueryParams) {
  return request.get<PageResult<DataType>>('/api/v1/resource', params)
}

// POST 请求
export function create(data: CreateDTO) {
  return request.post<DataType>('/api/v1/resource', data)
}

// DELETE 请求（带请求体）
export function batchDelete(ids: number[]) {
  return request.delete<void>('/api/v1/resource/batch', { data: ids })
}
```

### 8.2 响应处理

```typescript
// 响应结构
interface ApiResponse<T> {
  code: number      // 0=成功
  message: string
  data: T
  timestamp: number
}

// 使用示例
const response = await getList(params)
if (response.code === 0) {
  // 成功处理
  const data = response.data
}
```

### 8.3 状态管理

```typescript
// 使用 Pinia
import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', {
  state: () => ({
    userInfo: null,
    token: ''
  }),
  actions: {
    setUserInfo(info: UserInfo) {
      this.userInfo = info
    }
  }
})
```

---

## 九、日志规范

### 9.1 日志级别

- `DEBUG`: 调试信息
- `INFO`: 一般信息（业务流程关键节点）
- `WARN`: 警告信息（可恢复的异常）
- `ERROR`: 错误信息（需要关注的异常）

### 9.2 日志格式

```java
@Slf4j
public class ExampleService {
    
    public void process(Long id) {
        log.info("开始处理数据: id={}", id);
        
        try {
            // 业务逻辑
            log.debug("处理详情: data={}", data);
            
        } catch (BusinessException e) {
            log.warn("业务异常: id={}, reason={}", id, e.getMessage());
            throw e;
            
        } catch (Exception e) {
            log.error("系统异常: id={}", id, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "处理失败");
        }
        
        log.info("处理完成: id={}", id);
    }
}
```

---

## 十、工具类使用规范

### 10.1 字符串判断

```java
import org.springframework.util.StringUtils;

// 推荐写法
if (StringUtils.hasText(str)) {
    // 非空且非纯空白
}

if (!StringUtils.hasText(str)) {
    // null、空字符串或纯空白
}
```

### 10.2 集合判断

```java
import org.springframework.util.CollectionUtils;

if (CollectionUtils.isEmpty(list)) {
    // null 或空集合
}

if (!CollectionUtils.isEmpty(list)) {
    // 非空集合
}
```

### 10.3 Hutool 工具

```java
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.bean.BeanUtil;

// 字符串格式化
String msg = StrUtil.format("用户{}登录成功", username);

// Bean 复制
UserVO vo = BeanUtil.toBean(entity, UserVO.class);

// 集合操作
List<Long> ids = CollUtil.map(entities, Entity::getId, true);
```

---

## 十一、缓存规范

### 11.1 Caffeine 本地缓存

```java
import com.github.ben-manes.caffeine.cache.Cache;
import com.github.ben-manes.caffeine.cache.Caffeine;

@Component
public class LocalCache {
    
    private static final Cache<String, Object> CACHE = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(10))
            .build();
    
    public Object get(String key) {
        return CACHE.get(key, k -> loadFromDatabase(k));
    }
    
    public void invalidate(String key) {
        CACHE.invalidate(key);
    }
}
```

---

## 十二、测试规范

### 12.1 单元测试

```java
@SpringBootTest
class ExampleServiceTest {
    
    @Autowired
    private ExampleService service;
    
    @Test
    void testGetById_ShouldReturnData() {
        // Given
        Long id = 1L;
        
        // When
        DataVO result = service.getById(id);
        
        // Then
        assertNotNull(result);
        assertEquals(id, result.getId());
    }
}
```

---

## 附录：版本对照表

| 组件 | 版本 | 说明 |
|-----|------|------|
| JDK | 17 | Java 17 LTS |
| Spring Boot | 3.2.2 | Spring Boot 3.x |
| MyBatis-Plus | 3.5.5 | ORM 框架 |
| Sa-Token | 1.37.0 | 认证授权 |
| MySQL | 8.0.33 | 数据库 |
| Hutool | 5.8.25 | 工具集 |
| EasyExcel | 3.3.3 | Excel 处理 |
| Knife4j | 4.3.0 | API 文档 |
| Lombok | 1.18.30 | 代码简化 |
| Caffeine | 3.1.8 | 本地缓存 |

---

**文档版本**: 2.0.0  
**更新日期**: 2026-01-29  
**维护团队**: 慕声研发团队
