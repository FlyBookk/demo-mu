# GitHub 优秀 Agent 项目完整研究报告

**研究日期**: 2026-02-01
**目的**: 为慕声税务系统设计多职能 Agent 提供参考

---

## 📊 项目概览（按 GitHub Stars 排序）

| 排名 | 项目 | Stars | 语言 | 核心特点 | 适用场景 |
|------|------|-------|------|----------|----------|
| 1 | LangChain | 122,850⭐ | Python/TS | 最成熟的 LLM 框架 | 通用 Agent 开发 |
| 2 | MetaGPT | 61,919⭐ | Python | 多角色软件公司模拟 | 角色协作系统 |
| 3 | Langflow | 54,900⭐ | Python/TS | 可视化工作流 | 低代码 Agent 设计 |
| 4 | AutoGen | 52,927⭐ | Python | 多 Agent 对话系统 | 对话式协作 |
| 5 | LlamaIndex | 46,100⭐ | Python | RAG 和数据索引 | 知识库 Agent |
| 6 | CrewAI | 41,871⭐ | Python | 角色扮演 Agent | 任务编排 |
| 7 | Agno | 36,414⭐ | Python | 企业级 Agent 平台 | 大规模部署 |
| 8 | Haystack | 23,741⭐ | Python | 生产级 RAG 管道 | 搜索和问答 |
| 9 | Vercel AI SDK | 20,400⭐ | TypeScript | Web AI 应用 | 前端 Agent |
| 10 | Mastra | 19,021⭐ | TypeScript | TypeScript 原生 | 全栈 Agent |

---

## 🎯 详细项目分析

### 1. MetaGPT ⭐ 61,919 - 最适合学习角色系统

**GitHub**: https://github.com/geekan/MetaGPT
**许可**: MIT
**语言**: Python

#### 为什么推荐给你

MetaGPT 是学习**多角色协作系统**的最佳范例，它模拟了一个完整的软件公司，非常适合你设计税务系统的多职能 Agent。

#### 核心设计模式

**1. 角色（Role）系统**

```python
from metagpt.roles import Role
from metagpt.actions import Action
from metagpt.schema import Message

class Role:
    """角色基类"""
    def __init__(
        self,
        name: str,           # 角色名称
        profile: str,        # 角色简介
        goal: str,          # 角色目标
        constraints: str    # 约束条件
    ):
        self.name = name
        self.profile = profile
        self.goal = goal
        self.constraints = constraints
        self.actions = []
        self.rc = RoleContext()  # 角色上下文

    def _watch(self, actions: List[Type[Action]]):
        """订阅感兴趣的消息类型"""
        self.rc.watch = actions

    def set_actions(self, actions: List[Action]):
        """设置角色可执行的动作"""
        self.actions = actions

    async def _act(self) -> Message:
        """执行角色的主要逻辑（子类实现）"""
        raise NotImplementedError
```

**2. 消息订阅机制**

```python
# 产品经理监听用户需求
class ProductManager(Role):
    def __init__(self):
        super().__init__(
            name="Alice",
            profile="Product Manager",
            goal="设计成功的产品"
        )
        self._watch([UserRequirement])  # 只关注用户需求消息

# 架构师监听 PRD
class Architect(Role):
    def __init__(self):
        super().__init__(
            name="Bob",
            profile="Architect",
            goal="设计优雅的架构"
        )
        self._watch([WritePRD])  # 只关注 PRD 消息

# 工程师监听设计文档
class Engineer(Role):
    def __init__(self):
        super().__init__(
            name="Charlie",
            profile="Engineer",
            goal="编写高质量代码"
        )
        self._watch([WriteDesign])  # 只关注设计文档
```

**3. 动作（Action）链**

