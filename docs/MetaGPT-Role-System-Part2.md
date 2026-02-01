# MetaGPT 角色系统深度解析（第二部分）

**接续**: `MetaGPT-Role-System-Deep-Dive.md`

---

## 完整示例：从零构建一个软件公司

### 示例场景

我们要构建一个简化版的软件公司，包含 3 个角色：
1. **ProductManager**: 撰写 PRD
2. **Architect**: 设计架构
3. **Engineer**: 编写代码

### 完整代码实现

```python
import asyncio
from metagpt.roles import Role
from metagpt.actions import Action
from metagpt.schema import Message
from metagpt.team import Team
from metagpt.llm import LLM

# ============================================
# 第一步：定义 Actions
# ============================================

class WritePRD(Action):
    """撰写产品需求文档"""

    name: str = "WritePRD"

    PROMPT_TEMPLATE: str = """
    你是一位产品经理。请基于以下需求撰写 PRD：

    需求：{requirements}

    PRD 应包含：
    1. 功能概述
    2. 核心功能列表
    3. 验收标准

    请输出简洁的 PRD。
    """

    async def run(self, requirements: str) -> str:
        prompt = self.PROMPT_TEMPLATE.format(requirements=requirements)
        prd = await self.llm.aask(prompt)
        return prd


class WriteDesign(Action):
    """撰写设计文档"""

    name: str = "WriteDesign"

    PROMPT_TEMPLATE: str = """
    你是一位架构师。请基于以下 PRD 设计系统：

    PRD：{prd}

    设计文档应包含：
    1. 系统架构
    2. 核心模块
    3. 技术选型

    请输出简洁的设计文档。
    """

    async def run(self, prd: str) -> str:
        prompt = self.PROMPT_TEMPLATE.format(prd=prd)
        design = await self.llm.aask(prompt)
        return design


class WriteCode(Action):
    """编写代码"""

    name: str = "WriteCode"

    PROMPT_TEMPLATE: str = """
    你是一位工程师。请基于以下设计文档编写代码：

    设计：{design}

    代码要求：
    1. Python 实现
    2. 包含主要函数
    3. 添加注释

    请输出代码实现。
    """

    async def run(self, design: str) -> str:
        prompt = self.PROMPT_TEMPLATE.format(design=design)
        code = await self.llm.aask(prompt)
        return code


# ============================================
# 第二步：定义 Roles
# ============================================

class ProductManager(Role):
    """产品经理角色"""

    def __init__(
        self,
        name: str = "Alice",
        profile: str = "Product Manager",
        goal: str = "设计成功的产品",
        constraints: str = "遵循公司规范"
    ):
        super().__init__(name=name, profile=profile, goal=goal, constraints=constraints)

        # 初始化动作
        self._init_actions([WritePRD])

        # 监听用户需求
        self._watch([UserRequirement])

    async def _act(self) -> Message:
        """执行：撰写 PRD"""
        # 1. 获取用户需求
        todo = self.rc.todo

        # 2. 获取上下文
        context = self.rc.memory.get_by_action(UserRequirement)
        requirements = context.content if context else self.rc.memory.get()[-1].content

        # 3. 执行动作
        prd = await WritePRD().run(requirements)

        # 4. 创建消息
        msg = Message(
            content=prd,
            role=self.profile,
            cause_by=WritePRD,
            sent_from=self
        )

        # 5. 保存到记忆
        self.rc.memory.add(msg)

        return msg


class Architect(Role):
    """架构师角色"""

    def __init__(
        self,
        name: str = "Bob",
        profile: str = "Architect",
        goal: str = "设计优雅的架构",
        constraints: str = "考虑可扩展性和性能"
    ):
        super().__init__(name=name, profile=profile, goal=goal, constraints=constraints)

        # 初始化动作
        self._init_actions([WriteDesign])

        # 监听 PRD
        self._watch([WritePRD])

    async def _act(self) -> Message:
        """执行：撰写设计文档"""
        # 1. 获取 PRD
        prd_msg = self.rc.memory.get_by_action(WritePRD)
        prd = prd_msg.content if prd_msg else ""

        # 2. 执行动作
        design = await WriteDesign().run(prd)

        # 3. 创建消息
        msg = Message(
            content=design,
            role=self.profile,
            cause_by=WriteDesign,
            sent_from=self
        )

        # 4. 保存到记忆
        self.rc.memory.add(msg)

        return msg


class Engineer(Role):
    """工程师角色"""

    def __init__(
        self,
        name: str = "Charlie",
        profile: str = "Engineer",
        goal: str = "编写高质量代码",
        constraints: str = "遵循最佳实践"
    ):
        super().__init__(name=name, profile=profile, goal=goal, constraints=constraints)

        # 初始化动作
        self._init_actions([WriteCode])

        # 监听设计文档
        self._watch([WriteDesign])

    async def _act(self) -> Message:
        """执行：编写代码"""
        # 1. 获取设计文档
        design_msg = self.rc.memory.get_by_action(WriteDesign)
        design = design_msg.content if design_msg else ""

        # 2. 执行动作
        code = await WriteCode().run(design)

        # 3. 创建消息
        msg = Message(
            content=code,
            role=self.profile,
            cause_by=WriteCode,
            sent_from=self
        )

        # 4. 保存到记忆
        self.rc.memory.add(msg)

        return msg


# ============================================
# 第三步：创建团队并运行
# ============================================

async def main():
    """主函数"""

    # 1. 创建团队
    company = Team()

    # 2. 招聘角色
    company.hire([
        ProductManager(),
        Architect(),
        Engineer()
    ])

    # 3. 设置预算
    company.invest(investment=10.0)

    # 4. 运行项目
    print("🚀 启动项目...")
    result = await company.run_project(
        idea="开发一个简单的待办事项管理系统",
        n_round=5  # 最多运行 5 轮
    )

    # 5. 输出结果
    print("\n" + "="*60)
    print("📊 项目输出:")
    print("="*60)
    print(result)


# 运行
if __name__ == "__main__":
    asyncio.run(main())
```

