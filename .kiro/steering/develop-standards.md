---
inclusion: always
---

# QuickCEP 研发规范 (Develop Standards)

本规范用于指导 Kiro 在 QuickCEP 项目群中进行代码生成、修改和审查时保持风格一致性。

## 核心原则

1. **遵循 DDD 分层架构** - 复杂服务使用标准 DDD 分层，轻量服务使用简化分层
2. **统一代码风格** - 使用项目约定的注解、命名和注释规范
3. **中间件标准化** - 按规范使用 Redis、Kafka、QMQ、Caffeine 等中间件

## 项目架构

### 标准 DDD 分层（复杂业务）
```
{project}-client/           # Feign Client 定义
{project}-interfaces/       # Controller 层
{project}-application/      # Service 编排层
{project}-domain/           # 领域层
{project}-infrastructure/   # 基础设施层
{project}-start/            # 启动模块
```

### 简化分层（轻量服务）
```
{project}-core/             # 核心业务
{project}-domain/           # 领域模型
{project}-starter/          # 启动模块
```

## 包命名规范

| 层级 | 包名 |
|-----|------|
| 控制器 | `controller` / `facade` |
| 服务层 | `service` / `service.impl` |
| 实体 | `entity` / `domain.entity` |
| DTO/VO | `dto` / `vo` / `bo` |
| 数据访问 | `repository.mapper` |
| 枚举 | `enums` / `enumertion` |
| 配置 | `config` |
| 工具 | `utils` / `util` |

## GroupId 规范

- 框架组件: `com.quickshop.framework`
- QuickCEM 业务: `com.quickcem`
- 消息平台: `com.quickcep.message`
- 工单系统: `com.7moor.apass` / `com.m7`

## 技术栈版本

- JDK: 1.8
- Spring Boot: 2.3.2.RELEASE
- Spring Cloud: Hoxton.SR9
- Spring Cloud Alibaba: 2.2.6.RELEASE
- MyBatis-Plus: 3.5.2
- Redisson: 3.17.6
- Hutool: 5.7.15
- FastJSON: 1.2.75

详细规范请参考: #[[file:develop-agent.md]]