```python
class Action:
    """动作基类"""
    async def run(self, context: str) -> str:
        """执行动作并返回结果"""
        raise NotImplementedError

class WritePRD(Action):
    """撰写产品需求文档"""
    async def run(self, requirement: str) -> str:
        prompt = f"""
        基于以下用户需求撰写详细的 PRD：
        {requirement}

        PRD 应包含：
        1. 功能概述
        2. 用户故事
        3. 功能需求
        4. 非功能需求
        5. 验收标准
        """
        prd = await self.llm.aask(prompt)
        return prd

class WriteDesign(Action):
    """撰写系统设计文档"""
    async def run(self, prd: str) -> str:
        prompt = f"""
        基于以下 PRD 设计系统架构：
        {prd}

        设计文档应包含：
        1. 系统架构图
        2. 数据模型
        3. API 设计
        4. 技术选型
        5. 部署方案
        """
        design = await self.llm.aask(prompt)
        return design

class WriteCode(Action):
    """编写代码"""
    async def run(self, design: str) -> str:
        prompt = f"""
        基于以下设计文档编写代码：
        {design}

        要求：
        1. 遵循最佳实践
        2. 包含单元测试
        3. 添加必要注释
        4. 符合代码规范
        """
        code = await self.llm.aask(prompt)
        return code
```

**4. 团队协作**

```python
from metagpt.team import Team

# 创建软件公司
company = Team()

# 招聘团队成员
company.hire([
    ProductManager(),
    Architect(),
    Engineer(),
    QAEngineer()
])

# 设置预算（控制 LLM 调用成本）
company.invest(investment=10.0)

# 启动项目
await company.run_project(
    idea="开发一个 FBA 货件批量导入功能"
)
```

**执行流程**:
```
1. 用户提出需求 → UserRequirement 消息
2. ProductManager 监听到 → 执行 WritePRD → 发布 PRD 消息
3. Architect 监听到 PRD → 执行 WriteDesign → 发布 Design 消息
4. Engineer 监听到 Design → 执行 WriteCode → 发布 Code 消息
5. QAEngineer 监听到 Code → 执行 WriteTests → 发布 TestReport 消息
```

#### 适用于慕声税务系统的设计

