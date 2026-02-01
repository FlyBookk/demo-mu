# GitHub 优秀 Agent 项目研究报告

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

## 🎯 重点推荐项目详解

### 1. MetaGPT ⭐ 61,919 - 最适合学习角色系统

**GitHub**: https://github.com/geekan/MetaGPT
**许可**: MIT
**语言**: Python

#### 核心架构

MetaGPT 模拟一个软件公司，包含多个角色协作完成项目：

```
用户需求 → 产品经理 → 架构师 → 工程师 → 测试工程师
           (PRD)      (设计文档)  (代码)    (测试报告)
```

#### 角色定义示例

```python
from metagpt.roles import Role
from metagpt.actions import Action

class ProductManager(Role):
    """产品经理角色"""
    def __init__(
        self,
        name: str = "Alice",
        profile: str = "Product Manager",
        goal: str = "高效地设计成功的产品",
        constraints: str = "遵循公司规范和预算限制"
    ):
        super().__init__(name, profile, goal, constraints)

        # 监听的消息类型
        self._watch([UserRequirement])

        # 执行的动作
        self.set_actions([WritePRD, WriteUserStories])

    async def _act(self) -> Message:
        """执行角色的主要动作"""
        # 1. 获取最新的用户需求
        requirement = self.rc.memory.get_by_action(UserRequirement)

        # 2. 分析需求并撰写 PRD
        prd = await WritePRD().run(requirement)

        # 3. 发布 PRD 消息
        return Message(content=prd, role=self.profile)

class Architect(Role):
    """架构师角色"""
    def __init__(
        self,
        name: str = "Bob",
        profile: str = "Architect",
        goal: str = "设计优雅、可扩展的系统架构"
    ):
        super().__init__(name, profile, goal)

        # 监听 PRD
        self._watch([WritePRD])

        # 执行设计动作
        self.set_actions([WriteDesign, WriteAPI])

    async def _act(self) -> Message:
        """设计系统架构"""
        prd = self.rc.memory.get_by_action(WritePRD)
        design = await WriteDesign().run(prd)
        return Message(content=design, role=self.profile)

class Engineer(Role):
    """工程师角色"""
    def __init__(
        self,
        name: str = "Charlie",
        profile: str = "Engineer",
        goal: str = "编写高质量、可维护的代码"
    ):
        super().__init__(name, profile, goal)

        # 监听设计文档
        self._watch([WriteDesign])

        # 执行编码动作
        self.set_actions([WriteCode, WriteTests])

    async def _act(self) -> Message:
        """编写代码"""
        design = self.rc.memory.get_by_action(WriteDesign)
        code = await WriteCode().run(design)
        return Message(content=code, role=self.profile)

class QAEngineer(Role):
    """测试工程师角色"""
    def __init__(
        self,
        name: str = "David",
        profile: str = "QA Engineer",
        goal: str = "确保软件质量"
    ):
        super().__init__(name, profile, goal)

        # 监听代码
        self._watch([WriteCode])

        # 执行测试动作
        self.set_actions([WriteTestCases, RunTests])
```

#### 团队协作示例

```python
from metagpt.team import Team

# 创建软件公司团队
company = Team()

# 添加角色
company.hire([
    ProductManager(),
    Architect(),
    Engineer(),
    QAEngineer()
])

# 投资（设置预算）
company.invest(investment=10.0)

# 启动项目
company.run_project(idea="开发一个 FBA 货件批量导入功能")
```

#### 可借鉴的设计模式

**1. 角色系统**
```python
class Role:
    name: str           # 角色名称
    profile: str        # 角色简介
    goal: str          # 角色目标
    constraints: str   # 约束条件
    actions: List[Action]  # 可执行的动作

    def _watch(self, actions: List[Type[Action]]):
        """监听特定类型的消息"""
        pass

    def set_actions(self, actions: List[Action]):
        """设置角色的动作序列"""
        pass

    async def _act(self) -> Message:
        """执行角色的主要逻辑"""
        pass
```

**2. 消息订阅机制**
```python
# 角色通过 _watch() 订阅感兴趣的消息
class Architect(Role):
    def __init__(self):
        self._watch([WritePRD])  # 只关注 PRD 消息

# 当 ProductManager 发布 PRD 时，Architect 自动被触发
```

**3. 动作链（Action Chain）**
```python
class Action:
    """动作基类"""
    async def run(self, context: str) -> str:
        """执行动作并返回结果"""
        pass

class WritePRD(Action):
    """撰写产品需求文档"""
    async def run(self, requirement: str) -> str:
        prompt = f"基于以下需求撰写 PRD:\n{requirement}"
        prd = await self.llm.aask(prompt)
        return prd

class WriteDesign(Action):
    """撰写设计文档"""
    async def run(self, prd: str) -> str:
        prompt = f"基于以下 PRD 设计系统架构:\n{prd}"
        design = await self.llm.aask(prompt)
        return design
```

