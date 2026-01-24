# 店铺系统设计与实施计划

**目标：** 实现类SaaS多租户的店铺数据隔离机制，业务模块数据必须归属店铺

**架构：** 店铺作为数据隔离维度，与站点独立；通过全局店铺选择器 + 路由守卫强制业务模块选择店铺

**技术栈：** Spring Boot + MyBatis-Plus / Vue 3 + Pinia + Ant Design Vue

---

## 一、模块归属设计

### 公共模块（无需店铺）
- 基础配置：货币、站点、交易类型映射、字段映射模板
- 汇率管理：汇率导入、汇率查询
- 系统管理：用户管理、权限管理、操作日志

### 业务模块（必须选择店铺）
- 销售数据：数据导入、数据列表
- 配送数据：数据导入、数据列表
- FBA货件明细：数据导入、数据列表
- 广告数据：广告费录入、数据列表
- 汇总报表：汇总查询、报表下载

### 导入记录
- 业务数据导入记录：关联店铺
- 汇率导入记录：不关联店铺

---

## 二、数据库设计

### 任务 1: 创建店铺表

**文件：** `musheng-tax-system/sql/v2.0_shop_system.sql`

```sql
-- =============================================
-- 店铺系统数据库变更
-- 版本: 2.0
-- 日期: 2026-01-24
-- =============================================

-- 1. 创建店铺表
CREATE TABLE t_shop (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    shop_code varchar(50) NOT NULL COMMENT '店铺编码',
    shop_name varchar(100) NOT NULL COMMENT '店铺名称',
    seller_id varchar(50) DEFAULT NULL COMMENT '亚马逊卖家ID',
    company_name varchar(200) DEFAULT NULL COMMENT '公司名称',
    tax_id varchar(50) DEFAULT NULL COMMENT '统一社会信用代码',
    status tinyint DEFAULT 1 COMMENT '状态(1启用/0禁用)',
    remark varchar(500) DEFAULT NULL COMMENT '备注',
    create_by bigint DEFAULT NULL COMMENT '创建人',
    create_time datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by bigint DEFAULT NULL COMMENT '更新人',
    update_time datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_shop_code (shop_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='店铺表';

-- 2. 业务表添加 shop_id 字段
-- 2.1 销售数据表
ALTER TABLE t_sales_data ADD COLUMN shop_id bigint DEFAULT NULL COMMENT '店铺ID' AFTER id;
ALTER TABLE t_sales_data ADD INDEX idx_shop_id (shop_id);

-- 2.2 配送数据表
ALTER TABLE t_shipping_data ADD COLUMN shop_id bigint DEFAULT NULL COMMENT '店铺ID' AFTER id;
ALTER TABLE t_shipping_data ADD INDEX idx_shop_id (shop_id);

-- 2.3 广告数据表
ALTER TABLE t_advertising_data ADD COLUMN shop_id bigint DEFAULT NULL COMMENT '店铺ID' AFTER id;
ALTER TABLE t_advertising_data ADD INDEX idx_shop_id (shop_id);

-- 2.4 汇总缓存表
ALTER TABLE t_summary_cache ADD COLUMN shop_id bigint DEFAULT NULL COMMENT '店铺ID' AFTER id;
ALTER TABLE t_summary_cache ADD INDEX idx_shop_id (shop_id);

-- 2.5 导入记录表
ALTER TABLE t_import_record ADD COLUMN shop_id bigint DEFAULT NULL COMMENT '店铺ID（业务数据导入）' AFTER id;
ALTER TABLE t_import_record ADD INDEX idx_shop_id (shop_id);

-- 3. 插入默认店铺（可选）
INSERT INTO t_shop (shop_code, shop_name, seller_id, company_name, tax_id, status, remark)
VALUES ('DEFAULT', '默认店铺', NULL, '东莞市慕声商贸有限公司', '91441900MA4WNG4C6H', 1, '系统默认店铺');
```

---

## 三、后端实施

### 任务 2: 创建店铺模块 (musheng-config)

