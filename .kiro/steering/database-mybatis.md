---
inclusion: fileMatch
fileMatchPattern: "**/*Mapper.java,**/*Mapper.xml,**/mapper/**/*.java,**/entity/**/*.java"
---

# 数据库与 MyBatis-Plus 规范

当编写或修改数据库相关代码时，遵循以下规范。

## 表命名规范

- 使用小写字母和下划线
- 业务模块前缀: `sales_`, `shipping_`, `fba_`, `advertising_`
- 示例: `sales_data`, `shipping_data`, `fba_shipment`

## 字段命名规范

- 使用小写字母和下划线
- 主键: `id`
- 外键: `{table}_id`
- 时间字段: `create_time`, `update_time`
- 状态字段: `status`, `deleted`
- 店铺ID: `shop_id`（用于数据隔离）

## PO 实体类

```java
@Data
@TableName("sales_data")
public class SalesData {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long shopId;  // 店铺ID（数据隔离）
    
    private String name;
    
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    
    @TableLogic
    private Boolean deleted;
}
```

## Mapper 接口

```java
@Mapper
public interface {Entity}Mapper extends BaseMapper<{Entity}> {
    
    // 自定义查询方法
    List<{Entity}> selectByCondition(@Param("param") QueryParam param);
    
    // 物理删除方法
    @Delete("DELETE FROM table_name WHERE id = #{id}")
    int physicalDeleteById(@Param("id") Long id);
}
```

## Lambda 查询

```java
// 条件查询
List<Entity> list = mapper.selectList(
    new LambdaQueryWrapper<Entity>()
        .eq(Entity::getShopId, shopId)
        .eq(Entity::getStatus, status)
        .orderByDesc(Entity::getCreateTime)
);

// 更新
mapper.update(null, 
    new LambdaQueryWrapper<Entity>()
        .set(Entity::getStatus, newStatus)
        .eq(Entity::getId, id)
);

// 删除（逻辑删除）
mapper.deleteById(id);
```

## Service 层使用

```java
@Service
public class EntityServiceImpl implements EntityService {
    
    @Autowired
    private EntityMapper mapper;
    
    public List<Entity> listByShopId(Long shopId) {
        return mapper.selectList(
            new LambdaQueryWrapper<Entity>()
                .eq(Entity::getShopId, shopId)
        );
    }
    
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(Long id, Integer status) {
        Entity entity = new Entity();
        entity.setId(id);
        entity.setStatus(status);
        return mapper.updateById(entity) > 0;
    }
}
```

## 分页查询

```java
// Controller
@GetMapping
public Result<PageResult<EntityVO>> list(
        @RequestParam(defaultValue = "1") Integer current,
        @RequestParam(defaultValue = "10") Integer size) {
    Page<Entity> page = new Page<>(current, size);
    mapper.selectPage(page, new LambdaQueryWrapper<Entity>());
    return Result.success(convert(page));
}
```
