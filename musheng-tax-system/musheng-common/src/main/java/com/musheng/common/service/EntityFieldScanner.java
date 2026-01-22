package com.musheng.common.service;

import com.musheng.common.dto.TargetFieldInfo;

import java.util.List;

/**
 * 实体字段扫描服务接口
 * 通过反射扫描实体类的 @FieldMapping 注解，自动生成目标字段配置
 */
public interface EntityFieldScanner {

    /**
     * 扫描指定实体类，获取目标字段列表
     *
     * @param entityClass 实体类
     * @return 目标字段列表
     */
    List<TargetFieldInfo> scanEntityFields(Class<?> entityClass);

    /**
     * 根据数据类型获取对应的实体类
     *
     * @param dataType   数据类型：SALES, SHIPPING, ADVERTISING, RATE
     * @param sourceType 子类型（仅 SALES 有效）：ORIGINAL, ERP
     * @return 对应的实体类
     */
    Class<?> getEntityClass(String dataType, String sourceType);

    /**
     * 根据数据类型获取目标字段列表
     *
     * @param dataType   数据类型
     * @param sourceType 子类型
     * @return 目标字段列表
     */
    List<TargetFieldInfo> getTargetFields(String dataType, String sourceType);

    /**
     * 清除缓存
     */
    void clearCache();

    /**
     * 清除指定数据类型的缓存
     *
     * @param dataType 数据类型
     */
    void clearCache(String dataType);
}
