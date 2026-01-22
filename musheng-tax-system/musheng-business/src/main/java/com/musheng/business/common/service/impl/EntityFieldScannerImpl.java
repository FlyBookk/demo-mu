package com.musheng.business.common.service.impl;

import com.musheng.business.advertising.entity.AdvertisingData;
import com.musheng.business.rate.entity.ExchangeRate;
import com.musheng.business.sales.entity.SalesData;
import com.musheng.business.shipping.entity.ShippingData;
import com.musheng.common.annotation.FieldMapping;
import com.musheng.common.dto.TargetFieldInfo;
import com.musheng.common.service.EntityFieldScanner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 实体字段扫描服务实现
 * 通过反射扫描实体类的 @FieldMapping 注解，自动生成目标字段配置
 */
@Slf4j
@Service
public class EntityFieldScannerImpl implements EntityFieldScanner {

    /**
     * 数据类型到实体类的映射
     */
    private static final Map<String, Class<?>> DATA_TYPE_ENTITY_MAP = Map.of(
            "SALES", SalesData.class,
            "SHIPPING", ShippingData.class,
            "ADVERTISING", AdvertisingData.class,
            "RATE", ExchangeRate.class
    );

    /**
     * 缓存：避免重复扫描
     */
    private final Map<String, List<TargetFieldInfo>> fieldCache = new ConcurrentHashMap<>();

    @Override
    public List<TargetFieldInfo> scanEntityFields(Class<?> entityClass) {
        if (entityClass == null) {
            return Collections.emptyList();
        }

        List<TargetFieldInfo> fields = new ArrayList<>();

        // 获取所有字段（包括父类）
        List<Field> allFields = getAllFields(entityClass);

        for (Field field : allFields) {
            FieldMapping annotation = field.getAnnotation(FieldMapping.class);
            
            // 跳过没有注解的字段
            if (annotation == null) {
                continue;
            }
            
            // 跳过忽略的字段
            if (annotation.ignore()) {
                continue;
            }

            TargetFieldInfo info = new TargetFieldInfo();
            info.setField(field.getName()); // 使用 Java 驼峰命名
            info.setLabel(annotation.label());
            info.setDescription(annotation.description().isEmpty() ? null : annotation.description());
            info.setType(getFieldType(field.getType()));
            info.setRequired(annotation.required());
            
            if (annotation.maxLength() > 0) {
                info.setMaxLength(annotation.maxLength());
            }
            
            if ("number".equals(info.getType())) {
                info.setPrecision(annotation.precision());
            }

            // 使用 order 作为排序依据
            info.setSortOrder(annotation.order());

            fields.add(info);
        }

        // 按 order 排序
        fields.sort(Comparator.comparingInt(TargetFieldInfo::getSortOrder));

        log.info("扫描实体类 {} 完成，共 {} 个目标字段", entityClass.getSimpleName(), fields.size());
        return fields;
    }

    @Override
    public Class<?> getEntityClass(String dataType, String sourceType) {
        // 目前所有子类型共用同一个实体类
        // 如果将来需要区分，可以在这里添加逻辑
        return DATA_TYPE_ENTITY_MAP.get(dataType);
    }

    @Override
    public List<TargetFieldInfo> getTargetFields(String dataType, String sourceType) {
        String cacheKey = dataType + ":" + (sourceType != null ? sourceType : "");
        
        return fieldCache.computeIfAbsent(cacheKey, key -> {
            Class<?> entityClass = getEntityClass(dataType, sourceType);
            if (entityClass == null) {
                log.warn("未找到数据类型 {} 对应的实体类", dataType);
                return Collections.emptyList();
            }
            return scanEntityFields(entityClass);
        });
    }

    @Override
    public void clearCache() {
        fieldCache.clear();
        log.info("目标字段缓存已清除");
    }

    @Override
    public void clearCache(String dataType) {
        fieldCache.keySet().removeIf(key -> key.startsWith(dataType + ":"));
        log.info("数据类型 {} 的目标字段缓存已清除", dataType);
    }

    /**
     * 获取所有字段（包括父类）
     */
    private List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = clazz;
        
        while (current != null && current != Object.class) {
            fields.addAll(Arrays.asList(current.getDeclaredFields()));
            current = current.getSuperclass();
        }
        
        return fields;
    }

    /**
     * Java 驼峰命名转数据库下划线命名
     */
    private String camelToSnake(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) {
            return camelCase;
        }
        
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < camelCase.length(); i++) {
            char c = camelCase.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    result.append('_');
                }
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * 根据 Java 类型获取字段类型字符串
     */
    private String getFieldType(Class<?> type) {
        if (type == String.class) {
            return "string";
        } else if (type == Integer.class || type == int.class ||
                   type == Long.class || type == long.class ||
                   type == BigDecimal.class ||
                   type == Double.class || type == double.class ||
                   type == Float.class || type == float.class) {
            return "number";
        } else if (type == LocalDateTime.class || type == LocalDate.class ||
                   type == Date.class) {
            return "datetime";
        } else if (type == Boolean.class || type == boolean.class) {
            return "boolean";
        }
        return "string"; // 默认
    }
}