**目录结构：**
```
musheng-config/src/main/java/com/musheng/config/shop/
├── controller/
│   └── ShopController.java
├── service/
│   ├── ShopService.java
│   └── impl/
│       └── ShopServiceImpl.java
├── mapper/
│   └── ShopMapper.java
├── entity/
│   └── Shop.java
└── dto/
    ├── ShopRequest.java
    └── ShopQueryRequest.java
```

#### 步骤 2.1: 创建 Shop 实体

**文件：** `musheng-config/.../shop/entity/Shop.java`

```java
package com.musheng.config.shop.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.musheng.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_shop")
public class Shop extends BaseEntity {
    
    /** 店铺编码 */
    private String shopCode;
    
    /** 店铺名称 */
    private String shopName;
    
    /** 亚马逊卖家ID */
    private String sellerId;
    
    /** 公司名称 */
    private String companyName;
    
    /** 统一社会信用代码 */
    private String taxId;
    
    /** 状态(1启用/0禁用) */
    private Integer status;
    
    /** 备注 */
    private String remark;
}
```

#### 步骤 2.2: 创建 ShopMapper

**文件：** `musheng-config/.../shop/mapper/ShopMapper.java`

```java
package com.musheng.config.shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.musheng.config.shop.entity.Shop;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ShopMapper extends BaseMapper<Shop> {
}
```

#### 步骤 2.3: 创建 DTO

**文件：** `musheng-config/.../shop/dto/ShopRequest.java`

```java
package com.musheng.config.shop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ShopRequest {
    
    @NotBlank(message = "店铺编码不能为空")
    @Size(max = 50, message = "店铺编码最多50字符")
    private String shopCode;
    
    @NotBlank(message = "店铺名称不能为空")
    @Size(max = 100, message = "店铺名称最多100字符")
    private String shopName;
    
    @Size(max = 50, message = "卖家ID最多50字符")
    private String sellerId;
    
    @Size(max = 200, message = "公司名称最多200字符")
    private String companyName;
    
    @Size(max = 50, message = "统一社会信用代码最多50字符")
    private String taxId;
    
    private Integer status = 1;
    
    @Size(max = 500, message = "备注最多500字符")
    private String remark;
}
```

**文件：** `musheng-config/.../shop/dto/ShopQueryRequest.java`

```java
package com.musheng.config.shop.dto;

import com.musheng.common.dto.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ShopQueryRequest extends PageRequest {
    
    /** 店铺编码（模糊） */
    private String shopCode;
    
    /** 店铺名称（模糊） */
    private String shopName;
    
    /** 状态 */
    private Integer status;
}
```

#### 步骤 2.4: 创建 Service

**文件：** `musheng-config/.../shop/service/ShopService.java`

```java
package com.musheng.config.shop.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.musheng.config.shop.dto.ShopQueryRequest;
import com.musheng.config.shop.dto.ShopRequest;
import com.musheng.config.shop.entity.Shop;
import java.util.List;

public interface ShopService extends IService<Shop> {
    
    /** 分页查询 */
    IPage<Shop> queryPage(ShopQueryRequest request);
    
    /** 查询所有启用的店铺（下拉选项） */
    List<Shop> listEnabled();
    
    /** 新增店铺 */
    Long createShop(ShopRequest request);
    
    /** 更新店铺 */
    void updateShop(Long id, ShopRequest request);
    
    /** 删除店铺 */
    void deleteShop(Long id);
    
    /** 根据编码查询 */
    Shop getByCode(String shopCode);
}
```

**文件：** `musheng-config/.../shop/service/impl/ShopServiceImpl.java`