#### 适用于慕声税务系统的设计

```python
# 借鉴 MetaGPT 设计税务系统的多角色 Agent

class TaxAnalyst(Role):
    """税务分析师 - 分析税务规则和计算逻辑"""
    def __init__(self):
        super().__init__(
            name="TaxAnalyst",
            profile="税务分析师",
            goal="准确分析税务计算规则，确保合规性",
            constraints="必须遵循最新的税法规定"
        )
        self._watch([TaxRequirement])
        self.set_actions([
            AnalyzeTaxRules,      # 分析税务规则
            GenerateTaxSpec,      # 生成税务规格
            ValidateTaxLogic      # 验证计算逻辑
        ])

class DataValidator(Role):
    """数据验证师 - 验证导入数据的准确性"""
    def __init__(self):
        super().__init__(
            name="DataValidator",
            profile="数据验证师",
            goal="确保导入数据 100% 准确",
            constraints="零容忍数据错误"
        )
        self._watch([ImportRequest])
        self.set_actions([
            ValidateDataFormat,   # 验证数据格式
            CheckBusinessRules,   # 检查业务规则
            GenerateValidationReport  # 生成验证报告
        ])

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
tax_team.run_project(idea="实现 FBA 货件批量导入功能")
```

---

### 2. LangChain ⭐ 122,850 - 最成熟的框架

**GitHub**: https://github.com/langchain-ai/langchain
**许可**: MIT
**语言**: Python/TypeScript

#### 核心架构

LangChain 提供完整的 Agent 开发工具链：

```
Tools → Agent → Memory → Callbacks
  ↓       ↓        ↓         ↓
执行工具  决策引擎  上下文    监控日志
```

#### Agent 类型

**1. ReAct Agent（推理+行动）**

```python
from langchain.agents import create_react_agent, AgentExecutor
from langchain.tools import Tool
from langchain_openai import ChatOpenAI

# 定义工具
def calculator(expression: str) -> str:
    """计算数学表达式"""
    try:
        return str(eval(expression))
    except Exception as e:
        return f"计算错误: {str(e)}"

def search(query: str) -> str:
    """搜索信息"""
    # 实际实现会调用搜索 API
    return f"搜索结果: {query}"

tools = [
    Tool(
        name="Calculator",
        func=calculator,
        description="用于数学计算。输入应该是数学表达式，如 '25 * 4'"
    ),
    Tool(
        name="Search",
        func=search,
        description="用于搜索信息。输入应该是搜索查询"
    )
]

# 创建 Agent
llm = ChatOpenAI(model="gpt-4", temperature=0)
agent = create_react_agent(llm, tools, prompt_template)

# 创建执行器
agent_executor = AgentExecutor(
    agent=agent,
    tools=tools,
    verbose=True,
    max_iterations=5
)

# 执行
result = agent_executor.invoke({
    "input": "计算 25 * 4 的结果，然后搜索这个数字的含义"
})
```

**Agent 的思考过程（ReAct 模式）**:
```
Thought: 我需要先计算 25 * 4
Action: Calculator
Action Input: 25 * 4
Observation: 100

Thought: 现在我需要搜索 100 的含义
Action: Search
Action Input: 100 的含义
Observation: 搜索结果: 100 是一个完美的数字...

Thought: 我现在知道最终答案了
Final Answer: 25 * 4 = 100，这是一个完美的数字...
```

**2. Structured Chat Agent（结构化对话）**

```python
from langchain.agents import create_structured_chat_agent

# 适合需要复杂输入的工具
tools = [
    Tool(
        name="TaxCalculator",
        func=calculate_tax,
        description="计算税额。需要输入: amount (金额), tax_rate (税率)"
    )
]

agent = create_structured_chat_agent(llm, tools, prompt)
```

#### Memory 系统

**1. 对话历史记忆**

```python
from langchain.memory import ConversationBufferMemory

memory = ConversationBufferMemory(
    memory_key="chat_history",
    return_messages=True
)

agent_executor = AgentExecutor(
    agent=agent,
    tools=tools,
    memory=memory  # 添加记忆
)

# 第一轮对话
agent_executor.invoke({"input": "我的名字是张三"})

# 第二轮对话（Agent 会记住之前的对话）
agent_executor.invoke({"input": "我的名字是什么？"})
# 输出: "您的名字是张三"
```

**2. 向量存储记忆（长期记忆）**

