# 代码评审流程

**适用场景**: 代码开发完成后，提交前的自我评审或团队评审

**评审目标**: 确保代码质量、可维护性、性能、安全性

---

## 1️⃣ 自我评审清单

在提交代码评审前，先进行自我检查：

### 代码规范
- [ ] 代码格式符合ESLint规范（运行`npm run lint`）
- [ ] 没有console.log等调试代码
- [ ] 没有注释掉的废弃代码
- [ ] 变量命名清晰、有意义（camelCase）
- [ ] 常量使用UPPER_SNAKE_CASE
- [ ] 组件名使用PascalCase
- [ ] 文件名符合规范

### TypeScript类型
- [ ] 所有变量都有明确类型
- [ ] Props定义完整
- [ ] Emits定义完整
- [ ] API返回值类型定义
- [ ] 没有使用any（除非必要）
- [ ] 类型导入正确（type导入用`import type`）

### Vue组件规范
- [ ] Props有默认值（可选的）
- [ ] Props有类型验证
- [ ] Emits事件命名符合规范（kebab-case）
- [ ] 样式使用scoped
- [ ] 组件结构清晰（template/script/style顺序）
- [ ] 响应式数据使用ref/reactive
- [ ] 计算属性使用computed
- [ ] 副作用使用watch

### 代码质量
- [ ] 函数单一职责
- [ ] 函数长度合理（<100行）
- [ ] 避免深层嵌套（<4层）
- [ ] 复杂逻辑有注释说明
- [ ] 公共代码已提取
- [ ] 魔法数字已定义为常量

### 错误处理
- [ ] API调用有try-catch或错误处理
- [ ] 表单验证完善
- [ ] 边界情况处理（空数组、null、undefined）
- [ ] 错误提示友好

### 性能优化
- [ ] 大型组件使用懒加载
- [ ] 长列表使用虚拟滚动
- [ ] 图片使用懒加载
- [ ] 防抖/节流应用于频繁操作
- [ ] 避免不必要的重渲染
- [ ] computed代替复杂表达式

### 安全性
- [ ] 没有XSS漏洞（用户输入需转义）
- [ ] 没有SQL注入（使用参数化查询）
- [ ] 敏感信息不暴露（如密码、token）
- [ ] API请求带Token认证

### 测试
- [ ] 核心逻辑有单元测试
- [ ] 功能已本地测试
- [ ] 跨浏览器兼容性测试
- [ ] 响应式布局测试

---

## 2️⃣ 代码评审步骤

### 步骤1: 运行自动化检查
```bash
# ESLint检查
npm run lint

# TypeScript类型检查
npm run type-check

# 单元测试
npm run test

# 构建测试
npm run build
```

### 步骤2: 自我评审
使用上面的自我评审清单逐项检查

### 步骤3: 准备评审材料
准备以下信息：
- **变更说明**: 本次修改了什么
- **影响范围**: 影响哪些模块
- **测试情况**: 测试结果如何
- **关键代码**: 需要重点关注的代码

### 步骤4: 提交评审
```bash
# 提交代码
git add .
git commit -m "feat: [功能描述]

- 添加了xxx功能
- 修复了xxx问题
- 优化了xxx性能

测试: ✅ 已通过本地测试
影响: 仅影响xxx模块
"

# 推送到远程
git push origin feature/xxx
```

### 步骤5: 创建评审请求（如使用PR）
在GitHub/GitLab创建Pull Request，填写：
- 标题：简明扼要的描述
- 描述：详细的变更说明
- 截图/录屏：UI变更需要提供
- 测试用例：如何验证

---

## 3️⃣ 评审重点检查项

### 组件设计
```
评审问题：
- 组件职责是否单一？
- 组件是否可复用？
- Props设计是否合理？
- 组件拆分是否合理？
```

示例：
```vue
<!-- ❌ 不好：组件职责不单一 -->
<UserProfile />  <!-- 包含了用户信息展示、编辑、权限管理等多个职责 -->

<!-- ✅ 好：职责单一 -->
<UserInfo />         <!-- 仅展示用户信息 -->
<UserEditForm />     <!-- 仅编辑用户信息 -->
<UserPermissions />  <!-- 仅管理权限 -->
```

### 状态管理
```
评审问题：
- 状态是否放在正确的位置？（组件内 vs Store）
- 状态更新逻辑是否清晰？
- 是否有不必要的状态？
```

