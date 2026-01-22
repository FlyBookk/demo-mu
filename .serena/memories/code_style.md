# 代码风格和规范

## 前端 (Vue 3 + TypeScript)

### 文件命名
- Vue 组件：PascalCase，如 `FbaShipmentList.vue`
- TypeScript 文件：camelCase，如 `fbaShipment.ts`
- 样式文件：kebab-case，如 `fba-shipment.scss`

### 组件结构
```vue
<template>
  <!-- 模板 -->
</template>

<script setup lang="ts">
// 使用 Composition API + script setup
import { ref, reactive, computed, onMounted } from 'vue'

// 类型导入
import type { SomeType } from '@/types/xxx'

// API 导入
import { someApi } from '@/api/xxx'

// 响应式状态
const loading = ref(false)
const formData = reactive({})

// 方法
function handleSubmit() {}

// 生命周期
onMounted(() => {})
</script>

<style lang="scss" scoped>
// 使用 SCSS，scoped 作用域
</style>
```

### API 层结构
- 路径：`src/api/xxx.ts`
- 使用 `request` 工具封装
- 导出函数形式的 API 调用

### 类型定义
- 路径：`src/types/xxx.ts`
- 使用 TypeScript interface 定义数据类型

## 后端 (Java + Spring Boot)

### 包结构
```
com.musheng.business.xxx
├── controller/  # 控制器层
├── service/     # 服务层接口
│   └── impl/    # 服务层实现
├── mapper/      # MyBatis Mapper
└── entity/      # 实体类
```

### 注解使用
- `@RestController` + `@RequestMapping` 定义 REST API
- `@RequiredArgsConstructor` 构造器注入
- `@OperationLog` 操作日志
- Swagger 注解：`@Tag`, `@Operation`, `@Parameter`

### 命名规范
- 类名：PascalCase
- 方法名：camelCase
- 常量：UPPER_SNAKE_CASE

## 提交规范
使用 Conventional Commits：
- `feat:` 新功能
- `fix:` 修复 bug
- `docs:` 文档更新
- `refactor:` 重构
- `test:` 测试
- `chore:` 构建/工具
