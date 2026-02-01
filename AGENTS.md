# AGENTS.md - AI 助手使用指南

> **这是一个"机器人的 README"** - 告诉 AI 助手如何理解和使用本项目。

## 项目概览

**项目名称**: 慕声报税管理系统 (Musheng Tax System)
**项目类型**: 亚马逊 FBA 转口贸易报税管理系统
**架构**: 前后端分离（Vue 3 + Spring Boot 3）

## 核心原则（必读）

在开始任何工作前，**必须阅读**：

1. **Constitution**: `.specify/memory/constitution.md` - 项目宪章，定义了不可违反的开发原则
2. **Superpowers 工作流程**: `.cursorrules` - 日常开发的工作流程规范

### 关键原则摘要

- ✅ **测试驱动开发（TDD）是强制性的** - 没有失败的测试就不能写代码
- ✅ **数据准确性优先** - 税务计算必须 100% 准确
- ✅ **前后端契约** - API 变更必须先更新文档
- ✅ **审计可追溯** - 关键操作必须记录日志
- ✅ **简洁性（YAGNI）** - 只构建当前需要的功能

## 项目结构

```
musheng/
├── musheng-tax-web/              # 前端 Vue 3 项目
│   ├── src/
│   │   ├── api/                  # API 调用封装
│   │   ├── components/           # 可复用组件
│   │   ├── views/                # 页面视图
│   │   ├── router/               # 路由配置
│   │   ├── stores/               # Pinia 状态管理
│   │   └── utils/                # 工具函数
│   └── tests/                    # 前端测试
│
├── musheng-tax-system/           # 后端 Spring Boot 项目
│   ├── musheng-common/           # 公共模块（工具类、常量）
│   ├── musheng-system/           # 系统模块（用户、权限）
│   ├── musheng-business/         # 业务模块（FBA 货件、税务计算）
│   ├── musheng-config/           # 配置模块
│   └── musheng-web/              # Web 入口模块
│
├── .specify/                     # Spec-Kit 规格管理
│   ├── memory/
│   │   └── constitution.md       # 项目宪章（必读）
│   ├── specs/
│   │   ├── active/               # 活跃的功能规格
│   │   └── archive/              # 已完成的规格
│   ├── scripts/                  # 自动化脚本
│   └── templates/                # 规格模板
│
├── .cursor/superpowers/          # Superpowers 技能
├── .cursorrules                  # Superpowers 工作流程（必读）
└── docs/                         # 项目文档
```

## 技术栈

### 前端
- **框架**: Vue 3.4 + TypeScript
- **构建**: Vite 5.2
- **UI**: Ant Design Vue 4.1
- **状态**: Pinia
- **路由**: Vue Router 4
- **HTTP**: Axios
- **表格**: VXE-Table
- **图表**: ECharts

### 后端
- **语言**: Java 17
- **框架**: Spring Boot 3.2.2
- **ORM**: MyBatis-Plus 3.5.5
- **认证**: Sa-Token 1.37.0
- **数据库**: MySQL 8.0
- **文档**: Knife4j 4.3.0 (Swagger)
- **Excel**: EasyExcel 3.3.3
- **工具**: Hutool

## 开发工作流程

### 方式一：使用 Spec-Kit（重要功能）

适用于：税务计算、数据迁移、架构变更等重要功能。

```bash
# 1. 创建功能规格目录
mkdir -p .specify/specs/active/[编号]-[功能名称]

# 2. 填写规格文档（基于模板）
# - spec.md: 用户场景、需求、成功标准
# - plan.md: 技术方案、API 契约
# - tasks.md: 任务清单

# 3. 审查规格（团队 Review）

# 4. 实施（AI 助手按照规格执行）
```

### 方式二：使用 Superpowers（日常开发）

适用于：Bug 修复、小功能添加、代码重构。

AI 助手会自动判断并执行相应工作流程：
- **Brainstorming**: 新功能设计
- **TDD**: 测试驱动开发
- **Systematic Debugging**: 系统化调试
- **Writing Plans**: 多步骤任务规划

## AI 助手工作指南

### 开始任何任务前

1. **检查是否需要规格文档**
   - 重要功能？→ 先创建 Spec-Kit 规格
   - 日常开发？→ 直接使用 Superpowers 流程

2. **阅读相关文档**
   - Constitution（如果涉及核心原则）
   - 现有规格文档（如果功能相关）
   - API 文档（如果涉及接口变更）

3. **确认测试策略**
   - 税务计算？→ 必须有单元测试 + 集成测试
   - API 变更？→ 必须有契约测试
   - 数据操作？→ 必须有数据一致性测试

### 编写代码时

#### 前端代码规范
```typescript
// ✅ 好的例子：使用 Composition API
<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const loading = ref(false)

const handleSubmit = async () => {
  loading.value = true
  try {
    // 业务逻辑
  } finally {
    loading.value = false
  }
}
</script>

// ❌ 避免：Options API
export default {
  data() {
    return { loading: false }
  }
}
```

