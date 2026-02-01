---
inclusion: fileMatch
fileMatchPattern: "**/pom.xml"
---

# Maven 配置规范

当编写或修改 pom.xml 时，遵循以下规范。

## 版本管理

使用统一的版本属性：

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

## GroupId 规范

| 项目类型 | GroupId |
|---------|---------|
| 框架组件 | `com.quickshop.framework` |
| QuickCEM 业务 | `com.quickcem` |
| 消息平台 | `com.quickcep.message` |
| 工单系统 | `com.7moor.apass` / `com.m7` |

## 仓库配置

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

## 常用 Starter

| Starter | GroupId | 功能 |
|---------|---------|------|
| `swagger-spring-boot-starter` | `com.quickshop.framework` | API 文档 |
| `jwt-spring-boot-starter` | `com.quickshop.framework` | JWT 认证 |
| `response-spring-boot-starter` | `com.quickshop.framework` | 统一响应 |
| `trace-spring-boot-starter` | `com.quickshop.framework` | 链路追踪 |
| `oss-spring-boot-starter` | `com.quickshop.framework` | 对象存储 |
| `quick-message-spring-boot-starter` | `com.quickcep.message` | 消息推送 |
