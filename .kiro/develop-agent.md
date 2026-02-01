# QuickCEP 研发规范指南 (Develop Agent)

本文档定义了 QuickCEP 项目群的研发规范，用于指导 AI Agent 和开发人员在项目调整时保持风格一致性。

---

## 一、项目架构规范

### 1.1 模块分层结构

**标准 DDD 分层模式**（适用于复杂业务服务）：
```
{project-name}/
├── {project-name}-client/           # 对外API接口定义（Feign Client）
├── {project-name}-interfaces/       # 接口层（Controller）
├── {project-name}-application/      # 应用层（Service编排）
├── {project-name}-domain/           # 领域层（核心业务逻辑）
├── {project-name}-infrastructure/   # 基础设施层（数据访问、外部服务）
└── {project-name}-start/            # 启动模块
```

**简化分层模式**（适用于轻量级服务）：
```
{project-name}/
├── {project-name}-core/             # 核心业务实现
├── {project-name}-domain/           # 领域模型
└── {project-name}-starter/          # 启动模块
```

### 1.2 包命名规范

| 层级 | 包名模式 | 示例 |
|-----|---------|------|
| 控制器 | `controller` / `facade` | `com.quickcem.im.core.controller` |
| 服务层 | `service` / `service.impl` | `com.quickcem.im.core.service.impl` |
| 领域实体 | `entity` / `domain.entity` | `com.quickcem.im.domain.entity` |
| 数据传输 | `dto` / `vo` / `bo` | `com.quickcem.im.domain.dto` |
| 数据访问 | `dao` / `repository` / `mapper` | `com.quickcem.im.domain.repository.mapper` |
| 枚举类 | `enums` / `enumertion` | `com.quickcem.im.domain.enumertion` |
| 常量类 | `constant` / `constants` | `com.quickcem.im.core.constants` |
| 工具类 | `utils` / `util` | `com.quickcem.im.core.utils` |
| 配置类 | `config` | `com.quickcem.im.core.config` |
| 工厂类 | `factory` | `com.quickcem.im.core.factory` |
| 事件类 | `event` | `com.quickcem.im.core.event` |
| 异常类 | `exception` | `com.quickcem.im.core.exception` |

### 1.3 GroupId 规范

| 项目类型 | GroupId | 示例 |
|---------|---------|------|
| 框架组件 | `com.quickshop.framework` | swagger-spring-boot-starter |
| QuickCEM 业务 | `com.quickcem` | quickcem-im, quickcem-cdp-analysis |
| 消息平台 | `com.quickcep.message` | quick-message-spring-boot-starter |
| 工单系统(旧) | `com.7moor.apass` / `com.m7` | apaas-business |

---

## 二、代码风格规范

### 2.1 类注解规范

**启动类**：
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

**Controller 类**：
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

**Service 实现类**：
```java
@Service
@Slf4j
public class {Service}Impl implements I{Service} {
    @Resource
    private {Repository} repository;
    // ...
}
```

**配置类**：
```java
@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "{config-prefix}")
public class {Config}Properties {
    // ...
}
```

### 2.2 实体类注解规范

**DTO/VO/BO 类**：
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class {Entity}DTO {
    // 字段定义
}
```

**PO 类（数据库实体）**：
```java
@Data
@TableName("{table_name}")
@Accessors(chain = true)
public class {Entity}PO {
    @TableId(type = IdType.AUTO)
    private Long id;
    // 其他字段
}
```

**枚举类**：
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

### 2.3 方法注释规范

```java
/**
 * 方法功能描述
 *
 * @author {author}
 * {HH:mm yyyy年MM月dd日}
 * @param paramName 参数说明
 * @return {@link ReturnType} 返回值说明
 */
public ReturnType methodName(ParamType paramName) {
    // ...
}
```

### 2.4 依赖注入规范

- 优先使用 `@Resource` 进行依赖注入
- 构造器注入用于必需依赖
- `@Autowired` 作为备选方案

```java
@Service
public class ExampleService {
    @Resource
    private UserRepository userRepository;
    
