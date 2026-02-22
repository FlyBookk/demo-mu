---
inclusion: always
---

# 慕声税务系统研发规范 (Musheng Tax System Development Standards)

本规范用于指导 Kiro 在慕声亚马逊转口贸易报税系统中进行代码生成、修改和审查时保持风格一致性。

## 核心原则

1. **模块化架构** - 使用 Maven 多模块管理，清晰的职责划分
2. **统一代码风格** - 使用项目约定的注解、命名和注释规范
3. **中文注释** - 所有代码注释、文档、日志消息使用中文
4. **现代化技术栈** - 使用 Spring Boot 3.x、Java 17、Sa-Token 等现代框架

## 项目架构

### Maven 模块结构
```
musheng-tax-system/         # 后端项目根目录
├── musheng-common/         # 公共模块（工具类、异常、结果封装）
├── musheng-system/         # 系统模块（用户、角色、权限、日志）
├── musheng-config/         # 配置模块（Swagger、CORS、Sa-Token）
├── musheng-business/       # 业务模块（销售、配送、FBA、广告数据）
└── musheng-web/            # Web 模块（启动类、Controller）
```

## 包命名规范

| 层级 | 包名 |
|-----|------|
| 控制器 | `controller` |
| 服务层 | `service` / `service.impl` |
| 实体 | `entity` |
| DTO/VO | `dto` / `vo` |
| 数据访问 | `mapper` |
| 枚举 | `enums` |
| 配置 | `config` |
| 工具 | `utils` |

## GroupId 规范

- 项目 GroupId: `com.musheng`

## 技术栈版本

- JDK: 17
- Spring Boot: 3.2.2
- MyBatis-Plus: 3.5.5
- Sa-Token: 1.37.0
- MySQL: 8.0.33
- Hutool: 5.8.25
- EasyExcel: 3.3.3
- Knife4j: 4.3.0

详细规范请参考: #[[file:develop-agent.md]]