```python
# 为税务系统设计专用角色

class TaxAnalyst(Role):
    """税务分析师 - 分析税务规则"""
    def __init__(self):
        super().__init__(
            name="TaxAnalyst",
            profile="税务分析师",
            goal="准确分析税务计算规则，确保合规性",
            constraints="必须遵循最新税法，零容忍错误"
        )
        self._watch([TaxRequirement])
        self.set_actions([
            AnalyzeTaxRules,      # 分析税务规则
            GenerateTaxSpec,      # 生成税务规格
            ValidateTaxLogic      # 验证计算逻辑
        ])

    async def _act(self) -> Message:
        """分析税务需求并生成规格"""
        requirement = self.rc.memory.get_by_action(TaxRequirement)

        # 1. 分析税务规则
        rules = await AnalyzeTaxRules().run(requirement)

        # 2. 生成税务规格
        spec = await GenerateTaxSpec().run(rules)

        # 3. 验证逻辑正确性
        validation = await ValidateTaxLogic().run(spec)

        return Message(
            content=spec,
            role=self.profile,
            cause_by=GenerateTaxSpec
        )

class DataValidator(Role):
    """数据验证师 - 验证导入数据"""
    def __init__(self):
        super().__init__(
            name="DataValidator",
            profile="数据验证师",
            goal="确保导入数据 100% 准确",
            constraints="零容忍数据错误，所有异常必须报告"
        )
        self._watch([ImportRequest])
        self.set_actions([
            ValidateDataFormat,   # 验证数据格式
            CheckBusinessRules,   # 检查业务规则
            GenerateValidationReport  # 生成验证报告
        ])

    async def _act(self) -> Message:
        """验证导入数据"""
        import_data = self.rc.memory.get_by_action(ImportRequest)

        # 1. 验证数据格式
        format_result = await ValidateDataFormat().run(import_data)

        # 2. 检查业务规则
        business_result = await CheckBusinessRules().run(import_data)

        # 3. 生成验证报告
        report = await GenerateValidationReport().run({
            'format': format_result,
            'business': business_result
        })

        return Message(
            content=report,
            role=self.profile,
            cause_by=GenerateValidationReport
        )

class CodeGenerator(Role):
    """代码生成器 - 生成高质量代码"""
    def __init__(self):
        super().__init__(
            name="CodeGenerator",
            profile="代码生成器",
            goal="生成符合规范的高质量代码",
            constraints="必须遵循 TDD 和 Constitution"
        )
        self._watch([TaxSpec, ValidationReport])
        self.set_actions([
            WriteTests,           # 先写测试
            GenerateCode,         # 生成代码
            RefactorCode          # 重构优化
        ])

    async def _act(self) -> Message:
        """生成代码（TDD 流程）"""
        spec = self.rc.memory.get_by_action(TaxSpec)

        # 1. 先写测试
        tests = await WriteTests().run(spec)

        # 2. 生成代码
        code = await GenerateCode().run(spec)

        # 3. 重构优化
        refactored_code = await RefactorCode().run(code)

        return Message(
            content=refactored_code,
            role=self.profile,
            cause_by=GenerateCode
        )

class TestEngineer(Role):
    """测试工程师 - 确保代码质量"""
    def __init__(self):
        super().__init__(
            name="TestEngineer",
            profile="测试工程师",
            goal="确保代码通过所有测试",
            constraints="测试覆盖率 ≥ 90%"
        )
        self._watch([GenerateCode])
        self.set_actions([
            RunUnitTests,         # 运行单元测试
            RunIntegrationTests,  # 运行集成测试
            GenerateTestReport    # 生成测试报告
        ])

class Reviewer(Role):
    """代码审查员 - 审查代码质量"""
    def __init__(self):
        super().__init__(
            name="Reviewer",
            profile="代码审查员",
            goal="确保代码符合 Constitution 原则",
            constraints="严格执行代码审查标准"
        )
        self._watch([GenerateCode, TestReport])
        self.set_actions([
            ReviewCode,           # 审查代码
            CheckConstitution,    # 检查宪章合规性
            ApproveOrReject       # 批准或拒绝
        ])

# 创建税务系统开发团队
tax_team = Team()
tax_team.hire([
    TaxAnalyst(),
    DataValidator(),
    CodeGenerator(),
    TestEngineer(),
    Reviewer()
])

# 启动项目
await tax_team.run_project(
    idea="实现 FBA 货件批量导入功能，包含数据验证和税务计算"
)
```

---

### 2. LangChain ⭐ 122,850 - 最成熟的框架

**GitHub**: https://github.com/langchain-ai/langchain
**许可**: MIT
**语言**: Python/TypeScript

#### 核心组件

**1. Tools（工具）**

```python
from langchain.tools import Tool, StructuredTool
from pydantic import BaseModel, Field

# 简单工具定义
def calculator(expression: str) -> str:
    """计算数学表达式"""
    try:
        return str(eval(expression))
    except Exception as e:
        return f"计算错误: {str(e)}"

simple_tool = Tool(
    name="Calculator",
    func=calculator,
    description="用于数学计算。输入应该是数学表达式，如 '25 * 4'"
)

# 结构化工具定义（推荐）
class TaxCalculatorInput(BaseModel):
    """税务计算器输入"""
    amount: float = Field(description="金额")
    tax_rate: float = Field(description="税率，如 0.13 表示 13%")

def calculate_tax(amount: float, tax_rate: float) -> str:
    """计算税额"""
    tax = amount * tax_rate
    return f"税额: {tax:.2f} 元"

structured_tool = StructuredTool.from_function(
    func=calculate_tax,
    name="TaxCalculator",
    description="计算税额",
    args_schema=TaxCalculatorInput
)
```

**2. Agents（代理）**