```java
package com.musheng.config.shop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.musheng.common.exception.BusinessException;
import com.musheng.config.shop.dto.ShopQueryRequest;
import com.musheng.config.shop.dto.ShopRequest;
import com.musheng.config.shop.entity.Shop;
import com.musheng.config.shop.mapper.ShopMapper;
import com.musheng.config.shop.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements ShopService {

    @Override
    public IPage<Shop> queryPage(ShopQueryRequest request) {
        LambdaQueryWrapper<Shop> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(request.getShopCode()), Shop::getShopCode, request.getShopCode())
               .like(StringUtils.hasText(request.getShopName()), Shop::getShopName, request.getShopName())
               .eq(request.getStatus() != null, Shop::getStatus, request.getStatus())
               .orderByDesc(Shop::getCreateTime);
        
        return page(new Page<>(request.getPageNum(), request.getPageSize()), wrapper);
    }

    @Override
    public List<Shop> listEnabled() {
        return lambdaQuery()
                .eq(Shop::getStatus, 1)
                .orderByAsc(Shop::getShopCode)
                .list();
    }

    @Override
    public Long createShop(ShopRequest request) {
        // 检查编码唯一性
        if (getByCode(request.getShopCode()) != null) {
            throw new BusinessException("店铺编码已存在");
        }
        
        Shop shop = new Shop();
        BeanUtils.copyProperties(request, shop);
        save(shop);
        return shop.getId();
    }

    @Override
    public void updateShop(Long id, ShopRequest request) {
        Shop existing = getById(id);
        if (existing == null) {
            throw new BusinessException("店铺不存在");
        }
        
        // 如果修改了编码，检查唯一性
        if (!existing.getShopCode().equals(request.getShopCode())) {
            if (getByCode(request.getShopCode()) != null) {
                throw new BusinessException("店铺编码已存在");
            }
        }
        
        BeanUtils.copyProperties(request, existing);
        existing.setId(id);
        updateById(existing);
    }

    @Override
    public void deleteShop(Long id) {
        Shop shop = getById(id);
        if (shop == null) {
            throw new BusinessException("店铺不存在");
        }
        
        // TODO: 检查是否有关联业务数据
        
        removeById(id);
    }

    @Override
    public Shop getByCode(String shopCode) {
        return lambdaQuery()
                .eq(Shop::getShopCode, shopCode)
                .one();
    }
}
```

#### 步骤 2.5: 创建 Controller

**文件：** `musheng-config/.../shop/controller/ShopController.java`

```java
package com.musheng.config.shop.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.musheng.common.annotation.OperationLog;
import com.musheng.common.result.Result;
import com.musheng.config.shop.dto.ShopQueryRequest;
import com.musheng.config.shop.dto.ShopRequest;
import com.musheng.config.shop.entity.Shop;
import com.musheng.config.shop.service.ShopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "店铺管理")
@RestController
@RequestMapping("/api/config/shop")
@RequiredArgsConstructor
public class ShopController {

    private final ShopService shopService;

    @Operation(summary = "分页查询店铺")
    @GetMapping("/page")
    public Result<IPage<Shop>> page(ShopQueryRequest request) {
        return Result.success(shopService.queryPage(request));
    }

    @Operation(summary = "获取启用的店铺列表（下拉选项）")
    @GetMapping("/options")
    public Result<List<Shop>> options() {
        return Result.success(shopService.listEnabled());
    }

    @Operation(summary = "获取店铺详情")
    @GetMapping("/{id}")
    public Result<Shop> getById(@PathVariable Long id) {
        return Result.success(shopService.getById(id));
    }

    @Operation(summary = "新增店铺")
    @PostMapping
    @OperationLog(module = "店铺管理", operation = "新增店铺")
    public Result<Long> create(@Valid @RequestBody ShopRequest request) {
        return Result.success(shopService.createShop(request));
    }

    @Operation(summary = "更新店铺")
    @PutMapping("/{id}")
    @OperationLog(module = "店铺管理", operation = "更新店铺")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ShopRequest request) {
        shopService.updateShop(id, request);
        return Result.success();
    }

    @Operation(summary = "删除店铺")
    @DeleteMapping("/{id}")
    @OperationLog(module = "店铺管理", operation = "删除店铺")
    public Result<Void> delete(@PathVariable Long id) {
        shopService.deleteShop(id);
        return Result.success();
    }
}
```

---

### 任务 3: 店铺上下文机制

#### 步骤 3.1: 创建 ShopContext

**文件：** `musheng-common/.../context/ShopContext.java`

