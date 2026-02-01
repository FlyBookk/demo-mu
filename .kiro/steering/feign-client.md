---
inclusion: fileMatch
fileMatchPattern: "**/*Client.java,**/client/**/*.java"
---

# Feign Client 规范

当编写或修改 Feign Client 时，遵循以下规范。

## Feign Client 定义

所有 Feign Client 统一定义在 `quickcem-feign` 项目中：

```java
@FeignClient(name = "{service-name}", path = "/{api-prefix}")
public interface {Service}Client {
    
    @GetMapping("/{resource}/{id}")
    ResponseResult<{Entity}VO> getById(@PathVariable("id") Long id);
    
    @PostMapping("/{resource}")
    ResponseResult<{Entity}VO> create(@RequestBody {Entity}DTO dto);
    
    @PutMapping("/{resource}/{id}")
    ResponseResult<{Entity}VO> update(
        @PathVariable("id") Long id, 
        @RequestBody {Entity}DTO dto);
    
    @DeleteMapping("/{resource}/{id}")
    ResponseResult<Void> delete(@PathVariable("id") Long id);
    
    @GetMapping("/{resource}")
    ResponseResult<List<{Entity}VO>> list(@RequestParam("storeId") Integer storeId);
}
```

## 服务调用

```java
@Service
public class ExampleService {
    @Resource
    private {Service}Client serviceClient;
    
    public EntityVO getEntity(Long id) {
        ResponseResult<EntityVO> result = serviceClient.getById(id);
        if (result.isSuccess()) {
            return result.getData();
        }
        log.warn("调用服务失败: {}", result.getMessage());
        return null;
    }
}
```

## 服务名称规范

| 服务 | name | path |
|-----|------|------|
| 店铺服务 | `quickcem-store` | `/store` |
| IM 服务 | `quickcem-im` | `/im` |
| CDP 服务 | `quickcem-cdp-analysis` | `/cdp` |
| 工单服务 | `apaas-business` | `/business` |

## 注意事项

1. 返回值统一使用 `ResponseResult<T>` 包装
2. 路径参数使用 `@PathVariable`
3. 请求体使用 `@RequestBody`
4. 查询参数使用 `@RequestParam`
5. 调用方需要处理 `result.isSuccess()` 判断