### 执行流程详解

```
轮次 1:
  User → UserRequirement 消息
  ↓
  ProductManager 观察到 UserRequirement
  ↓
  ProductManager 执行 WritePRD
  ↓
  发布 PRD 消息到环境

轮次 2:
  Architect 观察到 WritePRD 消息
  ↓
  Architect 执行 WriteDesign
  ↓
  发布 Design 消息到环境

轮次 3:
  Engineer 观察到 WriteDesign 消息
  ↓
  Engineer 执行 WriteCode
  ↓
  发布 Code 消息到环境

轮次 4-5:
  没有新的消息需要处理
  ↓
  项目完成
```

---

## 源码分析：关键实现细节

### 1. 消息过滤的实现

```python
# metagpt/memory/memory.py

class Memory:
    def get_by_actions(self, actions: set) -> list[Message]:
        """根据动作类型过滤消息"""
        result = []

        # 遍历所有消息
        for msg in self.storage:
            # 检查消息的 cause_by 是否在监听列表中
            if any_to_str(msg.cause_by) in actions:
                result.append(msg)

        return result

# 关键函数：any_to_str
def any_to_str(action: Type[Action]) -> str:
    """将动作类型转换为字符串标识"""
    if isinstance(action, type):
        return action.__name__
    return str(action)
```

### 2. 角色的执行循环

```python
# metagpt/roles/role.py

class Role:
    async def run(self, message: Message = None) -> Message:
        """运行角色的主循环"""

        # 1. 如果有输入消息，添加到记忆
        if message:
            self.rc.memory.add(message)

        # 2. 观察环境
        num_news = await self._observe()

        # 3. 如果没有新消息，返回
        if num_news == 0:
            return None

        # 4. 执行动作
        rsp = await self._act()

        # 5. 发布消息到环境
        self.rc.env.publish_message(rsp)

        return rsp

    async def _observe(self) -> int:
        """观察环境中的新消息"""
        # 从环境获取消息
        news = self.rc.env.memory.get_by_actions(self.rc.watch)

        # 过滤掉自己发送的消息
        news = [n for n in news if n.sent_from != self]

        # 过滤掉已经处理过的消息
        old_messages = set(id(m) for m in self.rc.memory.storage)
        news = [n for n in news if id(n) not in old_messages]

        # 添加到自己的记忆
        self.rc.memory.add_batch(news)

        return len(news)
```

### 3. 环境的运行机制

```python
# metagpt/environment.py

class Environment:
    async def run(self, k=1):
        """运行 k 轮"""
        for i in range(k):
            # 获取所有角色
            roles = list(self.roles.values())

            # 并发执行所有角色
            futures = []
            for role in roles:
                future = role.run()
                futures.append(future)

            # 等待所有角色完成
            results = await asyncio.gather(*futures)

            # 检查是否有新消息产生
            has_new_messages = any(r is not None for r in results)

            # 如果没有新消息，停止运行
            if not has_new_messages:
                break
```

---

## 调试技巧

### 1. 启用详细日志

```python
import logging

# 设置日志级别
logging.basicConfig(
    level=logging.DEBUG,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)

# MetaGPT 的日志
logger = logging.getLogger("metagpt")
logger.setLevel(logging.DEBUG)
```

### 2. 打印消息流

