---
inclusion: fileMatch
fileMatchPattern: "**/*Mapper.java,**/*Mapper.xml,**/repository/**/*.java,**/po/**/*.java"
---

# 数据库与 MyBatis-Plus 规范

当编写或修改数据库相关代码时，遵循以下规范。

## 表命名规范

- 使用小写字母和下划线
- 前缀表示业务模块: `im_`, `cdp_`, `ticket_`, `store_`
- 示例: `im_chat_session`, `cdp_user_data`, `store_staff`

## 字段命名规范

- 使用小写字母和下划线
- 主键: `id`
- 外键: `{table}_id`
- 时间字段: `create_time`, `update_time`
- 状态字段: `status`, `is_deleted`

## PO 实体类

```java
@Data
@TableName("{table_name}")
@Accessors(chain = true)
public class {Entity}PO {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("store_id")
    private Integer storeId;
    
    @TableField("name")
    private String name;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer isDeleted;
}
```

## Mapper 接口

```java
@Mapper
public interface {Entity}Mapper extends BaseMapper<{Entity}PO> {
    
    // 自定义查询方法
    List<{Entity}PO> selectByCondition(@Param("param") QueryParam param);
    
    // 分页查询
    Page<{Entity}PO> selectPage(Page<{Entity}PO> page, @Param("param") QueryParam param);
}
```

## Lambda 查询

```java
// 条件查询
List<EntityPO> list = mapper.selectList(
    Wrappers.<EntityPO>lambdaQuery()
        .eq(EntityPO::getStoreId, storeId)
        .eq(EntityPO::getStatus, status)
        .orderByDesc(EntityPO::getCreateTime)
);

// 更新
mapper.update(null, 
    Wrappers.<EntityPO>lambdaUpdate()
        .set(EntityPO::getStatus, newStatus)
        .eq(EntityPO::getId, id)
);

// 删除
mapper.delete(
    Wrappers.<EntityPO>lambdaQuery()
        .eq(EntityPO::getStoreId, storeId)
        .in(EntityPO::getId, idList)
);
```

## Service 层使用

```java
@Service
public class EntityServiceImpl extends ServiceImpl<EntityMapper, EntityPO> 
        implements IEntityService {
    
    public List<EntityPO> listByStoreId(Integer storeId) {
        return lambdaQuery()
            .eq(EntityPO::getStoreId, storeId)
            .list();
    }
    
    public boolean updateStatus(Long id, Integer status) {
        return lambdaUpdate()
            .set(EntityPO::getStatus, status)
            .eq(EntityPO::getId, id)
            .update();
    }
}
```

## 分页查询

```java
// Controller
@GetMapping
public ResponseResult<PageResult<EntityVO>> list(
        @RequestParam(defaultValue = "1") Integer pageNum,
        @RequestParam(defaultValue = "10") Integer pageSize) {
    Page<EntityPO> page = service.page(new Page<>(pageNum, pageSize));
    return ResponseResult.pageSuccess(convert(page));
}
```
