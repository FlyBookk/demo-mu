---
inclusion: fileMatch
fileMatchPattern: "**/*Controller.java,**/*Api.java,**/controller/**/*.java,**/facade/**/*.java"
---

# API 设计规范

当编写或修改 Controller/API 类时，遵循以下规范。

## RESTful 接口模板

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

## 响应格式

使用 `com.quickshop.framework.response.ResponseResult` 封装：

```java
// 成功响应
return ResponseResult.success(data);
return ResponseResult.querySuccess(data);

// 失败响应
return ResponseResult.failed("error.message.key");
return ResponseResult.validateFailed("validation.error");
```

## Token 校验

```java
// 需要 Token 校验（默认）
@GetMapping("/protected")
public ResponseResult<?> protectedApi() { }

// 跳过 Token 校验
@IgnoreToken
@GetMapping("/public")
public ResponseResult<?> publicApi() { }
```

## Swagger 注解

- 类级别: `@Api(tags = "资源名称")`
- 方法级别: `@ApiOperation(value = "操作描述")`
- 参数级别: `@ApiParam(value = "参数描述")`

## 参数校验

使用 `@Valid` 配合 JSR-303 注解：
```java
public ResponseResult<?> create(@RequestBody @Valid CreateDTO dto) { }
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
