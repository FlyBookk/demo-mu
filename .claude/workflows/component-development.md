# 前端组件开发标准流程

**适用场景**: 开发新的前端组件（业务组件或通用组件）

**前置条件**:
- [ ] 组件需求已明确（来自PRD或技术设计）
- [ ] 技术设计已完成（如果是复杂组件）
- [ ] 设计稿已就绪（如果涉及UI）

---

## 执行步骤

### 1️⃣ 准备阶段

#### 1.1 切换角色
```
/agent 前端
```

#### 1.2 查看当前待办
```
/todo 开发
```

#### 1.3 搜索相关文档
```
/doc [组件名称]
```

查找以下信息：
- 组件功能需求
- 设计稿和交互说明
- 类似组件参考
- 技术实现要求

---

### 2️⃣ 设计阶段

#### 2.1 确定组件类型和位置
- **业务组件**: `src/components/business/[ComponentName]/`
- **通用组件**: `src/components/common/[ComponentName]/`
- **页面组件**: `src/views/[module]/[page]/`

#### 2.2 分析组件结构
确定需要创建的文件：
```
ComponentName/
├── index.vue           # 主组件
├── types.ts            # 类型定义（如果复杂）
├── hooks/              # 组合式函数（如果需要）
│   └── useXxx.ts
├── components/         # 子组件（如果需要）
│   └── SubComponent.vue
└── style.scss          # 样式文件（如果独立）
```

#### 2.3 设计组件接口
定义：
- **Props**: 组件接收的属性
- **Emits**: 组件触发的事件
- **Slots**: 插槽定义
- **Expose**: 暴露的方法和属性

示例：
```typescript
// Props
interface ComponentProps {
  data: DataType[]
  loading?: boolean
  size?: 'small' | 'medium' | 'large'
}

// Emits
interface ComponentEmits {
  (e: 'select', item: DataType): void
  (e: 'change', value: string): void
}
```

---

### 3️⃣ 开发阶段

#### 3.1 创建文件结构
```bash
# 创建组件目录
mkdir -p src/components/business/ComponentName
```

#### 3.2 编写类型定义
文件: `types.ts`
- 定义 Props 类型
- 定义 Emits 类型
- 定义数据结构类型
- 导出所有类型

#### 3.3 实现主组件
文件: `index.vue`

**模板结构**:
```vue
<template>
  <div class="component-name">
    <!-- 组件内容 -->
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import type { ComponentProps, ComponentEmits } from './types'

// 1. Props定义
const props = withDefaults(defineProps<ComponentProps>(), {
  loading: false,
  size: 'medium'
})

// 2. Emits定义
const emit = defineEmits<ComponentEmits>()

// 3. 响应式数据
const internalData = ref<DataType[]>([])

// 4. 计算属性
const computedValue = computed(() => {
  // ...
})

// 5. 方法
const handleAction = () => {
  emit('change', 'value')
}

// 6. 生命周期
onMounted(() => {
  // 初始化逻辑
})

// 7. 暴露方法（如需要）
defineExpose({
  refresh: () => {
    // ...
  }
})
</script>

<style lang="scss" scoped>
.component-name {
  // 样式
}
</style>
```

#### 3.4 编写组合式函数（如需要）
文件: `hooks/useXxx.ts`
- 复用逻辑提取为Hook
- 完整的类型定义
- 返回值明确

#### 3.5 编写子组件（如需要）
按照主组件的规范编写

#### 3.6 API接口封装（如需要）
文件: `src/api/xxx.ts`
```typescript
import { request } from '@/utils/request'
import type { DataType } from '@/types'

export function getData(): Promise<DataType[]> {
  return request.get('/api/v1/xxx')
}
```

---

### 4️⃣ 测试阶段

#### 4.1 本地开发服务器
```bash
npm run dev
```

#### 4.2 功能测试清单
- [ ] 组件正常渲染
- [ ] Props 传递正常
- [ ] Events 触发正常
- [ ] Slots 插槽正常
- [ ] 暴露方法可调用
- [ ] 样式显示正确
- [ ] 响应式正常

#### 4.3 边界情况测试
- [ ] Props 为空/undefined
- [ ] 数据为空数组
- [ ] 极端值处理
- [ ] 错误情况处理

#### 4.4 性能检查
- [ ] 无不必要的重渲染
- [ ] 无内存泄漏
- [ ] 大数据量正常

#### 4.5 代码质量检查
```bash
# ESLint检查
npm run lint

# 类型检查
npm run type-check
```

---

### 5️⃣ 文档阶段

#### 5.1 添加组件注释
```vue
<script setup lang="ts">
/**
 * [组件名称]组件
 *
 * @description [组件功能描述]
 * @example
 * <ComponentName
 *   :data="data"
 *   @select="handleSelect"
 * />
 */
</script>
```

#### 5.2 创建使用示例（如果是公共组件）
文件: `examples/ComponentName.vue`

#### 5.3 更新组件索引
文件: `src/components/index.ts`
```typescript
export { default as ComponentName } from './business/ComponentName/index.vue'
```

---

### 6️⃣ 提交阶段

#### 6.1 代码审查清单
- [ ] TypeScript类型完整
- [ ] Props有默认值
- [ ] 事件命名符合规范（kebab-case）
- [ ] 无console.log未删除
- [ ] 无未使用的变量/导入
- [ ] 样式使用scoped
- [ ] 注释完整清晰

#### 6.2 提交代码
```bash
git add .
git commit -m "feat: 添加[组件名称]组件

- 实现了[功能点1]
- 实现了[功能点2]
- 包含完整类型定义
"
```

#### 6.3 代码评审
```
/review 代码评审
```

---

### 7️⃣ 集成阶段

#### 7.1 在页面中使用组件
导入并使用新组件

#### 7.2 联调测试（如涉及接口）
与后端进行接口联调

#### 7.3 更新待办状态
标记组件开发任务完成

---

## 快速开始（使用Skill）

如果不想手动执行每个步骤，可以使用自动化skill：

```
/dev-flow [组件名称]
```

该skill会自动执行上述大部分步骤。

---

## 常见问题

### Q1: 组件应该放在哪个目录？
- **业务组件**（项目特定）→ `components/business/`
- **通用组件**（可复用）→ `components/common/`
- **页面级组件**（仅某页面用）→ 直接放在页面目录下

### Q2: 何时需要单独的types.ts文件？
- 类型定义超过50行
- 多个文件需要共享类型
- 类型定义复杂（多层嵌套）

### Q3: 何时使用组合式函数？
- 逻辑需要在多个组件中复用
- 单个组件逻辑过于复杂（超过200行）
- 需要独立测试的逻辑

### Q4: 样式应该写在哪里？
- **简单组件**：直接写在`<style scoped>`中
- **复杂组件**：独立的`style.scss`文件
- **全局样式**：`assets/styles/`目录

---

## 参考资料

- 项目技术设计文档: `项目文档/02_设计文档/前端技术设计/前端技术设计文档.md`
- Vue 3 官方文档: https://cn.vuejs.org/
- Ant Design Vue 文档: https://antdv.com/
- TypeScript 手册: https://www.typescriptlang.org/

---

*流程版本: v1.0*
*最后更新: 2026年1月20日*
