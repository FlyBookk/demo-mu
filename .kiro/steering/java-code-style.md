---
inclusion: fileMatch
fileMatchPattern: "**/*.java"
---

# Java 代码风格规范

当编写或修改 Java 代码时，遵循以下规范。

## 类注解规范

### 启动类
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

### Controller 类
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

### Service 实现类
```java
@Service
@Slf4j
public class {Service}Impl implements {Service} {
    @Autowired  // 或 @Resource
    private {Mapper} mapper;
    // ...
}
```

### 配置类
```java
@Configuration
public class {Config}Configuration {
    // 配置 Bean
}
```

## 实体类注解

### DTO/VO 类
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
public class {Entity} {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    
    @TableLogic
    private Boolean deleted;
}
```

### 枚举类
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

## 依赖注入

- 优先使用 `@Autowired` 或 `@Resource`
- 构造器注入用于必需依赖（推荐）
- 字段注入用于可选依赖

## 方法注释规范

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

## API 响应规范

使用 `Result` 封装响应：
```java
return Result.success(data);
return Result.success();
return Result.error(ErrorCode.PARAM_ERROR);
```

## 工具类使用

### 字符串判断
```java
import org.springframework.util.StringUtils;
if (StringUtils.hasText(str)) { }
if (!StringUtils.hasText(str)) { }
```

### 集合判断
```java
import org.springframework.util.CollectionUtils;
if (CollectionUtils.isEmpty(list)) { }
if (!CollectionUtils.isEmpty(list)) { }
```

### Hutool 工具
```java
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.bean.BeanUtil;
String msg = StrUtil.format("用户{}登录成功", username);
UserVO vo = BeanUtil.toBean(entity, UserVO.class);
```