```java
package com.musheng.common.context;

/**
 * 店铺上下文 - 使用 ThreadLocal 存储当前请求的店铺ID
 */
public class ShopContext {
    
    private static final ThreadLocal<Long> CURRENT_SHOP_ID = new ThreadLocal<>();
    
    public static void setShopId(Long shopId) {
        CURRENT_SHOP_ID.set(shopId);
    }
    
    public static Long getShopId() {
        return CURRENT_SHOP_ID.get();
    }
    
    public static Long requireShopId() {
        Long shopId = CURRENT_SHOP_ID.get();
        if (shopId == null) {
            throw new IllegalStateException("未选择店铺");
        }
        return shopId;
    }
    
    public static void clear() {
        CURRENT_SHOP_ID.remove();
    }
}
```

#### 步骤 3.2: 创建拦截器

**文件：** `musheng-web/.../interceptor/ShopContextInterceptor.java`

```java
package com.musheng.web.interceptor;

import com.musheng.common.context.ShopContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ShopContextInterceptor implements HandlerInterceptor {
    
    public static final String SHOP_ID_HEADER = "X-Shop-Id";
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String shopIdStr = request.getHeader(SHOP_ID_HEADER);
        if (StringUtils.hasText(shopIdStr)) {
            try {
                Long shopId = Long.parseLong(shopIdStr);
                ShopContext.setShopId(shopId);
            } catch (NumberFormatException ignored) {
                // 忽略无效的店铺ID
            }
        }
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                                Object handler, Exception ex) {
        ShopContext.clear();
    }
}
```

#### 步骤 3.3: 注册拦截器

**文件：** `musheng-web/.../config/WebMvcConfig.java` (修改)

```java
// 添加拦截器注册
@Override
public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(shopContextInterceptor)
            .addPathPatterns("/api/**")
            .excludePathPatterns("/api/auth/**");
}
```

---

### 任务 4: 业务模块改造

#### 步骤 4.1: 业务实体添加 shopId 字段

以 SalesData 为例：

**修改：** `musheng-business/.../sales/entity/SalesData.java`

```java
// 添加字段
/** 店铺ID */
private Long shopId;
```

同样修改：
- `ShippingData.java`
- `AdvertisingData.java` 
- `SummaryCache.java`

#### 步骤 4.2: 业务查询添加店铺过滤

以 SalesDataService 为例：

```java
@Override
public IPage<SalesData> queryPage(SalesDataQueryRequest request) {
    Long shopId = ShopContext.requireShopId(); // 强制要求店铺ID
    
    LambdaQueryWrapper<SalesData> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(SalesData::getShopId, shopId); // 自动过滤
    // ... 其他条件
}
```

#### 步骤 4.3: 导入记录关联店铺

修改 ImportRecord 实体和导入逻辑，业务数据导入时关联店铺ID。

---

## 四、前端实施

### 任务 5: 创建店铺 Store

**文件：** `musheng-tax-web/src/stores/modules/shop.ts`

```typescript
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getShopOptions } from '@/api/shop'

export interface Shop {
  id: number
  shopCode: string
  shopName: string
  sellerId?: string
  companyName?: string
  taxId?: string
  status: number
}

export const useShopStore = defineStore('shop', () => {
  // State
  const currentShop = ref<Shop | null>(null)
  const shopList = ref<Shop[]>([])
  const loading = ref(false)

  // Getters
  const currentShopId = computed(() => currentShop.value?.id)
  const currentShopName = computed(() => currentShop.value?.shopName || '请选择店铺')
  const hasSelectedShop = computed(() => !!currentShop.value)

  // Actions
  async function fetchShopList() {
    loading.value = true
    try {
      const res = await getShopOptions()
      shopList.value = res.data || []
      
      // 如果只有一个店铺，自动选择
      if (shopList.value.length === 1 && !currentShop.value) {
        setCurrentShop(shopList.value[0])
      }
      
      // 恢复之前选择的店铺
      const savedShopId = localStorage.getItem('currentShopId')
      if (savedShopId && !currentShop.value) {
        const shop = shopList.value.find(s => s.id === Number(savedShopId))
        if (shop) {
          setCurrentShop(shop)
        }
      }
    } finally {
      loading.value = false
    }
  }

  function setCurrentShop(shop: Shop | null) {
    currentShop.value = shop
    if (shop) {
      localStorage.setItem('currentShopId', String(shop.id))
    } else {
      localStorage.removeItem('currentShopId')
    }
  }

  function clearCurrentShop() {
    currentShop.value = null
    localStorage.removeItem('currentShopId')
  }

  return {
    // State
    currentShop,
    shopList,
    loading,
    
    // Getters
    currentShopId,
    currentShopName,
    hasSelectedShop,
    
    // Actions
    fetchShopList,
    setCurrentShop,
    clearCurrentShop
  }
})
```