```python
from langchain.agents import create_react_agent, AgentExecutor
from langchain_openai import ChatOpenAI

# 创建 ReAct Agent（推理+行动）
llm = ChatOpenAI(model="gpt-4", temperature=0)

agent = create_react_agent(
    llm=llm,
    tools=[simple_tool, structured_tool],
    prompt=prompt_template
)

agent_executor = AgentExecutor(
    agent=agent,
    tools=[simple_tool, structured_tool],
    verbose=True,
    max_iterations=5,
    handle_parsing_errors=True
)

# 执行
result = agent_executor.invoke({
    "input": "计算 10000 元的进口税，税率 13%"
})
```

**Agent 思考过程（ReAct 模式）**:
```
Thought: 我需要使用税务计算器来计算税额
Action: TaxCalculator
Action Input: {"amount": 10000, "tax_rate": 0.13}
Observation: 税额: 1300.00 元

Thought: 我现在知道最终答案了
Final Answer: 10000 元的进口税（税率 13%）为 1300.00 元
```

**3. Memory（记忆）**

```python
from langchain.memory import ConversationBufferMemory, ConversationSummaryMemory

# 完整对话历史记忆
buffer_memory = ConversationBufferMemory(
    memory_key="chat_history",
    return_messages=True
)

# 摘要记忆（节省 token）
summary_memory = ConversationSummaryMemory(
    llm=llm,
    memory_key="chat_history"
)

# 使用记忆
agent_executor = AgentExecutor(
    agent=agent,
    tools=tools,
    memory=buffer_memory
)

# 第一轮对话
agent_executor.invoke({"input": "我的货件编号是 FBA123"})

# 第二轮对话（Agent 会记住之前的信息）
agent_executor.invoke({"input": "这个货件的数量是多少？"})
# Agent 会记住 FBA123 并查询
```

**4. Callbacks（回调）**

```python
from langchain.callbacks.base import BaseCallbackHandler

class CustomCallbackHandler(BaseCallbackHandler):
    """自定义回调处理器 - 用于日志和监控"""

    def on_llm_start(self, serialized, prompts, **kwargs):
        """LLM 开始时"""
        print(f"[LLM Start] Model: {serialized.get('name')}")
        print(f"[LLM Start] Prompt: {prompts[0][:100]}...")

    def on_llm_end(self, response, **kwargs):
        """LLM 结束时"""
        print(f"[LLM End] Response: {response.generations[0][0].text[:100]}...")

    def on_tool_start(self, serialized, input_str, **kwargs):
        """工具开始时"""
        tool_name = serialized.get('name', 'Unknown')
        print(f"[Tool Start] {tool_name}: {input_str}")

    def on_tool_end(self, output, **kwargs):
        """工具结束时"""
        print(f"[Tool End] Output: {output}")

    def on_agent_action(self, action, **kwargs):
        """Agent 执行动作时"""
        print(f"[Agent Action] Tool: {action.tool}")
        print(f"[Agent Action] Input: {action.tool_input}")

    def on_agent_finish(self, finish, **kwargs):
        """Agent 完成时"""
        print(f"[Agent Finish] Output: {finish.return_values}")

# 使用回调
agent_executor = AgentExecutor(
    agent=agent,
    tools=tools,
    callbacks=[CustomCallbackHandler()],
    verbose=True
)
```

#### 适用于税务系统的工具设计

