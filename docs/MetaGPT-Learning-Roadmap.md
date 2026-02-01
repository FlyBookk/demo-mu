# MetaGPT 学习路线图

**基于已完成的文档**: `MetaGPT-Role-System-Deep-Dive.md` 和 `MetaGPT-Role-System-Part2.md`

---

## 🎯 学习路线（4 周计划）

### 第一周：理解核心概念

**目标**: 掌握 MetaGPT 的基础架构

**学习内容**:
1. 阅读第一部分文档（核心架构）
2. 理解 Role、Action、Message 的关系
3. 理解消息订阅机制

**实践任务**:
```python
# 任务 1: 创建一个简单的 Role
class SimpleRole(Role):
    def __init__(self):
        super().__init__(name="Simple", profile="Simple Role")
        self._init_actions([SimpleAction])

# 任务 2: 创建一个简单的 Action
class SimpleAction(Action):
    async def run(self, context: str) -> str:
        return f"处理: {context}"

# 任务 3: 测试消息订阅
role = SimpleRole()
role._watch([SimpleAction])
```

**检查点**:
- [ ] 能够解释 Role 的生命周期
- [ ] 能够解释消息如何在角色间传递
- [ ] 能够创建简单的 Role 和 Action

---

### 第二周：实践基础示例

**目标**: 运行和修改示例代码

**学习内容**:
1. 阅读第二部分文档（完整示例）
2. 运行"从零构建软件公司"示例
3. 理解执行流程

**实践任务**:
```bash
# 1. 安装 MetaGPT
pip install metagpt

# 2. 配置 API Key
export OPENAI_API_KEY="your-key"

# 3. 运行示例
python examples/software_company.py

# 4. 修改示例
# - 添加一个新角色（如 Tester）
# - 修改 Action 的提示词
# - 调整团队规模
```

**检查点**:
- [ ] 成功运行示例代码
- [ ] 能够添加新角色
- [ ] 能够修改 Action 行为
- [ ] 理解消息流转过程

---

### 第三周：设计税务系统 Agent

**目标**: 为慕声税务系统设计 Agent 架构

**学习内容**:
1. 阅读实战案例部分
2. 分析税务系统的需求
3. 设计角色体系

**实践任务**:

**任务 1: 设计角色体系**
```python
# 定义 5 个核心角色
roles = [
    "TaxAnalyst",      # 税务分析师
    "DataValidator",   # 数据验证师
    "CodeGenerator",   # 代码生成器
    "TestEngineer",    # 测试工程师
    "Reviewer"         # 代码审查员
]

# 为每个角色定义：
# - name, profile, goal, constraints
# - actions (可执行的动作)
# - watch (监听的消息类型)
```

**任务 2: 设计消息流**
```
TaxRequirement → TaxAnalyst → TaxSpec
                                  ↓
ImportRequest → DataValidator → ValidationReport
                                  ↓
TaxSpec + ValidationReport → CodeGenerator → Code
                                               ↓
Code → TestEngineer → TestReport
                        ↓
Code + TestReport → Reviewer → Approval
```

**任务 3: 定义 Actions**
```python
# 为每个角色定义具体的 Actions
class AnalyzeTaxRules(Action):
    """分析税务规则"""
    async def run(self, requirement: str) -> str:
        # 实现税务规则分析逻辑
        pass

class ValidateData(Action):
    """验证数据"""
    async def run(self, data: dict) -> str:
        # 实现数据验证逻辑
        pass

# ... 更多 Actions
```

**检查点**:
- [ ] 完成角色体系设计
- [ ] 绘制消息流程图
- [ ] 定义所有 Actions
- [ ] 编写设计文档

---

### 第四周：实现和测试

**目标**: 实现完整的 Agent 系统

**学习内容**:
1. 阅读调试技巧和性能优化部分
2. 实现核心角色
3. 测试和优化

**实践任务**:

**任务 1: 实现核心角色**
```python
# 实现 TaxAnalyst
class TaxAnalyst(Role):
    def __init__(self):
        super().__init__(
            name="TaxAnalyst",
            profile="税务分析师",
            goal="准确分析税务规则",
            constraints="必须遵循最新税法"
        )
        self._init_actions([AnalyzeTaxRules, GenerateTaxSpec])
        self._watch([TaxRequirement])

    async def _act(self) -> Message:
        # 实现具体逻辑
        pass

# 实现其他角色...
```

**任务 2: 创建团队并测试**
```python
# 创建团队
tax_team = Team()
tax_team.hire([
    TaxAnalyst(),
    DataValidator(),
    CodeGenerator(),
    TestEngineer(),
    Reviewer()
])

# 运行测试
result = await tax_team.run_project(
    idea="实现 FBA 货件批量导入功能"
)

# 验证结果
assert "税务规格" in result
assert "验证报告" in result
assert "代码实现" in result
```