```python
class DebugRole(Role):
    async def _observe(self) -> int:
        """观察并打印消息"""
        num_news = await super()._observe()

        print(f"\n[{self.profile}] 观察到 {num_news} 条新消息:")
        for msg in self.rc.memory.get(k=num_news):
            print(f"  - {msg.role}: {msg.content[:50]}...")

        return num_news

    async def _act(self) -> Message:
        """执行并打印结果"""
        print(f"\n[{self.profile}] 开始执行动作...")

        msg = await super()._act()

        print(f"[{self.profile}] 完成动作，输出:")
        print(f"  {msg.content[:100]}...")

        return msg
```

### 3. 检查记忆状态

```python
def print_memory(role: Role):
    """打印角色的记忆状态"""
    print(f"\n{'='*60}")
    print(f"{role.profile} 的记忆:")
    print(f"{'='*60}")

    memory = role.rc.memory
    print(f"总消息数: {memory.count()}")

    print("\n消息列表:")
    for i, msg in enumerate(memory.storage, 1):
        print(f"{i}. [{msg.role}] {msg.content[:50]}...")

    print("\n索引:")
    for action_type, messages in memory.index.items():
        print(f"  {action_type}: {len(messages)} 条消息")
```

### 4. 追踪消息流转

```python
class TrackedMessage(Message):
    """可追踪的消息"""

    def __init__(self, **data):
        super().__init__(**data)
        self.trace_id = str(uuid.uuid4())[:8]
        self.timestamp = datetime.now()

    def __str__(self):
        return f"[{self.trace_id}] {self.role}: {self.content[:30]}..."

# 使用
msg = TrackedMessage(
    content="PRD 内容...",
    role="Product Manager",
    cause_by=WritePRD
)

print(f"消息创建: {msg.trace_id} at {msg.timestamp}")
```

---

## 性能优化

### 1. 并行执行动作

```python
class ParallelRole(Role):
    """支持并行执行多个动作的角色"""

    async def _act(self) -> Message:
        """并行执行多个动作"""
        if len(self.actions) <= 1:
            return await super()._act()

        # 并行执行所有动作
        tasks = []
        for action in self.actions:
            task = action().run(context)
            tasks.append(task)

        # 等待所有动作完成
        results = await asyncio.gather(*tasks)

        # 合并结果
        combined_result = "\n\n".join(results)

        return Message(content=combined_result, ...)
```

### 2. 缓存 LLM 响应

```python
from functools import lru_cache
import hashlib

class CachedAction(Action):
    """带缓存的动作"""

    @lru_cache(maxsize=100)
    async def run(self, context: str) -> str:
        """执行动作（带缓存）"""
        # 生成缓存键
        cache_key = hashlib.md5(context.encode()).hexdigest()

        # 检查缓存
        if cache_key in self._cache:
            print(f"[Cache Hit] {self.name}")
            return self._cache[cache_key]

        # 执行动作
        result = await super().run(context)

        # 保存到缓存
        self._cache[cache_key] = result

        return result
```

### 3. 限制记忆大小

```python
class BoundedMemory(Memory):
    """有界记忆 - 只保留最近的 N 条消息"""

    def __init__(self, max_size: int = 100):
        super().__init__()
        self.max_size = max_size

    def add(self, message: Message):
        """添加消息（自动清理旧消息）"""
        super().add(message)

        # 如果超过限制，删除最旧的消息
        if len(self.storage) > self.max_size:
            # 删除最旧的消息
            old_msg = self.storage.pop(0)

            # 更新索引
            action_type = any_to_str(old_msg.cause_by)
            if action_type in self.index:
                self.index[action_type].remove(old_msg)
```

### 4. 批量处理消息

```python
class BatchRole(Role):
    """批量处理消息的角色"""

    async def run(self, message: Message = None) -> list[Message]:
        """批量处理多条消息"""
        # 1. 观察环境
        await self._observe()

        # 2. 获取所有待处理消息
        news = self.rc.memory.get_by_actions(self.rc.watch)

        # 3. 批量处理
        results = []
        for msg in news:
            result = await self._process_message(msg)
            results.append(result)

        return results
```

---

## 高级技巧

### 1. 动态角色切换

```python
class AdaptiveRole(Role):
    """自适应角色 - 根据情况切换动作"""

    def __init__(self, **kwargs):
        super().__init__(**kwargs)

        # 定义多组动作
        self.action_sets = {
            "design": [WriteDesign, ReviewDesign],
            "code": [WriteCode, WriteTests],
            "review": [ReviewCode, ApproveCode]
        }

        self.current_mode = "design"

    async def _think(self) -> Action:
        """根据上下文选择动作集"""
        # 分析当前状态
        recent_messages = self.rc.memory.get(k=5)

        # 决定模式
        if any("设计" in msg.content for msg in recent_messages):
            self.current_mode = "design"
        elif any("代码" in msg.content for msg in recent_messages):
            self.current_mode = "code"
        elif any("审查" in msg.content for msg in recent_messages):
            self.current_mode = "review"

        # 切换动作集
        self.actions = self.action_sets[self.current_mode]

        # 选择动作
        return await super()._think()
```