### 任务 6: 创建店铺选择器组件

**文件：** `musheng-tax-web/src/components/business/ShopSelector.vue`

```vue
<template>
  <a-dropdown :trigger="['click']">
    <div class="shop-selector" :class="{ 'no-shop': !hasSelectedShop }">
      <ShopOutlined />
      <span class="shop-name">{{ currentShopName }}</span>
      <DownOutlined />
    </div>
    <template #overlay>
      <a-menu @click="handleSelect">
        <a-menu-item 
          v-for="shop in shopList" 
          :key="shop.id"
          :class="{ 'selected': shop.id === currentShopId }"
        >
          <CheckOutlined v-if="shop.id === currentShopId" />
          <span>{{ shop.shopName }}</span>
          <span class="shop-code">{{ shop.shopCode }}</span>
        </a-menu-item>
        <a-menu-divider v-if="showManage" />
        <a-menu-item v-if="showManage" key="manage">
          <SettingOutlined />
          <span>店铺管理</span>
        </a-menu-item>
      </a-menu>
    </template>
  </a-dropdown>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ShopOutlined, DownOutlined, CheckOutlined, SettingOutlined } from '@ant-design/icons-vue'
import { useShopStore } from '@/stores/modules/shop'
import { useAuthStore } from '@/stores/modules/auth'

const router = useRouter()
const shopStore = useShopStore()
const authStore = useAuthStore()

const currentShopId = computed(() => shopStore.currentShopId)
const currentShopName = computed(() => shopStore.currentShopName)
const hasSelectedShop = computed(() => shopStore.hasSelectedShop)
const shopList = computed(() => shopStore.shopList)
const showManage = computed(() => authStore.isAdmin)

onMounted(() => {
  shopStore.fetchShopList()
})

function handleSelect({ key }: { key: string | number }) {
  if (key === 'manage') {
    router.push('/config/shop')
    return
  }
  
  const shop = shopList.value.find(s => s.id === key)
  if (shop) {
    shopStore.setCurrentShop(shop)
  }
}
</script>

<style lang="scss" scoped>
.shop-selector {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s;
  
  &:hover {
    background: rgba(0, 0, 0, 0.04);
  }
  
  &.no-shop {
    color: #ff4d4f;
    background: #fff2f0;
    
    &:hover {
      background: #ffccc7;
    }
  }
  
  .shop-name {
    max-width: 150px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.selected {
  background: #e6f7ff;
}

.shop-code {
  margin-left: 8px;
  color: #999;
  font-size: 12px;
}
</style>
```

### 任务 7: 修改 MainLayout 添加店铺选择器

**修改：** `musheng-tax-web/src/layouts/MainLayout.vue`

在 header-right 区域添加店铺选择器：

```vue
<div class="header-right">
  <!-- 店铺选择器 -->
  <ShopSelector class="shop-selector-wrapper" />
  
  <!-- 用户下拉 -->
  <a-dropdown>
    ...
  </a-dropdown>
</div>
```

### 任务 8: 创建店铺路由守卫

**文件：** `musheng-tax-web/src/router/guards.ts` (修改)