```python
from langchain.memory import VectorStoreRetrieverMemory
from langchain.vectorstores import Chroma
from langchain.embeddings import OpenAIEmbeddings

# 创建向量存储
vectorstore = Chroma(
    embedding_function=OpenAIEmbeddings()
)

# 创建记忆
memory = VectorStoreRetrieverMemory(
    retriever=vectorstore.as_retriever(search_kwargs={"k": 5})
)

# 保存记忆
memory.save_context(
    {"input": "FBA123 货件的数量是 1000"},
    {"output": "已记录"}
)

# 检索记忆
relevant_memories = memory.load_memory_variables(
    {"input": "FBA123 的数量是多少？"}
)
```

#### Callbacks 系统（监控和日志）

```python
from langchain.callbacks import StdOutCallbackHandler
from langchain.callbacks.base import BaseCallbackHandler

class CustomCallbackHandler(BaseCallbackHandler):
    """自定义回调处理器"""

    def on_llm_start(self, serialized, prompts, **kwargs):
        """LLM 开始时调用"""
        print(f"[LLM Start] Prompts: {prompts}")

    def on_llm_end(self, response, **kwargs):
        """LLM 结束时调用"""
        print(f"[LLM End] Response: {response}")

    def on_tool_start(self, serialized, input_str, **kwargs):
        """工具开始时调用"""
        print(f"[Tool Start] {serialized['name']}: {input_str}")

    def on_tool_end(self, output, **kwargs):
        """工具结束时调用"""
        print(f"[Tool End] Output: {output}")

    def on_agent_action(self, action, **kwargs):
        """Agent 执行动作时调用"""
        print(f"[Agent Action] {action.tool}: {action.tool_input}")

# 使用回调
agent_executor = AgentExecutor(
    agent=agent,
    tools=tools,
    callbacks=[CustomCallbackHandler()],
    verbose=True
)
```

#### 适用于慕声税务系统的设计

```python
# 为税务系统创建专用工具

from langchain.tools import Tool

# 1. 税务计算工具
def calculate_import_tax(params: dict) -> str:
    """计算进口税"""
    amount = params['amount']
    tax_rate = params['tax_rate']
    tax = amount * tax_rate
    return f"进口税: {tax:.2f} 元"

# 2. 数据验证工具
def validate_fba_data(data: dict) -> str:
    """验证 FBA 数据"""
    errors = []
    if data['quantity'] <= 0:
        errors.append("数量必须大于 0")
    if not data['shipment_id']:
        errors.append("货件编号不能为空")

    if errors:
        return f"验证失败: {', '.join(errors)}"
    return "验证通过"

# 3. 数据库查询工具
def query_shipment(shipment_id: str) -> str:
    """查询货件信息"""
    # 实际会查询数据库
    return f"货件 {shipment_id} 的详细信息..."

# 创建工具列表
tax_tools = [
    Tool(
        name="TaxCalculator",
        func=calculate_import_tax,
        description="计算进口税。输入格式: {'amount': 金额, 'tax_rate': 税率}"
    ),
    Tool(
        name="DataValidator",
        func=validate_fba_data,
        description="验证 FBA 数据。输入格式: {'shipment_id': '...', 'quantity': ...}"
    ),
    Tool(
        name="ShipmentQuery",
        func=query_shipment,
        description="查询货件信息。输入: 货件编号"
    )
]

# 创建税务 Agent
tax_agent = create_react_agent(llm, tax_tools, prompt)
tax_agent_executor = AgentExecutor(
    agent=tax_agent,
    tools=tax_tools,
    memory=ConversationBufferMemory(),
    callbacks=[CustomCallbackHandler()],
    verbose=True
)

# 使用示例
result = tax_agent_executor.invoke({
    "input": "验证货件 FBA123 的数据，如果通过则计算税额（金额 10000，税率 0.13）"
})
```

---

### 3. AutoGen (Microsoft) ⭐ 52,927 - 多 Agent 对话

**GitHub**: https://github.com/microsoft/autogen
**许可**: Apache-2.0
**语言**: Python

#### 核心架构

AutoGen 专注于多 Agent 之间的对话和协作：

```
Agent A ←→ Agent B ←→ Agent C
   ↓          ↓          ↓
 工具执行   代码生成   人类审查
```

#### 基础对话模式

```python
from autogen import AssistantAgent, UserProxyAgent

# 1. 创建助手 Agent
assistant = AssistantAgent(
    name="assistant",
    system_message="你是一个有帮助的 AI 助手",
    llm_config={
        "model": "gpt-4",
        "temperature": 0,
        "api_key": "your-api-key"
    }
)

# 2. 创建用户代理 Agent
user_proxy = UserProxyAgent(
    name="user_proxy",
    human_input_mode="TERMINATE",  # 模式: ALWAYS, TERMINATE, NEVER
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