示例：
```typescript
// ❌ 不好：局部状态放在Store中
const globalStore = {
  modalVisible: false  // 这应该是组件局部状态
}

// ✅ 好：局部状态放在组件中
const modalVisible = ref(false)

// ✅ 好：全局状态放在Store中
const authStore = {
  userInfo: { ... },  // 用户信息是全局状态
  token: '...'        // Token是全局状态
}
```

### API调用
```
评审问题：
- API调用是否正确封装？
- 错误处理是否完善？
- 加载状态是否处理？
- 是否有重复请求？
```

示例：
```typescript
// ❌ 不好：直接在组件中调用axios
const data = await axios.get('/api/users')

// ✅ 好：使用封装的API
const data = await getUserList({ page: 1, pageSize: 20 })

// ✅ 好：完善的错误处理
const loading = ref(false)
const error = ref<Error | null>(null)

try {
  loading.value = true
  const data = await getUserList(params)
  // 处理数据
} catch (err) {
  error.value = err as Error
  message.error('加载失败')
} finally {
  loading.value = false
}
```

### 性能问题
```
评审问题：
- 是否有性能瓶颈？
- 是否有不必要的计算？
- 是否有内存泄漏？
- 列表渲染是否有key？
```

示例：
```vue
<!-- ❌ 不好：没有key -->
<div v-for="item in list">{{ item.name }}</div>

<!-- ✅ 好：有唯一key -->
<div v-for="item in list" :key="item.id">{{ item.name }}</div>

<!-- ❌ 不好：每次渲染都创建新函数 -->
<button @click="() => handleClick(item)">Click</button>

<!-- ✅ 好：使用固定引用 -->
<button @click="handleClick(item.id)">Click</button>
```

### 样式问题
```
评审问题：
- 样式是否使用scoped？
- 是否有样式污染？
- 响应式是否正常？
- 是否使用了项目的样式变量？
```

示例：
```vue
<!-- ❌ 不好：没有scoped -->
<style>
.button {
  color: red;  /* 可能污染全局 */
}
</style>

<!-- ✅ 好：使用scoped -->
<style lang="scss" scoped>
.button {
  color: $primary-color;  /* 使用项目变量 */

  @include respond-to('md') {
    font-size: 14px;  /* 响应式 */
  }
}
</style>
```

---

## 4️⃣ 评审意见分级

### P0 - 必须修改（阻塞性问题）
- 功能缺陷
- 安全漏洞
- 严重性能问题
- 会导致系统崩溃的问题

### P1 - 强烈建议修改（重要问题）
- 代码规范严重违反
- 可维护性差
- 明显的性能问题
- 错误处理缺失

### P2 - 建议修改（一般问题）
- 代码可读性问题
- 轻微的性能优化
- 注释缺失
- 命名不规范

### P3 - 可选修改（优化建议）
- 代码优化建议
- 更好的实现方式
- 代码风格调整

---

## 5️⃣ 评审模板

### 评审意见模板
```markdown
## 代码评审意见

### 总体评价
- 代码质量: ⭐⭐⭐⭐☆ (4/5)
- 可维护性: ⭐⭐⭐⭐☆ (4/5)
- 代码规范: ⭐⭐⭐⭐⭐ (5/5)

### P0问题（必须修改）
1. [文件:行号] 问题描述
   - 影响: xxx
   - 建议: xxx

### P1问题（强烈建议）
1. [文件:行号] 问题描述
   - 建议: xxx

### P2问题（建议修改）
1. [文件:行号] 问题描述
   - 建议: xxx

### P3问题（可选优化）
1. [文件:行号] 优化建议
   - 建议: xxx

### 优点
- 优点1
- 优点2

### 结论
- [ ] 通过，可以合并
- [ ] 有条件通过，修改P0/P1问题后合并
- [ ] 不通过，需要重大修改
```

---

## 6️⃣ 快速评审命令

如果已配置评审skill：
```
/review 代码评审
```

---

## 参考资料

- ESLint规则: `.eslintrc.cjs`
- TypeScript配置: `tsconfig.json`
- 项目编码规范: `项目文档/02_设计文档/前端技术设计/前端技术设计文档.md`

---

*流程版本: v1.0*
*最后更新: 2026年1月20日*
