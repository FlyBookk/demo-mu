---
inclusion: always
---

# 语言偏好设置

## 输出语言

**所有输出必须使用中文**，包括但不限于：

- 需求文档 (requirements.md)
- 设计文档 (design.md)
- 任务列表 (tasks.md)
- 代码注释
- 错误消息
- 日志消息
- API 文档描述
- 用户交互回复

## 例外情况

以下内容保持英文：

- 代码中的变量名、方法名、类名（遵循 Java 命名规范）
- 技术术语（如 BigDecimal、MyBatis-Plus、Spring Boot 等）
- 文件名和路径
- Git 提交消息的 type 前缀（如 feat:、fix:、docs:）

## 示例

### ✅ 正确

```java
/**
 * 验证屏幕比例值是否有效
 * 
 * @param screenRatio 屏幕比例值
 * @return 有效返回 true，否则返回 false
 * @author wanhua
 * 10:30 2026年01月29日
 */
public boolean isValidScreenRatio(BigDecimal screenRatio) {
    // 检查是否为有效值
    if (screenRatio == null) {
        return true;
    }
    return VALID_SCREEN_RATIOS.contains(screenRatio);
}
```

### ❌ 错误

```java
/**
 * Validate if screen ratio value is valid
 * 
 * @param screenRatio screen ratio value
 * @return true if valid, false otherwise
 */
public boolean isValidScreenRatio(BigDecimal screenRatio) {
    // Check if it's a valid value
    ...
}
```

## 文档模板

创建 spec 文档时使用中文标题：

- `# 需求文档` 而非 `# Requirements Document`
- `## 验收标准` 而非 `## Acceptance Criteria`
- `## 设计概述` 而非 `## Design Overview`
- `## 任务列表` 而非 `## Tasks`
