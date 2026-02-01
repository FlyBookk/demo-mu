# MetaGPT 角色系统深度解析

**创建日期**: 2026-02-01
**目的**: 深入理解 MetaGPT 的角色系统实现原理

---

## 📚 目录

1. [核心架构概览](#核心架构概览)
2. [Role 基类设计](#role-基类设计)
3. [消息订阅机制](#消息订阅机制)
4. [Action 动作系统](#action-动作系统)
5. [Memory 记忆管理](#memory-记忆管理)
6. [Team 团队协作](#team-团队协作)
7. [完整示例](#完整示例)
8. [源码分析](#源码分析)

---

## 核心架构概览

MetaGPT 的角色系统基于以下核心概念：

```
┌─────────────────────────────────────────────────────────────┐
│                        Team (团队)                           │
│  ┌──────────────────────────────────────────────────────┐  │
│  │                   Environment (环境)                  │  │
│  │  ┌────────────────────────────────────────────────┐  │  │
│  │  │              Message Queue (消息队列)           │  │  │
│  │  └────────────────────────────────────────────────┘  │  │
│  │                                                        │  │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐           │  │
│  │  │  Role A  │  │  Role B  │  │  Role C  │           │  │
│  │  │          │  │          │  │          │           │  │
│  │  │ Actions  │  │ Actions  │  │ Actions  │           │  │
│  │  │ Memory   │  │ Memory   │  │ Memory   │           │  │
│  │  └──────────┘  └──────────┘  └──────────┘           │  │
│  └────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### 核心组件

1. **Role（角色）**: 具有特定职责的智能体
2. **Action（动作）**: 角色可以执行的具体操作
3. **Message（消息）**: 角色间通信的载体
4. **Memory（记忆）**: 角色的上下文和历史
5. **Environment（环境）**: 角色运行的容器
6. **Team（团队）**: 多个角色的协作单元

---

## Role 基类设计

### 类定义

```python
from typing import Iterable, Type
from pydantic import BaseModel, Field

class Role(BaseModel):
    """角色基类 - MetaGPT 的核心抽象"""

    # 基本属性
    name: str = ""                    # 角色名称
    profile: str = ""                 # 角色简介/职位
    goal: str = ""                    # 角色目标
    constraints: str = ""             # 约束条件
    desc: str = ""                    # 详细描述

    # 运行时属性
    is_human: bool = False            # 是否是人类角色
    recovered: bool = False           # 是否从持久化恢复
    rc: RoleContext = Field(default_factory=RoleContext)  # 角色上下文

    # 动作相关
    actions: list[Type[Action]] = Field(default_factory=list)  # 可执行的动作
    states: list[str] = Field(default_factory=list)            # 状态列表

    # LLM 配置
    llm: BaseLLM = Field(default_factory=LLM)  # 语言模型

    def __init__(self, **data):
        """初始化角色"""
        super().__init__(**data)
        self._init_actions()
        self._watch(self.actions)

    def _init_actions(self):
        """初始化动作列表"""
        self.actions = []

    def _watch(self, actions: Iterable[Type[Action]]):
        """订阅感兴趣的动作类型"""
        self.rc.watch = {any_to_str(t) for t in actions}

    def set_actions(self, actions: Iterable[Type[Action]]):
        """设置角色的动作序列"""
        self.actions = actions
        self._init_actions()

    async def _think(self) -> Action:
        """思考：决定下一步执行哪个动作"""
        if len(self.actions) == 1:
            # 只有一个动作，直接返回
            return self.actions[0]

        # 多个动作时，根据当前状态选择
        # 这里可以实现复杂的决策逻辑
        prompt = self._get_think_prompt()
        rsp = await self.llm.aask(prompt)
        # 解析响应，选择动作
        return self._parse_action(rsp)

    async def _act(self) -> Message:
        """执行：执行选定的动作"""
        # 1. 思考：选择动作
        action = await self._think()

        # 2. 获取上下文
        context = self.rc.memory.get_by_actions(self.rc.watch)

        # 3. 执行动作
        result = await action.run(context)

        # 4. 创建消息
        msg = Message(
            content=result,
            role=self.profile,
            cause_by=type(action),
            sent_from=self
        )

        # 5. 保存到记忆
        self.rc.memory.add(msg)

        return msg

    async def _observe(self) -> int:
        """观察：检查是否有新消息"""
        # 从环境中获取新消息
        news = self.rc.env.memory.get_by_actions(self.rc.watch)

        # 过滤掉自己发送的消息
        news = [n for n in news if n.sent_from != self]

        # 添加到自己的记忆
        self.rc.memory.add_batch(news)

        return len(news)

    async def _react(self) -> Message:
        """反应：观察 -> 思考 -> 执行的完整循环"""
        # 1. 观察环境
        await self._observe()

        # 2. 执行动作
        rsp = await self._act()

        # 3. 发布消息到环境
        self.rc.env.publish_message(rsp)

        return rsp

    async def run(self, message: Message = None):
        """运行角色（主入口）"""
        if message:
            # 如果有输入消息，添加到记忆
            self.rc.memory.add(message)

        # 执行反应循环
        return await self._react()
```

### 关键设计点

#### 1. **Pydantic BaseModel**
```python
class Role(BaseModel):
    # 使用 Pydantic 提供：
    # - 类型验证
    # - 序列化/反序列化
    # - 配置管理
```

#### 2. **RoleContext（角色上下文）**
```python
class RoleContext(BaseModel):
    """角色的运行时上下文"""

    env: Environment = None           # 所在环境
    memory: Memory = None             # 记忆系统
    watch: set[str] = set()          # 监听的动作类型
    news: list[Message] = []         # 新消息
    state: int = 0                   # 当前状态

    def __init__(self, **data):
        super().__init__(**data)
        if not self.memory:
            self.memory = Memory()
```

#### 3. **观察者模式（_watch）**
```python
def _watch(self, actions: Iterable[Type[Action]]):
    """订阅感兴趣的动作类型"""
    # 将动作类型转换为字符串标识
    self.rc.watch = {any_to_str(t) for t in actions}

# 使用示例
class Architect(Role):
    def __init__(self):
        super().__init__()
        # 架构师只关注 PRD 相关的消息
        self._watch([WritePRD])
```

---

## 消息订阅机制

### Message 类设计

```python
class Message(BaseModel):
    """消息 - 角色间通信的载体"""

    content: str                      # 消息内容
    role: str = "user"               # 发送者角色
    cause_by: Type[Action] = None    # 由哪个动作产生
    sent_from: Role = None           # 发送者
    send_to: set[Role] = set()       # 接收者

    instruct_content: BaseModel = None  # 结构化指令
    restricted_to: str = ""          # 访问限制

    def __str__(self):
        return f"{self.role}: {self.content[:50]}..."

    def __repr__(self):
        return self.__str__()
```

### 订阅机制实现

```python
class Role:
    def _watch(self, actions: Iterable[Type[Action]]):
        """订阅机制的核心"""
        # 1. 将动作类型转换为字符串标识
        self.rc.watch = {any_to_str(t) for t in actions}

    async def _observe(self) -> int:
        """观察环境中的新消息"""
        # 1. 从环境获取所有消息
        all_messages = self.rc.env.memory.get()

        # 2. 过滤：只保留自己关注的消息类型
        news = []
        for msg in all_messages:
            # 检查消息是否由自己关注的动作产生
            if any_to_str(msg.cause_by) in self.rc.watch:
                # 排除自己发送的消息
                if msg.sent_from != self:
                    news.append(msg)

        # 3. 添加到自己的记忆
        self.rc.memory.add_batch(news)

        return len(news)
```

### 消息流转示例

```python
# 场景：产品经理 -> 架构师 -> 工程师

# 1. 产品经理发布 PRD
class ProductManager(Role):
    def __init__(self):
        super().__init__()
        self._watch([UserRequirement])  # 监听用户需求
        self.set_actions([WritePRD])    # 执行写 PRD

    async def _act(self):
        # 写 PRD
        prd = await WritePRD().run(context)

        # 创建消息
        msg = Message(
            content=prd,
            role="Product Manager",
            cause_by=WritePRD,  # 关键：标记消息来源
            sent_from=self
        )

        # 发布到环境
        self.rc.env.publish_message(msg)
        return msg

# 2. 架构师接收 PRD
class Architect(Role):
    def __init__(self):
        super().__init__()
        self._watch([WritePRD])  # 监听 PRD 消息
        self.set_actions([WriteDesign])

    async def _observe(self):
        # 自动过滤出 WritePRD 产生的消息
        news = self.rc.env.memory.get_by_actions(self.rc.watch)
        # news 中只包含 PRD 消息
        self.rc.memory.add_batch(news)

# 3. 工程师接收设计文档
class Engineer(Role):
    def __init__(self):
        super().__init__()
        self._watch([WriteDesign])  # 监听设计文档
        self.set_actions([WriteCode])
```

### 消息过滤流程

```
Environment.memory (所有消息)
    ↓
Role._observe() 过滤
    ↓
检查 msg.cause_by 是否在 self.rc.watch 中
    ↓
排除 msg.sent_from == self 的消息
    ↓
Role.rc.memory (角色的相关消息)
```

---

## Action 动作系统

### Action 基类

```python
class Action(BaseModel):
    """动作基类 - 角色可执行的具体操作"""

    name: str = ""                    # 动作名称
    llm: BaseLLM = Field(default_factory=LLM)  # 语言模型
    context: str = ""                 # 执行上下文
    prefix: str = ""                  # 提示词前缀
    desc: str = ""                    # 动作描述

    async def run(self, context: str = "") -> str:
        """执行动作（子类必须实现）"""
        raise NotImplementedError

    def __str__(self):
        return self.__class__.__name__

    def __repr__(self):
        return self.__str__()
```

### 具体 Action 实现示例

```python
class WritePRD(Action):
    """撰写产品需求文档"""

    name: str = "WritePRD"
    desc: str = "根据用户需求撰写详细的产品需求文档"

    PROMPT_TEMPLATE: str = """
    # 角色
    你是一位经验丰富的产品经理。

    # 任务
    基于以下用户需求，撰写详细的产品需求文档（PRD）。

    # 用户需求
    {requirements}

    # PRD 格式
    ## 1. 功能概述
    ## 2. 用户故事
    ## 3. 功能需求
    ## 4. 非功能需求
    ## 5. 验收标准

    # 输出
    请输出完整的 PRD 文档。
    """

    async def run(self, requirements: str) -> str:
        """执行：撰写 PRD"""
        # 1. 构建提示词
        prompt = self.PROMPT_TEMPLATE.format(
            requirements=requirements
        )

        # 2. 调用 LLM
        prd = await self.llm.aask(prompt)

        # 3. 后处理（可选）
        prd = self._post_process(prd)

        return prd

    def _post_process(self, prd: str) -> str:
        """后处理：格式化、验证等"""
        # 添加元数据
        prd = f"# PRD\n\n**创建时间**: {datetime.now()}\n\n{prd}"
        return prd


class WriteDesign(Action):
    """撰写系统设计文档"""

    name: str = "WriteDesign"

    PROMPT_TEMPLATE: str = """
    # 角色
    你是一位资深系统架构师。

    # 任务
    基于以下 PRD，设计系统架构。

    # PRD
    {prd}

    # 设计文档格式
    ## 1. 系统架构
    ## 2. 数据模型
    ## 3. API 设计
    ## 4. 技术选型
    ## 5. 部署方案

    # 输出
    请输出完整的设计文档。
    """

    async def run(self, prd: str) -> str:
        """执行：撰写设计文档"""
        prompt = self.PROMPT_TEMPLATE.format(prd=prd)
        design = await self.llm.aask(prompt)
        return design


class WriteCode(Action):
    """编写代码"""

    name: str = "WriteCode"

    PROMPT_TEMPLATE: str = """
    # 角色
    你是一位高级软件工程师。

    # 任务
    基于以下设计文档，编写高质量代码。

    # 设计文档
    {design}

    # 代码要求
    1. 遵循最佳实践
    2. 包含单元测试
    3. 添加必要注释
    4. 符合代码规范

    # 输出
    请输出完整的代码实现。
    """

    async def run(self, design: str) -> str:
        """执行：编写代码"""
        prompt = self.PROMPT_TEMPLATE.format(design=design)
        code = await self.llm.aask(prompt)
        return code
```

### Action 的组合使用

```python
class ProductManager(Role):
    def __init__(self):
        super().__init__()
        # 设置多个动作
        self.set_actions([
            WritePRD,           # 主要动作
            WriteUserStories,   # 辅助动作
            ValidatePRD         # 验证动作
        ])

    async def _act(self):
        """执行动作序列"""
        # 1. 写 PRD
        prd = await WritePRD().run(context)

        # 2. 写用户故事
        stories = await WriteUserStories().run(prd)

        # 3. 验证 PRD
        validation = await ValidatePRD().run(prd)

        # 4. 合并结果
        result = f"{prd}\n\n{stories}\n\n{validation}"

        return Message(content=result, ...)
```

---

## Memory 记忆管理

### Memory 类设计

```python
class Memory(BaseModel):
    """记忆系统 - 存储和检索消息"""

    storage: list[Message] = []       # 消息存储
    index: dict[str, list[Message]] = {}  # 索引：按动作类型

    def add(self, message: Message):
        """添加单条消息"""
        self.storage.append(message)

        # 更新索引
        action_type = any_to_str(message.cause_by)
        if action_type not in self.index:
            self.index[action_type] = []
        self.index[action_type].append(message)

    def add_batch(self, messages: list[Message]):
        """批量添加消息"""
        for msg in messages:
            self.add(msg)

    def get(self, k=0) -> list[Message]:
        """获取最近的 k 条消息（k=0 表示全部）"""
        if k == 0:
            return self.storage
        return self.storage[-k:]

    def get_by_action(self, action: Type[Action]) -> Message:
        """根据动作类型获取最新消息"""
        action_type = any_to_str(action)
        messages = self.index.get(action_type, [])
        return messages[-1] if messages else None

    def get_by_actions(self, actions: set[Type[Action]]) -> list[Message]:
        """根据多个动作类型获取消息"""
        result = []
        for action in actions:
            action_type = any_to_str(action)
            result.extend(self.index.get(action_type, []))
        return result

    def clear(self):
        """清空记忆"""
        self.storage = []
        self.index = {}

    def count(self) -> int:
        """消息数量"""
        return len(self.storage)
```

### 记忆的使用场景

```python
class Role:
    async def _act(self):
        """使用记忆获取上下文"""
        # 场景 1：获取特定类型的最新消息
        prd = self.rc.memory.get_by_action(WritePRD)

        # 场景 2：获取多种类型的消息
        context_messages = self.rc.memory.get_by_actions([
            WritePRD,
            WriteUserStories
        ])

        # 场景 3：获取最近 N 条消息
        recent = self.rc.memory.get(k=5)

        # 场景 4：获取所有消息
        all_messages = self.rc.memory.get()

        # 使用上下文执行动作
        result = await self.actions[0].run(context=prd.content)
```

---

## Team 团队协作

### Team 类设计

```python
class Team(BaseModel):
    """团队 - 多个角色的协作单元"""

    env: Environment = Field(default_factory=Environment)
    investment: float = 10.0          # 预算（控制 LLM 调用成本）
    idea: str = ""                    # 项目想法

    def hire(self, roles: list[Role]):
        """招聘角色"""
        for role in roles:
            role.rc.env = self.env    # 设置环境
            self.env.add_role(role)   # 添加到环境

    def invest(self, investment: float):
        """投资（设置预算）"""
        self.investment = investment
        self.env.set_budget(investment)

    async def run_project(self, idea: str, n_round=10):
        """运行项目"""
        self.idea = idea

        # 1. 创建初始消息
        initial_message = Message(
            content=idea,
            role="User",
            cause_by=UserRequirement
        )

        # 2. 发布到环境
        self.env.publish_message(initial_message)

        # 3. 运行 n 轮
        for i in range(n_round):
            # 检查预算
            if self.env.is_budget_exhausted():
                break

            # 让所有角色执行一轮
            await self.env.run()

            # 检查是否完成
            if self.env.is_project_done():
                break

        # 4. 返回结果
        return self.env.get_project_output()
```

### Environment 类设计

```python
class Environment(BaseModel):
    """环境 - 角色运行的容器"""

    roles: dict[str, Role] = {}       # 角色列表
    memory: Memory = Field(default_factory=Memory)  # 共享记忆
    history: str = ""                 # 历史记录

    def add_role(self, role: Role):
        """添加角色"""
        role.rc.env = self
        self.roles[role.profile] = role

    def publish_message(self, message: Message):
        """发布消息到环境"""
        self.memory.add(message)
        self.history += f"\n{message}"

    async def run(self, k=1):
        """运行一轮：让所有角色执行"""
        for _ in range(k):
            for role in self.roles.values():
                # 每个角色观察、思考、执行
                await role.run()
```

### 完整的团队协作示例

```python
# 创建团队
company = Team()

# 招聘角色
company.hire([
    ProductManager(
        name="Alice",
        profile="Product Manager",
        goal="设计成功的产品"
    ),
    Architect(
        name="Bob",
        profile="Architect",
        goal="设计优雅的架构"
    ),
    Engineer(
        name="Charlie",
        profile="Engineer",
        goal="编写高质量代码"
    ),
    QAEngineer(
        name="David",
        profile="QA Engineer",
        goal="确保软件质量"
    )
])

# 设置预算
company.invest(investment=10.0)

# 运行项目
result = await company.run_project(
    idea="开发一个 FBA 货件批量导入功能"
)

# 输出结果
print(result)
```

---

**文档未完待续...**

下一部分将包含：
- 完整的源码分析
- 实际运行示例
- 调试技巧
- 性能优化

**当前文档大小**: 约 8KB
**预计完整文档**: 约 30-40KB

是否需要我继续完成剩余部分？
