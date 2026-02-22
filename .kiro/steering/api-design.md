---
inclusion: fileMatch
fileMatchPattern: "**/*Controller.java,**/*Api.java,**/controller/**/*.java"
---

# API 设计规范

当编写或修改 Controller/API 类时，遵循以下规范。

## RESTful 接口模板

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

## 响应格式

使用 `com.musheng.common.result.Result` 封装：

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

## 权限校验

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
```

## Knife4j 注解（OpenAPI 3）

- 类级别: `@Tag(name = "资源名称")`
- 方法级别: `@Operation(summary = "操作描述")`
- 参数级别: `@Parameter(description = "参数描述")`

## 参数校验

使用 `@Valid` 配合 JSR-303 注解：
```java
public Result<?> create(@RequestBody @Valid CreateDTO dto) { }
```

DTO 中使用校验注解：
```java
@Data
public class CreateDTO {
    @NotBlank(message = "名称不能为空")
    private String name;
    
    @NotNull(message = "类型不能为空")
    private Integer type;
    
    @Size(max = 500, message = "描述不能超过500字符")
    private String description;
}
```
