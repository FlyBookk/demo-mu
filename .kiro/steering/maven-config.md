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

## GroupId 规范

| 项目类型 | GroupId |
|---------|---------|
| 慕声税务系统 | `com.musheng` |

## 常用依赖

| 依赖 | GroupId | ArtifactId | 功能 |
|-----|---------|-----------|------|
| Spring Boot | `org.springframework.boot` | `spring-boot-starter-web` | Web 框架 |
| MyBatis-Plus | `com.baomidou` | `mybatis-plus-spring-boot3-starter` | ORM 框架 |
| Sa-Token | `cn.dev33` | `sa-token-spring-boot3-starter` | 认证授权 |
| MySQL | `com.mysql` | `mysql-connector-j` | 数据库驱动 |
| Hutool | `cn.hutool` | `hutool-all` | 工具集 |
| EasyExcel | `com.alibaba` | `easyexcel` | Excel 处理 |
| Knife4j | `com.github.xiaoymin` | `knife4j-openapi3-jakarta-spring-boot-starter` | API 文档 |
| Lombok | `org.projectlombok` | `lombok` | 代码简化 |