#### 后端代码规范
```java
// ✅ 好的例子：使用 MyBatis-Plus + 统一响应
@RestController
@RequestMapping("/api/fba-shipment")
@Tag(name = "FBA货件管理")
public class FbaShipmentController {

    @GetMapping("/list")
    @Operation(summary = "查询货件列表")
    public Result<Page<FbaShipmentVO>> list(FbaShipmentQuery query) {
        Page<FbaShipmentVO> page = fbaShipmentService.queryPage(query);
        return Result.success(page);
    }
}

// ❌ 避免：直接返回实体类，缺少文档注解
@GetMapping("/list")
public List<FbaShipment> list() {
    return fbaShipmentService.list();
}
```

### 测试编写指南

#### 前端测试（Vitest）
```typescript
// ✅ 测试用户交互和业务逻辑
import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import FbaShipmentForm from '@/components/FbaShipmentForm.vue'

describe('FbaShipmentForm', () => {
  it('should validate required fields', async () => {
    const wrapper = mount(FbaShipmentForm)
    await wrapper.find('button[type="submit"]').trigger('click')
    expect(wrapper.text()).toContain('请输入货件编号')
  })
})
```

#### 后端测试（JUnit 5 + Spring Boot Test）
```java
// ✅ 测试业务逻辑和数据准确性
@SpringBootTest
class TaxCalculationServiceTest {

    @Autowired
    private TaxCalculationService taxCalculationService;

    @Test
    @DisplayName("计算进口税 - 正常情况")
    void testCalculateImportTax_Normal() {
        // Given
        BigDecimal amount = new BigDecimal("1000.00");
        BigDecimal taxRate = new BigDecimal("0.13");

        // When
        BigDecimal tax = taxCalculationService.calculateImportTax(amount, taxRate);

        // Then
        assertEquals(new BigDecimal("130.00"), tax);
    }

    @Test
    @DisplayName("计算进口税 - 边界条件：零值")
    void testCalculateImportTax_ZeroAmount() {
        BigDecimal tax = taxCalculationService.calculateImportTax(
            BigDecimal.ZERO, new BigDecimal("0.13")
        );
        assertEquals(BigDecimal.ZERO, tax);
    }
}
```

### API 变更流程

1. **更新 Knife4j 注解**
```java
@Operation(summary = "新增货件", description = "创建新的FBA货件记录")
@Parameters({
    @Parameter(name = "shipmentId", description = "货件编号", required = true),
    @Parameter(name = "quantity", description = "数量", required = true)
})
```

2. **更新前端 API 调用**
```typescript
// src/api/fba-shipment.ts
export interface CreateShipmentRequest {
  shipmentId: string
  quantity: number
}

export const createShipment = (data: CreateShipmentRequest) => {
  return request.post<Result<void>>('/api/fba-shipment', data)
}
```

3. **编写契约测试**
```java
@Test
void testCreateShipment_ApiContract() {
    mockMvc.perform(post("/api/fba-shipment")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"shipmentId\":\"FBA123\",\"quantity\":100}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200));
}
```

## 常见场景处理

### 场景 1：添加新功能
```
用户说："我想添加货件批量导入功能"

AI 助手应该：
1. 判断是否需要 Spec-Kit 规格（批量导入是重要功能 → 需要）
2. 创建规格目录：.specify/specs/active/003-batch-import/
3. 引导用户填写 spec.md（用户场景、需求）
4. 生成 plan.md（技术方案、API 设计）
5. 生成 tasks.md（任务清单）
6. 执行 TDD 实施
```

### 场景 2：修复 Bug
```
用户说："税额计算结果不对"

AI 助手应该：
1. 启动 Systematic Debugging 流程
2. 阶段 1：根本原因调查（读取错误信息、检查测试用例）
3. 阶段 2：模式分析（查找类似的正确实现）
4. 阶段 3：假设和测试（写失败的测试用例）
5. 阶段 4：实施修复（最小化修改）
```

### 场景 3：重构代码
```
用户说："重构货件查询逻辑"

AI 助手应该：
1. 先确保有测试覆盖（如果没有，先写测试）
2. 运行测试确保全部通过（绿色状态）
3. 进行重构（保持测试绿色）
4. 提交代码
```

## 禁止事项

❌ **绝对不要做的事情：**

1. 在没有失败测试的情况下写生产代码
2. 修改税务计算逻辑而不添加测试
3. 直接修改数据库而不写迁移脚本
4. 跳过 API 文档更新
5. 使用字符串拼接构建 SQL
6. 在代码中硬编码敏感信息
7. 违反 Constitution 中的核心原则

## 获取帮助

### 查看文档
```bash
# 查看 Constitution
cat .specify/memory/constitution.md

# 查看 Superpowers 工作流程
cat .cursorrules

# 查看规格模板
ls .specify/templates/

# 查看现有规格
ls .specify/specs/active/
```

### 使用技能命令
```bash
/workflow  # 查看工作流程规范
/todo      # 管理任务
/review    # 启动评审流程
```

## 记忆文件

项目有以下记忆文件可供参考：
- `project_overview` - 项目概览
- `suggested_commands` - 常用命令
- `code_style` - 代码风格
- `task_completion` - 任务完成记录

使用 Serena 工具读取：
```
read_memory("project_overview")
```

## 版本信息

- **Constitution 版本**: 1.0.0
- **最后更新**: 2026-02-01
- **维护者**: 慕声开发团队

---

**记住**: 这个项目是税务系统，数据准确性是生命线。当有疑问时，优先选择更安全、更可测试的方案。