### 2. 人类参与（Human-in-the-loop）

```python
class HumanRole(Role):
    """需要人类参与的角色"""

    async def _act(self) -> Message:
        """执行动作（需要人类确认）"""
        # 1. AI 生成初稿
        draft = await super()._act()

        # 2. 展示给人类
        print(f"\n{'='*60}")
        print(f"[{self.profile}] AI 生成的内容:")
        print(f"{'='*60}")
        print(draft.content)

        # 3. 请求人类反馈
        print(f"\n请选择:")
        print("1. 接受")
        print("2. 修改")
        print("3. 重新生成")

        choice = input("你的选择 (1/2/3): ")

        if choice == "1":
            # 接受
            return draft
        elif choice == "2":
            # 修改
            modified = input("请输入修改后的内容: ")
            draft.content = modified
            return draft
        else:
            # 重新生成
            return await self._act()
```

### 3. 多轮对话优化

```python
class ConversationalRole(Role):
    """支持多轮对话的角色"""

    async def _act(self) -> Message:
        """多轮对话式执行"""
        max_rounds = 3

        for round_num in range(max_rounds):
            # 执行动作
            result = await self.actions[0].run(context)

            # 自我评估
            evaluation = await self._evaluate(result)

            if evaluation["quality"] >= 0.8:
                # 质量足够，返回结果
                return Message(content=result, ...)

            # 质量不够，改进提示词
            context = self._improve_context(context, evaluation)

        # 达到最大轮次，返回最后结果
        return Message(content=result, ...)

    async def _evaluate(self, result: str) -> dict:
        """评估结果质量"""
        prompt = f"""
        评估以下内容的质量（0-1）:
        {result}

        返回 JSON: {{"quality": 0.0-1.0, "issues": ["问题1", "问题2"]}}
        """

        evaluation = await self.llm.aask(prompt)
        return json.loads(evaluation)
```

---

## 实战案例：为慕声税务系统设计 Agent

### 角色定义

```python
class TaxAnalyst(Role):
    """税务分析师"""

    def __init__(self):
        super().__init__(
            name="TaxAnalyst",
            profile="税务分析师",
            goal="准确分析税务规则",
            constraints="必须遵循最新税法，零容忍错误"
        )

        self._init_actions([
            AnalyzeTaxRules,
            GenerateTaxSpec,
            ValidateTaxLogic
        ])

        self._watch([TaxRequirement])


class DataValidator(Role):
    """数据验证师"""

    def __init__(self):
        super().__init__(
            name="DataValidator",
            profile="数据验证师",
            goal="确保数据 100% 准确",
            constraints="零容忍数据错误"
        )

        self._init_actions([
            ValidateDataFormat,
            CheckBusinessRules,
            GenerateValidationReport
        ])

        self._watch([ImportRequest])


class CodeGenerator(Role):
    """代码生成器"""

    def __init__(self):
        super().__init__(
            name="CodeGenerator",
            profile="代码生成器",
            goal="生成符合规范的代码",
            constraints="必须遵循 TDD 和 Constitution"
        )

        self._init_actions([
            WriteTests,
            GenerateCode,
            RefactorCode
        ])

        self._watch([TaxSpec, ValidationReport])
```

### 使用示例

```python
# 创建税务开发团队
tax_team = Team()

tax_team.hire([
    TaxAnalyst(),
    DataValidator(),
    CodeGenerator(),
    TestEngineer(),
    Reviewer()
])

# 运行项目
result = await tax_team.run_project(
    idea="实现 FBA 货件批量导入功能，包含数据验证和税务计算"
)
```

---

## 总结

### MetaGPT 角色系统的核心优势

1. **清晰的职责分离**: 每个角色有明确的目标和约束
2. **松耦合通信**: 通过消息订阅机制实现角色间通信
3. **可扩展性**: 易于添加新角色和新动作
4. **可测试性**: 每个组件都可以独立测试
5. **可观察性**: 完整的消息历史和记忆系统

### 关键设计模式

- **观察者模式**: 消息订阅机制
- **策略模式**: 动作的可替换性
- **命令模式**: Action 的封装
- **备忘录模式**: Memory 系统

### 适用场景

✅ **适合**:
- 多角色协作的复杂任务
- 需要明确工作流程的场景
- 软件开发、内容创作等领域

❌ **不适合**:
- 简单的单一任务
- 需要实时响应的场景
- 对成本敏感的应用（LLM 调用较多）

---

**文档完成！**

相关文档：
- 第一部分：`MetaGPT-Role-System-Deep-Dive.md`
- Agent 研究总结：`Agent-Research-Summary.md`
- Spec-Kit 配置：`.specify/SETUP_COMPLETE.md`