**任务 3: 调试和优化**
```python
# 1. 启用详细日志
import logging
logging.basicConfig(level=logging.DEBUG)

# 2. 打印消息流
def print_message_flow(team: Team):
    for msg in team.env.memory.storage:
        print(f"{msg.role} → {msg.cause_by.__name__}")

# 3. 性能优化
# - 添加缓存
# - 限制记忆大小
# - 并行执行动作
```

**检查点**:
- [ ] 实现所有核心角色
- [ ] 通过基本测试
- [ ] 添加调试日志
- [ ] 完成性能优化
- [ ] 编写使用文档

---

## 📚 参考资源

### 官方资源
- **GitHub**: https://github.com/geekan/MetaGPT
- **文档**: https://docs.deepwisdom.ai/
- **示例**: https://github.com/geekan/MetaGPT/tree/main/examples

### 本地文档
- **核心架构**: `docs/MetaGPT-Role-System-Deep-Dive.md`
- **完整示例**: `docs/MetaGPT-Role-System-Part2.md`
- **Agent 研究**: `docs/Agent-Research-Summary.md`

### 相关配置
- **Constitution**: `.specify/memory/constitution.md`
- **AGENTS.md**: `AGENTS.md`
- **Spec-Kit**: `.specify/SETUP_COMPLETE.md`

---

## 🎓 进阶主题

### 1. 与 LangChain 集成
```python
# 使用 LangChain 的工具增强 MetaGPT
from langchain.tools import Tool
from metagpt.actions import Action

class LangChainAction(Action):
    def __init__(self, langchain_tool: Tool):
        super().__init__()
        self.tool = langchain_tool

    async def run(self, context: str) -> str:
        return self.tool.run(context)
```

### 2. 持久化和恢复
```python
# 保存团队状态
team.save_state("team_state.json")

# 恢复团队状态
team = Team.load_state("team_state.json")
team.resume()
```

### 3. 分布式部署
```python
# 使用 Ray 进行分布式部署
import ray

@ray.remote
class DistributedRole(Role):
    async def run(self, message: Message):
        return await super().run(message)
```

### 4. 监控和可观测性
```python
# 集成 Prometheus 监控
from prometheus_client import Counter, Histogram

message_counter = Counter('metagpt_messages', 'Total messages')
action_duration = Histogram('metagpt_action_duration', 'Action duration')

class MonitoredAction(Action):
    async def run(self, context: str) -> str:
        with action_duration.time():
            result = await super().run(context)
        message_counter.inc()
        return result
```

---

## 🚀 快速命令

### 查看文档
```bash
# 核心架构
cat docs/MetaGPT-Role-System-Deep-Dive.md

# 完整示例
cat docs/MetaGPT-Role-System-Part2.md

# 学习路线
cat docs/MetaGPT-Learning-Roadmap.md
```

### 运行示例
```bash
# 克隆 MetaGPT
git clone https://github.com/geekan/MetaGPT
cd MetaGPT

# 安装依赖
pip install -e .

# 运行示例
python examples/debate.py
python examples/software_company.py
```

### 创建你的第一个 Agent
```bash
# 创建项目目录
mkdir my-metagpt-agent
cd my-metagpt-agent

# 创建主文件
cat > main.py << 'EOF'
import asyncio
from metagpt.roles import Role
from metagpt.actions import Action
from metagpt.team import Team

# 你的代码...

if __name__ == "__main__":
    asyncio.run(main())
EOF

# 运行
python main.py
```

---

## ✅ 学习检查清单

### 基础知识
- [ ] 理解 Role 的生命周期
- [ ] 理解消息订阅机制
- [ ] 理解 Action 的执行流程
- [ ] 理解 Memory 的工作原理
- [ ] 理解 Team 的协作机制

### 实践能力
- [ ] 能够创建自定义 Role
- [ ] 能够创建自定义 Action
- [ ] 能够设计消息流
- [ ] 能够调试 Agent 系统
- [ ] 能够优化性能

### 高级技能
- [ ] 能够设计复杂的角色体系
- [ ] 能够实现动态角色切换
- [ ] 能够集成外部工具
- [ ] 能够实现人类参与
- [ ] 能够部署到生产环境

---

## 💡 常见问题

### Q1: MetaGPT 和 LangChain 有什么区别？
**A**:
- MetaGPT 专注于**多角色协作**，模拟软件公司
- LangChain 专注于**工具链**，提供通用的 Agent 框架
- 两者可以结合使用

### Q2: 如何控制 LLM 调用成本？
**A**:
- 使用 `team.invest()` 设置预算
- 添加缓存机制
- 使用更便宜的模型（如 GPT-3.5）
- 限制最大轮次

### Q3: 如何调试 Agent 不工作的问题？
**A**:
1. 启用详细日志
2. 打印消息流
3. 检查 _watch 配置
4. 验证 Action 的输出

### Q4: 如何让 Agent 更智能？
**A**:
- 优化提示词
- 添加更多上下文
- 使用更强的模型
- 实现多轮对话
- 添加自我评估

---

**创建时间**: 2026-02-01
**作者**: Claude Code
**项目**: 慕声报税管理系统