```typescript
// 需要选择店铺的业务模块
const BUSINESS_ROUTES = [
  'Sales', 'SalesImport', 'SalesList',
  'Shipping', 'ShippingImport', 'ShippingList',
  'FbaShipment', 'FbaShipmentImport', 'FbaShipmentList',
  'Advertising', 'AdvertisingImport', 'AdvertisingAdd', 'AdvertisingList',
  'Report', 'ReportSummary', 'ReportDownload'
]

router.beforeEach((to, from, next) => {
  // ... 现有的认证守卫
  
  // 店铺选择守卫
  if (BUSINESS_ROUTES.includes(to.name as string)) {
    const shopStore = useShopStore()
    if (!shopStore.hasSelectedShop) {
      message.warning('请先选择店铺')
      // 可以跳转到首页或显示店铺选择弹窗
      return next('/dashboard')
    }
  }
  
  next()
})
```

### 任务 9: Axios 请求拦截器添加店铺头

**修改：** `musheng-tax-web/src/utils/request.ts`

```typescript
// 请求拦截器
service.interceptors.request.use(
  (config) => {
    // Token
    const token = getToken()
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    
    // 店铺ID
    const shopStore = useShopStore()
    if (shopStore.currentShopId) {
      config.headers['X-Shop-Id'] = shopStore.currentShopId
    }
    
    return config
  }
)
```

### 任务 10: 导入确认弹窗

**创建：** `musheng-tax-web/src/components/business/ImportConfirmModal.vue`

```vue
<template>
  <a-modal
    v-model:open="visible"
    title="确认导入"
    @ok="handleConfirm"
    @cancel="handleCancel"
  >
    <a-alert type="warning" show-icon>
      <template #message>
        <p>您即将向以下店铺导入数据：</p>
        <p class="shop-info">
          <ShopOutlined />
          <strong>{{ shopName }}</strong>
          <span>({{ shopCode }})</span>
        </p>
        <p>请确认店铺选择正确，导入后数据将归属该店铺。</p>
      </template>
    </a-alert>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ShopOutlined } from '@ant-design/icons-vue'
import { useShopStore } from '@/stores/modules/shop'

const emit = defineEmits(['confirm', 'cancel'])
const shopStore = useShopStore()

const visible = ref(false)
const shopName = computed(() => shopStore.currentShop?.shopName || '')
const shopCode = computed(() => shopStore.currentShop?.shopCode || '')

function show() {
  visible.value = true
}

function handleConfirm() {
  visible.value = false
  emit('confirm')
}

function handleCancel() {
  visible.value = false
  emit('cancel')
}

defineExpose({ show })
</script>
```

### 任务 11: 创建店铺管理页面

**文件：** `musheng-tax-web/src/views/config/shop/index.vue`

（完整的CRUD页面，参考现有的站点管理页面结构）

---

## 五、路由配置

### 任务 12: 添加店铺管理路由

**修改：** `musheng-tax-web/src/router/routes.ts`

在基础配置下添加：

```typescript
{
  path: 'shop',
  name: 'Shop',
  component: () => import('@/views/config/shop/index.vue'),
  meta: {
    title: '店铺管理',
    permission: 'config:shop'
  }
}
```

---

## 六、实施顺序

| 阶段 | 任务 | 预估工作量 |
|------|------|-----------|
| 1 | 执行数据库迁移脚本 | 10分钟 |
| 2 | 后端店铺模块 CRUD | 2小时 |
| 3 | 店铺上下文机制 | 1小时 |
| 4 | 前端店铺 Store | 30分钟 |
| 5 | 前端店铺选择器组件 | 1小时 |
| 6 | 修改 MainLayout | 30分钟 |
| 7 | 路由守卫 + Axios拦截器 | 30分钟 |
| 8 | 店铺管理页面 | 2小时 |
| 9 | 业务模块改造（实体+Service） | 3小时 |
| 10 | 导入确认弹窗 | 1小时 |
| 11 | 集成测试 | 2小时 |

**总计：** 约 13-14 小时

---

## 七、后续扩展（可选）

1. **用户-店铺权限关联** - 限制用户只能访问特定店铺
2. **店铺切换时清空缓存** - 切换店铺后刷新业务数据
3. **店铺数据统计** - 首页展示店铺维度的数据概览
4. **店铺导入导出** - 批量管理店铺配置