```python
from langchain.tools import StructuredTool
from pydantic import BaseModel, Field
from typing import Dict, List

# 1. 税务计算工具
class TaxCalculationInput(BaseModel):
    amount: float = Field(description="金额（元）")
    tax_rate: float = Field(description="税率（如 0.13）")
    tax_type: str = Field(description="税种：import（进口税）或 vat（增值税）")

def calculate_tax(amount: float, tax_rate: float, tax_type: str) -> str:
    """计算税额"""
    tax = amount * tax_rate
    return f"{tax_type} 税额: {tax:.2f} 元（金额: {amount}, 税率: {tax_rate*100}%）"

tax_calculator = StructuredTool.from_function(
    func=calculate_tax,
    name="TaxCalculator",
    description="计算税额。支持进口税和增值税计算。",
    args_schema=TaxCalculationInput
)

# 2. 数据验证工具
class DataValidationInput(BaseModel):
    shipment_id: str = Field(description="货件编号")
    quantity: int = Field(description="数量")
    ship_date: str = Field(description="发货日期 YYYY-MM-DD")

def validate_fba_data(shipment_id: str, quantity: int, ship_date: str) -> str:
    """验证 FBA 数据"""
    errors = []

    if not shipment_id or len(shipment_id) < 3:
        errors.append("货件编号无效")

    if quantity <= 0:
        errors.append("数量必须大于 0")

    try:
        from datetime import datetime
        datetime.strptime(ship_date, "%Y-%m-%d")
    except:
        errors.append("日期格式错误，应为 YYYY-MM-DD")

    if errors:
        return f"验证失败: {', '.join(errors)}"
    return f"验证通过: 货件 {shipment_id}, 数量 {quantity}, 日期 {ship_date}"

data_validator = StructuredTool.from_function(
    func=validate_fba_data,
    name="DataValidator",
    description="验证 FBA 货件数据的格式和业务规则",
    args_schema=DataValidationInput
)

# 3. 数据库查询工具
class ShipmentQueryInput(BaseModel):
    shipment_id: str = Field(description="货件编号")

def query_shipment(shipment_id: str) -> str:
    """查询货件信息"""
    # 实际实现会查询数据库
    # 这里是模拟数据
    return f"""
    货件信息:
    - 编号: {shipment_id}
    - 数量: 1000
    - 发货日期: 2026-01-15
    - 目的地: 美国
    - 状态: 已发货
    """

shipment_query = StructuredTool.from_function(
    func=query_shipment,
    name="ShipmentQuery",
    description="查询货件的详细信息",
    args_schema=ShipmentQueryInput
)

# 4. 创建税务 Agent
tax_tools = [tax_calculator, data_validator, shipment_query]

tax_agent = create_react_agent(llm, tax_tools, prompt_template)

tax_agent_executor = AgentExecutor(
    agent=tax_agent,
    tools=tax_tools,
    memory=ConversationBufferMemory(memory_key="chat_history", return_messages=True),
    callbacks=[CustomCallbackHandler()],
    verbose=True,
    max_iterations=10
)

# 使用示例
result = tax_agent_executor.invoke({
    "input": """
    请帮我处理货件 FBA123：
    1. 查询货件信息
    2. 验证数据是否正确
    3. 如果验证通过，计算进口税（税率 13%）
    """
})
```

---

### 3. AutoGen (Microsoft) ⭐ 52,927 - 多 Agent 对话

**GitHub**: https://github.com/microsoft/autogen
**许可**: Apache-2.0
**语言**: Python

#### 核心特点

AutoGen 专注于**多 Agent 对话和协作**，支持：
- 人类参与（Human-in-the-loop）
- 代码执行和验证
- 群聊模式
- 灵活的终止条件

#### 基础对话模式

```python
from autogen import AssistantAgent, UserProxyAgent

# 1. 创建助手 Agent
assistant = AssistantAgent(
    name="assistant",
    system_message="你是一个有帮助的 AI 助手，擅长编写代码",
    llm_config={
        "model": "gpt-4",
        "temperature": 0,
        "api_key": "your-api-key"
    }
)

# 2. 创建用户代理 Agent
user_proxy = UserProxyAgent(
    name="user_proxy",
    human_input_mode="TERMINATE",  # ALWAYS, TERMINATE, NEVER
    max_consecutive_auto_reply=10,
    is_termination_msg=lambda x: x.get("content", "").rstrip().endswith("TERMINATE"),
    code_execution_config={
        "work_dir": "coding",
        "use_docker": False
    }
)

# 3. 启动对话
user_proxy.initiate_chat(
    assistant,
    message="帮我写一个计算斐波那契数列的 Python 函数"
)
```

**对话流程**:
```
User Proxy: 帮我写一个计算斐波那契数列的 Python 函数