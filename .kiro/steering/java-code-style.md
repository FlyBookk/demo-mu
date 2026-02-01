---
inclusion: fileMatch
fileMatchPattern: "**/*.java"
---

# Java 代码风格规范

当编写或修改 Java 代码时，遵循以下规范。

## 类注解规范

### 启动类
```java
@Slf4j
@EnableMBeanExport(registration = RegistrationPolicy.IGNORE_EXISTING)
@EnableScheduling
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.quickcem")
@ComponentScan({"com.quickcem", "com.quickshop", "com.quickcep"})
@MapperScan("com.quickcem.{module}.domain.repository.mapper")
@EnableTracing
public class {Module}Application {
    public static void main(String[] args) {
        SpringApplication.run({Module}Application.class, args);
        log.info(" ========= QuickCEM {Module} Service Start =========");
    }
}
```

### Controller 类
```java
@Slf4j
@CrossOrigin
@RestController
@RequestMapping("/{resource}")
@Api(tags = "{资源}API")
public class {Resource}Api {
    // ...
}
```

### Service 实现类
```java
@Service
@Slf4j
public class {Service}Impl implements I{Service} {
    @Resource
    private {Repository} repository;
    // ...
}
```

### 配置类
```java
@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "{config-prefix}")
public class {Config}Properties {
    // ...
}
```

## 实体类注解

### DTO/VO/BO
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class {Entity}DTO {
    // 字段定义
}
```

### PO（数据库实体）
```java
@Data
@TableName("{table_name}")
@Accessors(chain = true)
public class {Entity}PO {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer isDeleted;
}
```

### 枚举类
```java
@Getter
@AllArgsConstructor
public enum {Name}Enum {
    TYPE_A("a", "描述A"),
    TYPE_B("b", "描述B");
    
    private final String type;
    private final String desc;
}
```

## 依赖注入

- **优先使用** `@Resource` 进行依赖注入
- 构造器注入用于必需依赖
- `@Autowired` 作为备选方案

## 方法注释规范

```java
/**
 * 方法功能描述
 *
 * @author {author}
 * {HH:mm yyyy年MM月dd日}
 * @param paramName 参数说明
 * @return {@link ReturnType} 返回值说明
 */
```

## API 响应规范

使用 `ResponseResult` 封装响应：
```java
return ResponseResult.success(data);
return ResponseResult.querySuccess(data);
return ResponseResult.failed("error.message.key");
```

## 工具类使用

### 字符串判断
```java
import org.apache.commons.lang3.StringUtils;
if (StringUtils.isBlank(str)) { }
if (StringUtils.isNotBlank(str)) { }
```

### 集合判断
```java
import org.apache.commons.collections4.CollectionUtils;
if (CollectionUtils.isEmpty(list)) { }
if (CollectionUtils.isNotEmpty(list)) { }
```

### JSON 处理
```java
import com.alibaba.fastjson.JSON;
String jsonStr = JSON.toJSONString(object);
UserDTO user = JSON.parseObject(jsonStr, UserDTO.class);
```
