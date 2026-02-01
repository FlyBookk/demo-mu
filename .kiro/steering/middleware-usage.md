---
inclusion: manual
---

# 中间件使用规范

当需要使用 Redis、Kafka、QMQ、Caffeine 等中间件时，参考本规范。

## Redis/Redisson

### 分布式缓存（RBucket）
```java
@Resource
private RedissonClient redissonClient;

// 缓存读写
RBucket<DataVO> bucket = redissonClient.getBucket("cache:key:" + id);
if (!bucket.isExists()) {
    bucket.set(loadData(id));
}
return bucket.get();

// 缓存删除
redissonClient.getBucket("cache:key:" + id).delete();
```

### 分布式锁（RLock）
```java
RLock lock = redissonClient.getLock("business:lock:" + id);
try {
    if (lock.tryLock(3, 5, TimeUnit.SECONDS)) {
        try {
            // 业务逻辑
        } finally {
            lock.unlock();
        }
    }
} catch (InterruptedException e) {
    log.error("获取锁失败", e);
    Thread.currentThread().interrupt();
}
```

### Key 命名规范
- 组织机构: `organ:{storeId}`
- 员工详情: `staff:detail:{staffId}`
- 分布式锁: `{module}:lock:{resource}:{id}`

## Kafka

### 生产者
```java
@Resource
private KafkaTemplate<String, String> kafkaTemplate;

public void publish(EventDTO dto) {
    kafkaTemplate.send(topic, JSON.toJSONString(dto));
}
```

### 消费者
```java
@KafkaListener(
    id = "eventListener",
    groupId = "${kafka.topic.eventTopicGroup}",
    topics = "${kafka.topic.eventTopic}",
    properties = {
        "session.timeout.ms=90000",
        "max.poll.interval.ms=1800000"
    }
)
public void onMessage(List<ConsumerRecord<String, String>> records) {
    for (ConsumerRecord<String, String> record : records) {
        EventDTO dto = JSON.parseObject(record.value(), EventDTO.class);
        processEvent(dto);
    }
}
```

## QMQ 延时消息

### 生产者（支持延时）
```java
@Resource
private MessageProducer qmqProducer;

public void publish(EventDTO dto, int delaySeconds) {
    Message msg = qmqProducer.generateMessage(topic);
    msg.setProperty("payload", JSON.toJSONString(dto));
    msg.setDelayTime(delaySeconds, TimeUnit.SECONDS);
    qmqProducer.sendMessage(msg);
}
```

### 消费者
```java
@QmqConsumer(
    subject = "${qmq.topic.event}",
    consumerGroup = "${qmq.group.event}",
    executor = "qmqConsumerExecutor"
)
public void onMessage(Message message) {
    String payload = message.getStringProperty("payload");
    EventDTO dto = JSON.parseObject(payload, EventDTO.class);
    processEvent(dto);
}
```

## Caffeine 本地缓存

### LoadingCache（自动加载）
```java
private final LoadingCache<String, StoreInfo> cache = Caffeine.newBuilder()
        .maximumSize(2000)
        .expireAfterWrite(Duration.ofMinutes(5L))
        .build(this::fetchData);

public StoreInfo get(String key) {
    return cache.get(key);
}
```

### Cache（手动管理）
```java
private static final Cache<Integer, String> CACHE = Caffeine.newBuilder()
        .maximumSize(1024)
        .expireAfterAccess(10, TimeUnit.MINUTES)
        .build();

public String get(Integer key) {
    return CACHE.get(key, k -> fetchFromRemote(k));
}
```

## ThreadLocal 缓存

```java
// 使用框架提供的会话缓存
UserInfo userInfo = SessionThreadLocalCache.getUserInfo();
Integer storeId = userInfo.getStoreId();
Long staffId = userInfo.getStaffId();
```