    @Autowired
    private AsyncEventBus asyncEventBus;
}
```

---

## 三、Maven 配置规范

### 3.1 版本管理

**父 POM 配置**：
```xml
<properties>
    <maven.compiler.source>8</maven.compiler.source>
    <maven.compiler.target>8</maven.compiler.target>
    <java.version>1.8</java.version>
    <project.encoding>UTF-8</project.encoding>
    
    <!-- Spring 版本 -->
    <spring-boot.version>2.3.2.RELEASE</spring-boot.version>
    <spring-cloud.version>Hoxton.SR9</spring-cloud.version>
    <spring-cloud-alibaba.version>2.2.6.RELEASE</spring-cloud-alibaba.version>
    
    <!-- 框架版本 -->
    <quickshop-framework.version>1.0.0</quickshop-framework.version>
    <quickcem-feign.version>1.2.146.3</quickcem-feign.version>
    
    <!-- 常用依赖版本 -->
    <lombok.version>1.18.24</lombok.version>
    <fastjson.version>1.2.75</fastjson.version>
    <hutool.version>5.7.15</hutool.version>
    <mybatis-plus.version>3.5.2</mybatis-plus.version>
    <redisson.version>3.17.6</redisson.version>
</properties>
```

### 3.2 依赖管理

```xml
<dependencyManagement>
    <dependencies>
        <!-- Spring Cloud 版本依赖 -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>${spring-cloud.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        <!-- Spring Boot 版本依赖 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-parent</artifactId>
            <version>${spring-boot.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        <!-- Spring Cloud Alibaba 版本依赖 -->
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-alibaba-dependencies</artifactId>
            <version>${spring-cloud-alibaba.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### 3.3 构建配置

```xml
<build>
    <pluginManagement>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <version>2.7.4</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>repackage</goal>
                        </goals>
                    </execution>
                </executions>
                <configuration>
                    <executable>true</executable>
                    <fork>true</fork>
                </configuration>
            </plugin>
        </plugins>
    </pluginManagement>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.8.1</version>
            <configuration>
                <source>${java.version}</source>
                <target>${java.version}</target>
                <encoding>${project.encoding}</encoding>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### 3.4 仓库配置

```xml
<distributionManagement>
    <repository>
        <id>maven-releases</id>
        <name>maven-releases</name>
        <url>http://nexus.quickcep.com/repository/maven-releases/</url>
    </repository>
    <snapshotRepository>
        <id>maven-snapshots</id>
        <name>maven-snapshots</name>
        <url>http://nexus.quickcep.com/repository/maven-snapshots/</url>
    </snapshotRepository>
</distributionManagement>
```

---

## 四、API 设计规范

### 4.1 RESTful 接口规范

```java
@RestController
@RequestMapping("/{resource}")
@Api(tags = "{资源}API")
public class {Resource}Api {

    @ApiOperation(value = "查询列表")
    @GetMapping
    public ResponseResult<PageResult<{Entity}VO>> list(
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        // ...
    }

    @ApiOperation(value = "查询详情")
    @GetMapping("/{id}")
    public ResponseResult<{Entity}VO> get(@PathVariable("id") Long id) {
        // ...
    }

    @ApiOperation(value = "创建")
    @PostMapping
    public ResponseResult<{Entity}VO> create(@RequestBody @Valid {Entity}DTO dto) {
        // ...
    }

    @ApiOperation(value = "更新")
    @PutMapping("/{id}")
    public ResponseResult<{Entity}VO> update(
            @PathVariable("id") Long id,
            @RequestBody @Valid {Entity}DTO dto) {
        // ...
    }

    @ApiOperation(value = "删除")
    @DeleteMapping("/{id}")
    public ResponseResult<Void> delete(@PathVariable("id") Long id) {
        // ...
    }
}
```

### 4.2 统一响应格式

使用 `com.quickshop.framework.response.ResponseResult` 封装响应：

```java
// 成功响应
return ResponseResult.success(data);
return ResponseResult.querySuccess(data);

// 失败响应
return ResponseResult.failed("error.message.key");
return ResponseResult.validateFailed("validation.error");
```

### 4.3 Token 校验

```java
// 需要 Token 校验的接口（默认）
@GetMapping("/protected")
public ResponseResult<?> protectedApi() { }

// 跳过 Token 校验
@IgnoreToken
@GetMapping("/public")
public ResponseResult<?> publicApi() { }
```

---

## 五、配置规范

### 5.1 多环境配置

支持的环境：
- `local` - 本地开发
- `dev` - 开发环境
- `test` / `testa` / `testb` / `testc` - 测试环境
- `prod` - 生产环境

### 5.2 Nacos 配置

```yaml
spring:
  cloud:
    nacos:
      config:
        server-addr: ${NACOS_SERVER_ADDR:nacos:8848}
        namespace: ${NACOS_NAMESPACE:}
        group: DEFAULT_GROUP
        file-extension: yml
        extension-configs:
          - data-id: ext-message-platform.yml
            group: DEFAULT_GROUP
            refresh: true
          - data-id: ext-config-redisson.yml
            group: DEFAULT_GROUP
            refresh: true
```

### 5.3 日志配置

```yaml
logging:
  level:
    root: INFO
    com.quickcem: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
```

---

## 六、Docker 与部署规范

### 6.1 Dockerfile 模板

```dockerfile
FROM hwo-harbor.quickcep.com/library/base/java:1.8.0

LABEL author={author}@quickcep.com

WORKDIR /opt/app
COPY app.jar /opt/app

EXPOSE {port}

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 6.2 端口分配

| 服务类型 | 端口范围 | 示例 |
|---------|---------|------|
| CDP 服务 | 8200-8299 | cdp-analysis: 8203 |
| IM 服务 | 8300-8399 | quickcem-im: 8301 |
| 工单服务 | 8400-8499 | apaas-business: 8401 |
| 业务服务 | 8500-8599 | quickcem-store: 8501 |

---

## 七、文档规范

### 7.1 README 模板

每个项目根目录应包含：
- `README.md` - 英文说明
- `README-CH.md` - 中文说明

**README-CH.md 模板**：
```markdown
# {项目名称}

{项目简介}

## 项目信息

| 属性 | 值 |
|------|-----|
| GroupId | `{groupId}` |
| ArtifactId | `{artifactId}` |
| 版本 | {version} |
| JDK 版本 | 1.8 |
| Spring Boot | 2.3.2.RELEASE |

## 项目结构

{项目模块结构}

## 核心功能

{功能列表}

## 技术特点

{技术栈说明}

## 许可证

内部项目 - QuickCEP
```

### 7.2 代码注释规范

- 类注释：说明类的职责和作者
- 方法注释：说明方法功能、参数、返回值
- 复杂逻辑：添加行内注释说明

---

## 八、常用框架 Starter

| Starter | 功能 | 引入方式 |
|---------|------|---------|
| `swagger-spring-boot-starter` | API 文档 | `com.quickshop.framework` |
| `jwt-spring-boot-starter` | JWT 认证 | `com.quickshop.framework` |
| `response-spring-boot-starter` | 统一响应 | `com.quickshop.framework` |
| `trace-spring-boot-starter` | 链路追踪 | `com.quickshop.framework` |
| `timezone-spring-boot-starter` | 时区处理 | `com.quickshop.framework` |
| `translate-spring-boot-starter` | 翻译服务 | `com.quickshop.framework` |
| `oss-spring-boot-starter` | 对象存储 | `com.quickshop.framework` |
| `uid-spring-boot-starter` | 分布式ID | `com.quickshop.framework` |
| `quick-message-spring-boot-starter` | 消息推送 | `com.quickcep.message` |

---

## 九、服务间调用规范

### 9.1 Feign Client 定义

所有 Feign Client 统一定义在 `quickcem-feign` 项目中：

```java
@FeignClient(name = "{service-name}", path = "/{api-prefix}")
public interface {Service}Client {
    
    @GetMapping("/{resource}/{id}")
    ResponseResult<{Entity}VO> getById(@PathVariable("id") Long id);
    
    @PostMapping("/{resource}")
    ResponseResult<{Entity}VO> create(@RequestBody {Entity}DTO dto);
}
```

### 9.2 服务调用

```java
@Service
public class ExampleService {
    @Resource
    private {Service}Client serviceClient;
    
    public void doSomething() {
        ResponseResult<{Entity}VO> result = serviceClient.getById(id);
        if (result.isSuccess()) {
            // 处理成功响应
        }
    }
}
```

---

## 十、数据库规范

### 10.1 表命名

- 使用小写字母和下划线
- 前缀表示业务模块：`im_`, `cdp_`, `ticket_`
- 示例：`im_chat_session`, `cdp_user_data`

### 10.2 字段命名

- 使用小写字母和下划线
- 主键：`id`
- 外键：`{table}_id`
- 时间字段：`create_time`, `update_time`
- 状态字段：`status`, `is_deleted`

### 10.3 MyBatis-Plus 配置

```java
@TableName("{table_name}")
public class {Entity}PO {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("store_id")
    private Integer storeId;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer isDeleted;
}
```

---

## 附录：版本对照表

| 组件 | 推荐版本 |
|-----|---------|
| JDK | 1.8 |
| Spring Boot | 2.3.2.RELEASE |
| Spring Cloud | Hoxton.SR9 |
| Spring Cloud Alibaba | 2.2.6.RELEASE |
| MyBatis-Plus | 3.5.2 |
| Redisson | 3.17.6 |
| Hutool | 5.7.15 |
| FastJSON | 1.2.75 |
| Lombok | 1.18.24 |
| ShardingSphere | 5.1.1 |
| XXL-Job | 2.3.1 |
| Kafka Clients | 3.0.0 |

---

**文档版本**: 1.0.0  
**更新日期**: 2026-01-29  
**维护团队**: QuickCEP 研发团队


---

## 十一、中间件使用规范

### 11.1 Redis/Redisson 使用规范

#### 11.1.1 依赖引入

```xml
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-boot-starter</artifactId>
    <version>${redisson.version}</version>
</dependency>
```

#### 11.1.2 RedissonClient 注入

```java
@Service
public class ExampleService {
    @Resource
    private RedissonClient redissonClient;
}
```

#### 11.1.3 分布式缓存（RBucket）

```java
// 缓存读取与写入
public OrganTreeVO organTreeCache(Integer storeId) {
    RBucket<OrganTreeVO> bucket = redissonClient.getBucket(CommonConstant.getOrganKey(storeId));
    if (!bucket.isExists()) {
        bucket.set(organTree(storeId));
    }
    return bucket.get();
}

// 缓存删除
public void deleteCache(Integer storeId) {
    redissonClient.getBucket(CommonConstant.getOrganKey(storeId)).delete();
}
```

#### 11.1.4 分布式锁（RLock）

```java
// 标准分布式锁使用模式
public void doWithLock(Integer storeId) {
    RLock lock = redissonClient.getLock("business:lock:" + storeId);
    try {
        // tryLock(等待时间, 锁持有时间, 时间单位)
        if (lock.tryLock(3, 5, TimeUnit.SECONDS)) {
            try {
                // 业务逻辑
                doBusinessLogic();
            } finally {
                lock.unlock();
            }
        }
    } catch (InterruptedException e) {
        log.error("获取锁失败", e);
        Thread.currentThread().interrupt();
    }
}

// 简化锁模式（适用于短时操作）
public void doWithSimpleLock(Long id) {
    RLock lock = redissonClient.getLock("simple:lock:" + id);
    lock.lock(5, TimeUnit.SECONDS);
    try {
        // 业务逻辑
    } finally {
        lock.unlock();
    }
}
```

#### 11.1.5 缓存 Key 命名规范

| 场景 | Key 格式 | 示例 |
|-----|---------|------|
| 组织机构 | `organ:{storeId}` | `organ:193` |
| 员工详情 | `staff:detail:{staffId}` | `staff:detail:12345` |
| 分布式锁 | `{module}:lock:{resource}:{id}` | `sla:lock:config:193` |
| 会话缓存 | `session:{type}:{id}` | `session:distribute:rule:193` |

---

### 11.2 Kafka 使用规范

#### 11.2.1 依赖配置

```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:kafka:9092}
    consumer:
      group-id: ${spring.application.name}
      auto-offset-reset: earliest
      enable-auto-commit: false
    producer:
      retries: 3
      acks: all
```

#### 11.2.2 生产者使用

```java
@Component
@Slf4j
public class EventPublisher {
    @Resource
    private KafkaTemplate<String, String> kafkaTemplate;
    
    @Resource
    private EventKafkaProperties props;
    
    public void publish(EventDTO dto) {
        String message = JSON.toJSONString(dto);
        kafkaTemplate.send(props.getEventTopic(), message);
        log.info("发送Kafka消息: topic={}, message={}", props.getEventTopic(), message);
    }
}
```

#### 11.2.3 消费者使用

```java
@Component
@Slf4j
public class EventListener {
    
    @KafkaListener(
        id = "eventListener",
        groupId = "${kafka.topic.eventTopicGroup}",
        topics = "${kafka.topic.eventTopic}",
        properties = {
            "session.timeout.ms=90000",
            "max.poll.interval.ms=1800000",
            "request.timeout.ms=60000"
        }
    )
    public void onMessage(List<ConsumerRecord<String, String>> records) {
        log.info("收到Kafka消息: size={}", records.size());
        long start = System.currentTimeMillis();
        
        for (ConsumerRecord<String, String> record : records) {
            try {
                EventDTO dto = JSON.parseObject(record.value(), EventDTO.class);
                processEvent(dto);
            } catch (Exception e) {
                log.error("处理消息异常: {}", e.getMessage(), e);
                // 重试逻辑
                handleRetry(record);
            }
        }
        
        log.info("处理完成: size={}, cost={}ms", records.size(), System.currentTimeMillis() - start);
    }
}
```

#### 11.2.4 Topic 命名规范

| 场景 | Topic 格式 | 示例 |
|-----|-----------|------|
| 事件数据 | `{module}-{event}-topic` | `email-distribution-topic` |
| 附件处理 | `{module}-attachment-topic` | `email-attachment-topic` |
| 数据同步 | `{module}-sync-topic` | `user-data-sync-topic` |

---

### 11.3 QMQ 延时消息规范

#### 11.3.1 配置类

```java
@Data
@Configuration
@Slf4j
public class QMQConfig {
    
    @Value("${qmq.metaServer}")
    private String metaServer;
    
    @Bean
    public MessageProducer producer() {
        MessageProducerProvider producer = new MessageProducerProvider();
        producer.setAppCode(QMQConstant.APP_CODE);
        producer.setMetaServer(metaServer + QMQConstant.META_ADDRESS);
        log.info("QMQ生产者启动成功");
        return producer;
    }
    
    @Bean
    public ThreadPoolExecutor qmqConsumerExecutor() {
        int core = Runtime.getRuntime().availableProcessors();
        return new ThreadPoolExecutor(
            core, core * 2, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(10000),
            new BasicThreadFactory.Builder()
                .daemon(true)
                .namingPattern("qmq-consumer-%d")
                .build()
        );
    }
}
```

#### 11.3.2 生产者使用（支持延时）

```java
@Component
@Slf4j
public class QMQEventPublisher {
    @Resource
    private MessageProducer qmqProducer;
    
    @Value("${qmq.topic.event}")
    private String eventTopic;
    
    /**
     * 发送延时消息
     * @param dto 消息体
     * @param delaySeconds 延时秒数
     */
    public void publish(EventDTO dto, int delaySeconds) {
        Message msg = qmqProducer.generateMessage(eventTopic);
        msg.setProperty("payload", JSON.toJSONString(dto));
        msg.setDelayTime(delaySeconds, TimeUnit.SECONDS);
        qmqProducer.sendMessage(msg);
        log.info("发送QMQ延时消息: topic={}, delay={}s", eventTopic, delaySeconds);
    }
    
    /**
     * 指数退避重试
     */
    public int backoff(int retryTimes) {
        int[] delays = {5, 10, 30, 60, 120};
        return delays[Math.min(retryTimes, delays.length - 1)];
    }
}
```

#### 11.3.3 消费者使用

```java
@Component
@Slf4j
public class QMQEventConsumer {
    
    @QmqConsumer(
        subject = "${qmq.topic.event}",
        consumerGroup = "${qmq.group.event}",
        executor = "qmqConsumerExecutor"
    )
    public void onMessage(Message message) {
        String payload = message.getStringProperty("payload");
        log.info("收到QMQ消息: {}", payload);
        
        try {
            EventDTO dto = JSON.parseObject(payload, EventDTO.class);
            processEvent(dto);
        } catch (Exception e) {
            log.error("处理QMQ消息异常", e);
        }
    }
}
```

---

### 11.4 本地缓存（Caffeine）规范

#### 11.4.1 依赖引入

```xml
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

#### 11.4.2 LoadingCache 模式（推荐）

```java
@Slf4j
@Service
public class StoreInfoCache {
    
    private static final int MAXIMUM_SIZE = 2000;
    
    @Resource
    private StoreClient storeClient;
    
    /**
     * 自动加载缓存
     * - maximumSize: 最大缓存条目数
     * - expireAfterWrite: 写入后过期时间
     */
    private final LoadingCache<String, StoreInfo> storeInfoCache = Caffeine.newBuilder()
            .maximumSize(MAXIMUM_SIZE)
            .expireAfterWrite(Duration.ofMinutes(5L))
            .build(this::fetchStoreInfo);
    
    private StoreInfo fetchStoreInfo(String accessId) {
        ResponseResult<StoreInfoDTO> result = storeClient.getStoreInfo(accessId);
        if (result == null || result.getData() == null) {
            log.warn("获取店铺信息失败: accessId={}", accessId);
            return null;
        }
        StoreInfoDTO dto = result.getData();
        return StoreInfo.builder()
                .storeId(dto.getStoreId())
                .platform(dto.getPlatform())
                .build();
    }
    
    public StoreInfo getStoreInfo(String accessId) {
        return storeInfoCache.get(accessId);
    }
}
```

#### 11.4.3 Cache 模式（手动管理）

```java
@Component
@Slf4j
public class SessionCache {
    
    /**
     * 手动管理缓存
     * - expireAfterAccess: 访问后过期时间
     */
    private static final Cache<Integer, String> CACHE = Caffeine.newBuilder()
            .maximumSize(1024)
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build();
    
    public String getHost(Integer storeId) {
        return CACHE.get(storeId, key -> {
            // 缓存未命中时的加载逻辑
            return fetchHostFromRemote(key);
        });
    }
    
    public void invalidate(Integer storeId) {
        CACHE.invalidate(storeId);
    }
}
```

#### 11.4.4 Caffeine 配置参数说明

| 参数 | 说明 | 推荐值 |
|-----|------|-------|
| `maximumSize` | 最大缓存条目数 | 1000-5000 |
| `expireAfterWrite` | 写入后过期时间 | 5-30分钟 |
| `expireAfterAccess` | 访问后过期时间 | 10-60分钟 |
| `softValues` | 软引用值（内存不足时回收） | 大对象缓存 |

---

### 11.5 ThreadLocal 缓存规范

#### 11.5.1 会话上下文缓存

```java
// 使用框架提供的 SessionThreadLocalCache
import com.quickshop.framework.jwt.cache.SessionThreadLocalCache;

@Service
public class ExampleService {
    
    public void doSomething() {
        // 获取当前用户信息
        UserInfo userInfo = SessionThreadLocalCache.getUserInfo();
        Integer storeId = userInfo.getStoreId();
        Long staffId = userInfo.getStaffId();
        
        // 业务逻辑
    }
}
```

#### 11.5.2 自定义 ThreadLocal 缓存

```java
public class RequestContextHolder {
    
    private static final ThreadLocal<RequestContext> CONTEXT = new ThreadLocal<>();
    
    public static void set(RequestContext context) {
        CONTEXT.set(context);
    }
    
    public static RequestContext get() {
        return CONTEXT.get();
    }
    
    public static void clear() {
        CONTEXT.remove();
    }
}
```

---

### 11.6 集合工具类规范

#### 11.6.1 推荐使用顺序

1. **Apache Commons Collections4**（首选）
2. **Hutool CollUtil**（备选）
3. **Spring CollectionUtils**（简单场景）

#### 11.6.2 空集合判断

```java
import org.apache.commons.collections4.CollectionUtils;

// 推荐写法
if (CollectionUtils.isEmpty(list)) {
    return Collections.emptyList();
}

if (CollectionUtils.isNotEmpty(list)) {
    // 处理逻辑
}
```

#### 11.6.3 集合操作

```java
import org.apache.commons.collections4.CollectionUtils;

// 交集
Collection<Long> intersection = CollectionUtils.intersection(listA, listB);

// 差集
Collection<Long> subtract = CollectionUtils.subtract(listA, listB);

// 并集
Collection<Long> union = CollectionUtils.union(listA, listB);
```

#### 11.6.4 Hutool 集合工具

```java
import cn.hutool.core.collection.CollUtil;

// 创建集合
List<String> list = CollUtil.newArrayList("a", "b", "c");
Set<String> set = CollUtil.newHashSet("a", "b", "c");

// 集合转换
List<Long> ids = CollUtil.map(entities, Entity::getId, true);
```

---

### 11.7 字符串工具类规范

#### 11.7.1 推荐使用顺序

1. **Apache Commons Lang3 StringUtils**（首选）
2. **Hutool StrUtil**（备选）
3. **Spring StringUtils**（简单场景）

#### 11.7.2 空字符串判断

```java
import org.apache.commons.lang3.StringUtils;

// 推荐写法
if (StringUtils.isBlank(str)) {
    // null、空字符串、纯空白字符
}

if (StringUtils.isNotBlank(str)) {
    // 非空且非纯空白
}

if (StringUtils.isEmpty(str)) {
    // null 或空字符串
}
```

#### 11.7.3 字符串操作

```java
import org.apache.commons.lang3.StringUtils;

// 默认值
String value = StringUtils.defaultIfBlank(input, "default");

// 截取
String sub = StringUtils.substring(str, 0, 10);

// 拼接
String joined = StringUtils.join(list, ",");

// 比较
boolean equals = StringUtils.equals(str1, str2);
boolean equalsIgnoreCase = StringUtils.equalsIgnoreCase(str1, str2);
```

#### 11.7.4 Hutool 字符串工具

```java
import cn.hutool.core.util.StrUtil;

// 格式化
String msg = StrUtil.format("用户{}登录成功", username);

// 驼峰转换
String camel = StrUtil.toCamelCase("user_name");  // userName
String underline = StrUtil.toUnderlineCase("userName");  // user_name

// 空白处理
String trimmed = StrUtil.trim(str);
String cleaned = StrUtil.cleanBlank(str);
```

---

### 11.8 JSON 处理规范

#### 11.8.1 FastJSON 使用（项目标准）

```java
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONArray;

// 对象转JSON字符串
String jsonStr = JSON.toJSONString(object);

// JSON字符串转对象
UserDTO user = JSON.parseObject(jsonStr, UserDTO.class);

// JSON字符串转List
List<UserDTO> users = JSON.parseArray(jsonStr, UserDTO.class);

// JSONObject 操作
JSONObject json = JSON.parseObject(jsonStr);
String name = json.getString("name");
Integer age = json.getInteger("age");
```

#### 11.8.2 日志中的 JSON 输出

```java
@Slf4j
public class ExampleService {
    
    public void process(RequestDTO request) {
        log.info("处理请求: {}", JSON.toJSONString(request));
        
        try {
            // 业务逻辑
        } catch (Exception e) {
            log.error("处理失败: request={}", JSON.toJSONString(request), e);
        }
    }
}
```

---

### 11.9 中间件版本对照表

| 中间件 | 推荐版本 | 说明 |
|-------|---------|------|
| Redisson | 3.17.6+ | 分布式缓存/锁 |
| Kafka Clients | 3.0.0 | 消息队列 |
| QMQ | 1.1.26 | 延时消息 |
| Caffeine | 2.9.3 | 本地缓存 |
| Hutool | 5.7.15 | 工具集 |
| FastJSON | 1.2.75 | JSON处理 |
| Commons Lang3 | 3.12.0 | 字符串工具 |
| Commons Collections4 | 4.4 | 集合工具 |

