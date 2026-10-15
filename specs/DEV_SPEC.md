# Developer Specification (DEV_SPEC)

> 本文档是 Ecom-ai 的项目级设计规格，描述系统目标、设计原则、技术选择、模块边界、关键链路、数据契约、测试方案和演进路线。
> 功能级需求见 `specs/<capability>/spec.md`；实现计划和任务追踪见对应目录的 `plan.md` 与 `tasks.md`。

## 目录

1. 项目概述
2. 设计理念
3. 核心能力
4. 技术选型
5. 系统架构与模块设计
6. 请求生命周期与关键链路
7. 数据契约
8. Tool、RAG 与 Workflow 设计
9. 安全、Trace 与评估
10. 测试方案
11. 部署与运行
12. 工程规范
13. 交付里程碑
14. 已知边界
15. 演进路线
16. 术语表

---

## 1. 项目概述

### 1.1 定位

Ecom-ai 是一个面向电商售后场景的全链路客服 Agent 系统。它把自然语言理解、实时订单事实、知识检索、工具调用、售后审批、安全防护、执行追踪和质量评估放在同一套可验证的工程边界内。

系统不是“让模型直接操作订单”的聊天机器人，而是三服务解耦的业务系统：

| 服务 | 技术 | 核心职责 |
|---|---|---|
| `agent-service` | Python + FastAPI + LangGraph | 意图路由、Tool、RAG、Workflow、HITL、安全、Trace、Eval |
| `ecommerce-backend` | Java + Spring Boot | 订单、物流、商品、退款、审批、会员和业务事实 |
| `frontend` | React + TypeScript + Vite | Runtime Context 注入、聊天、Trace、Eval、Resume 和反馈 |

Agent 服务只能通过受控 HTTP 接口读取业务事实，不能直接访问 Java 业务库。

### 1.2 要解决的问题

| 问题 | 传统方式 | 本系统 |
|---|---|---|
| 模型不知道实时订单状态 | 让模型猜或让用户查后台 | Tool 查询业务后端 |
| 政策回答没有依据 | 依赖模型记忆 | Hybrid RAG + citation |
| 高风险售后直接执行 | 模型或脚本直接改状态 | Workflow + HITL + checkpoint |
| 执行过程不可解释 | 只看最终回答 | 公开 Trace + session state |
| 质量靠感觉 | 人工追问几个 case | 固定回归集检查答案和路径 |
| 供应商不可用 | 单点失败 | 确定性 fallback + 降级转人工 |

### 1.3 成功标准

- 常规 FAQ、活动规则和订单查询可以端到端运行。
- 高风险退款和退货不会直接执行资金动作。
- 低置信规则问题不会编造政策。
- 订单归属和工具调用可以被审计。
- 失败时可以定位到路由、Tool、RAG、Context、Workflow 或安全层。
- 代码、规格、回归 case 和 Trace 能互相印证。

### 1.4 非目标

- 不实现真实支付或退款清算。
- 不承诺多实例共享 Workflow checkpoint。
- 不把系统描述成完整 FinOps、质检平台或生产监控平台。
- 不提供 hidden chain-of-thought。
- 不允许模型绕过工具白名单访问任意业务接口。

---

## 2. 设计理念

### 2.1 事实优先，而非语言优先

回答质量首先取决于事实是否正确。系统按以下优先级处理上下文：

```text
Runtime Context
  > Verified Tool Fact
  > Workflow State
  > RAG Citation
  > Session Memory
  > Conversation History
  > User Message
```

用户文本只能表达问题，不能覆盖系统身份、订单归属、工具权限和审批边界。

### 2.2 模型参与决策，但系统拥有最终控制权

模型负责：

- 意图候选；
- 工具候选；
- 知识域候选；
- 自然语言表达。

系统负责：

- Runtime 身份；
- 订单归属；
- 工具白名单；
- 高风险动作分类；
- Workflow 状态机；
- 审批和幂等；
- 安全拒绝。

### 2.3 只读默认，高风险显式升级

普通 Agent 请求只能执行只读 Tool。退款和退货必须进入 Workflow，在 `require_approval` 暂停，恢复时重新校验冻结字段和 Resume Token。

### 2.4 每一层都要可观测

系统记录 route、tool、rag、workflow、hook、cost 和 security 事件。可观测性只暴露“执行了什么”，不暴露隐藏推理和系统 Prompt。

### 2.5 组件可替换

模型、Embedding、Reranker、知识文件和业务 API 都通过配置或工厂边界接入。替换组件不应重写 Agent 主流程。

### 2.6 质量必须可回归

一次 Prompt 或检索策略调整可能让原本正常的 case 退化。每条回归 case 同时检查最终回答和执行路径。

---

## 3. 核心能力

### 3.1 Intent Routing

意图枚举：

```text
general_chat
order_query
refund_status_query
refund_request
return_request
faq_query
promotion_query
product_query
low_confidence_query
degradation_request
security_request
unknown
```

输出 `RoutePlan`：

```json
{
  "intent": "order_query",
  "needs_rag": false,
  "needs_business_tools": true,
  "required_tools": ["get_order_detail", "get_order_logistics"],
  "tool_candidates": [],
  "knowledge_domains": [],
  "entity_refs": ["SO20260602103000009-a1000009"],
  "risk_level": "low",
  "requires_workflow": false,
  "confidence": 0.91,
  "source": "llm_with_policy_constraints",
  "fallback_policy": "safe_deterministic_path"
}
```

路由后系统重新执行白名单和风险约束，模型输出不是最终执行计划。

### 3.2 Tool Runtime

只读工具：

| 工具 | 输入 | 输出 | 风险 |
|---|---|---|---|
| `get_order_detail` | `order_id`, runtime user | 订单详情 | low |
| `get_order_logistics` | 订单快照 | 物流事件和状态 | low |
| `get_refund_status` | `order_id`, runtime user | 退款申请进度 | low |
| `search_products` | `keyword` | 商品和库存 | low |

工具结果统一为 `ToolCallTrace`，包含 arguments、output_summary、status、risk、next_action 和 error_type。

### 3.3 Hybrid RAG

检索范围：

- 售后政策；
- 发票 FAQ；
- 活动规则；
- 会员权益；
- 退货规则。

链路：

```text
Query
  → pre-retrieval
  → keyword scoring
  → embedding similarity
  → domain / version filter
  → candidate pool
  → lightweight reranker
  → citation assembly
  → answer generation
```

低置信结果转人工，不生成确定性政策。

### 3.4 Workflow 与 HITL

- 未发货退款；
- 签收后退货。

状态：

```text
draft
  → eligibility_checked
  → require_approval
  → business_recheck
  → submitted / rejected / blocked / paused
```

暂停时返回 `workflow_id`、`pending_action` 和 `resume_token`。

### 3.5 Safety

四路输入：

- user message；
- tool output；
- RAG text；
- history。

处理结果：

- `tainted` 标记；
- 攻击原文脱敏；
- 安全事件写入公开 Trace；
- 高风险请求阻断或转人工。

### 3.6 Trace、Eval 与 Cost

公开 Trace schema：`trace_event_v1`。

固定回归集检查：

- answer signals；
- tools；
- citations；
- trace events；
- session state；
- workflow state；
- forbidden text。

成本摘要区分轻路径、重路径、缓存、Prompt 片段和 Observation 压缩。

---

## 4. 技术选型

### 4.1 Agent 服务：FastAPI

**选择理由**

- Pydantic 提供 API 契约校验；
- Python AI 生态成熟；
- 易于实现 HTTP、Tool、RAG、Workflow 和评估；
- OpenAPI 便于前端联调。

**放弃方案**

- LangServe：不适合复杂业务 API 和 HITL 状态管理。
- Flask：异步和类型契约能力不足。
- 纯 LLM SDK：缺少 HTTP 层、鉴权和结构化验证。

### 4.2 Workflow：LangGraph

**选择理由**

- 显式 StateGraph；
- 支持暂停和恢复；
- 适合 HITL、Checkpoint 和幂等设计；
- 状态和边易于测试和可视化。

**代价**

- Checkpoint 持久化和多实例扩展需要额外设计；
- 图和节点调试比普通函数复杂。

### 4.3 业务后端：Spring Boot

**选择理由**

- 电商业务天然适合分层 Service/Repository/Controller；
- 事务、鉴权、审计和数据库迁移工具成熟；
- 可以独立维护订单和售后状态机。

**代价**

- 代码量和类型样板较多；
- Agent 与业务系统之间存在跨语言联调成本。

### 4.4 检索：Hybrid RAG + RRF + 轻量 Rerank

**选择理由**

- 关键词检索解决订单号、活动名和专有名词；
- 向量检索解决同义表达和口语化问题；
- RRF 避免跨模态分数归一化；
- 轻量 rerank 控制成本和延迟。

**放弃方案**

- 纯向量：专有名词和精确匹配弱。
- 纯 BM25：语义泛化弱。
- 重型 Cross-Encoder：当前 CPU 和成本约束下不是默认选择。

### 4.5 前端：React + TypeScript + Vite

**选择理由**

- 调试面板需要快速迭代；
- TypeScript 与 API Schema 对齐；
- Vite 构建轻量。

**边界**

- 只展示受控 reasoning summary；
- 不接收 hidden CoT；
- 不直接连接业务数据库。

---

## 5. 系统架构与模块设计

### 5.1 整体架构

```text
┌──────────────────────────────────────────────────────────────┐
│                    React Debug Console                       │
│  Chat / Resume / Trace / Eval / Feedback                     │
└───────────────────────┬──────────────────────────────────────┘
                        │ HTTPS / JSON
                        ▼
┌──────────────────────────────────────────────────────────────┐
│                    FastAPI Agent Service                     │
│                                                              │
│  Router → Tool / RAG / Workflow / Degradation / Security     │
│    │         │        │           │             │            │
│    │         │        │           │             └─ Source Guard│
│    │         │        │           └─ Checkpoint / Resume       │
│    │         │        └─ Hybrid Retrieval / Rerank             │
│    │         └─ E-commerce Client                              │
│    └─ Trace / Eval / Cost                                      │
└───────────────┬──────────────────────────┬───────────────────┘
                │ HTTP                     │ Model API
                ▼                          ▼
┌──────────────────────────────┐   ┌────────────────────────────┐
│ Spring Boot E-commerce       │   │ OpenAI-compatible Models   │
│ Order / Logistics / Refund   │   │ Chat / Embedding           │
│ Product / Approval / Member  │   └────────────────────────────┘
└───────────────┬──────────────┘
                ▼
              MySQL
```

### 5.2 Agent 服务分层

| 层 | 目录 | 职责 |
|---|---|---|
| API | `api/` | 路由、Schema、错误响应 |
| Orchestration | `agents/` | 主流程编排 |
| Context | `context/` | Runtime Context、信任级别、压缩 |
| Tools | `tools/` | 工具白名单、参数校验、业务 API |
| RAG | `rag/` | 知识文件、召回、融合、Rerank |
| Workflow | `workflows/` | LangGraph、Resume、幂等 |
| Safety | `safety/` | 输入扫描、脱敏、风险标记 |
| Observability | `observability/` | Trace 和公开摘要 |
| Evaluation | `evals/` | 固定回归 case |
| Feedback | `feedback/` | 失败归因和 case 回填 |
| Cost | `cost/` | 路径成本和 Observation 压缩 |

### 5.3 Java 后端分层

```text
controller → service → repository → MySQL
              │
              └── security / identity / approval
```

职责：

- 提供业务事实；
- 维护退款和退货状态；
- 记录人工审批；
- 验证 Agent Service 身份；
- 不包含 Prompt、LLM 和 RAG。

### 5.4 前端模块

| 模块 | 职责 |
|---|---|
| `useAgentChat` | 会话、用户、请求、Resume 和 Trace |
| `TracePanel` | 公开 Trace 和成本展示 |
| `EvaluationPanel` | 回归结果展示 |
| `demoScenarios` | 演示场景 |
| `api/types` | API 类型契约 |

### 5.5 配置边界

`agent-service/config/settings.py` 负责：

- 能力清单；
- Regression Case 路径；
- `.env` 加载；
- E-commerce Base URL；
- 模型和 Embedding 名称。

启动配置不得写入业务逻辑和硬编码事务。

---

## 6. 请求生命周期与关键链路

### 6.1 `/chat` 请求生命周期

```text
1. FastAPI 校验 ChatRequest
2. Source Guard 扫描用户输入和历史
3. Runtime Context 归一化
4. 确定性高风险 Guard
5. 模型路由 / fallback 路由
6. 工具白名单和实体校验
7. 选择 Tool / RAG / Workflow / Degradation / Security 主路径
8. 组装受控事实和 Citation
9. 模型生成最终客服话术
10. 记录 Trace、Eval 信号和 Cost
11. 返回 ChatResponse
```

### 6.2 物流查询

```text
用户问题
  → 提取并校验订单号
  → 确认订单归属
  → get_order_detail
  → get_order_logistics
  → Observation 压缩
  → 生成客服话术
```

该路径不调用知识库，因为物流是实时业务事实。

### 6.3 活动规则

```text
用户问题
  → intent = promotion_query
  → knowledge_domain = promotion / member
  → Hybrid Retrieval
  → RRF
  → Rerank
  → Citation
  → 模型回答
```

### 6.4 未发货退款

```text
订单存在且可访问
  → fulfillment_status = PENDING_SHIPMENT
  → 建立 workflow checkpoint
  → require_approval
  → 返回 resume_token
```

### 6.5 Prompt 注入

```text
四路输入
  → Source Guard
  → tainted
  → sanitized content
  → security_blocked
  → 安全拒绝
```

### 6.6 降级

```text
Tool timeout / 5xx
  → error classification
  → 只读重试（受控）
  → 降级模板
  → transfer_to_human
  → Trace
```
---

## 7. 数据契约

### 7.1 ChatRequest

```python
class ChatRequest(BaseModel):
    session_id: str
    runtime_user_id: str
    runtime_nickname: str | None
    runtime_member_level: str | None
    runtime_risk_level: str | None
    user_message: str
    reasoning_view: Literal["default", "off", "summary", "teaching"] = "default"
    debug: bool = True
    runtime_context: dict[str, Any] | None
    history_messages: list[HistoryMessage]
```

约束：

- `session_id` 和 `runtime_user_id` 必填；
- 身份字段由调用方注入；
- `history_messages` 只允许 user / assistant；
- 用户输入不能覆盖 Runtime Context。

### 7.2 ChatResponse

```python
class ChatResponse(BaseModel):
    session_id: str
    answer: str
    citations: list[Citation]
    tool_calls: list[ToolCallTrace]
    clarification: ClarificationRequest | None
    reasoning_summary: list[str]
    reasoning_content: str | None
    session_state: dict[str, Any]
```

`reasoning_content` 默认不返回。公开内容只通过 `reasoning_summary`。

### 7.3 ToolCallTrace

字段：

- `tool_name`
- `arguments`
- `output_summary`
- `status`
- `tool_source`
- `risk_level`
- `needs_human_approval`
- `next_action`
- `error_type`
- `candidates`
- `clarification_field`
- `clarification_prompt`

### 7.4 TraceEvent

```json
{
  "event_type": "tool_finished",
  "schema_version": "trace_event_v1",
  "category": "tool",
  "stage": "execution",
  "name": "get_order_logistics",
  "status": "success",
  "target": {},
  "ids": {"session_id": "..."},
  "summary": {"latency_ms": 82},
  "signals": ["realtime_fact"],
  "safety": {"tainted": false},
  "payload": {}
}
```

### 7.5 EvalCaseResult

必须返回：

- `passed`
- `actual_answer`
- `actual_tools`
- `actual_citations`
- `actual_trace_events`
- `missing_signals`
- `missing_tools`
- `unexpected_tools`
- `missing_citations`
- `forbidden_citation_hits`
- `missing_trace_events`
- `missing_session_state`
- `forbidden_text_hits`
- `failure_categories`

只返回 `passed=false` 不足以定位问题。

---

## 8. Tool、RAG 与 Workflow 设计

### 8.1 Tool 白名单

白名单是执行权限边界：

```python
ALLOWED_TOOLS = {
    "get_order_detail",
    "get_order_logistics",
    "get_refund_status",
    "search_products",
}
```

模型只能选择工具名，不能定义或动态注册工具。

### 8.2 工具参数解析

```text
模型输出参数
  → 类型校验
  → 必填校验
  → Runtime 订单匹配
  → 候选订单生成
  → 唯一确定 / 需要澄清
```

如果 Runtime 中有多个候选订单，不选择“第一个”，返回 `ClarificationRequest`。

### 8.3 RAG 知识域

| Domain | 文件 | 示例问题 |
|---|---|---|
| `after_sale` | `after_sale_policy.md` | 未发货能否退款 |
| `invoice` | `payment_invoice_policy.md` | 发票多久开好 |
| `promotion` | `promotion_policy.md` | 满减能否叠加 |
| `member` | `member_coupon_policy.md` | 金卡券权益 |
| `return` | `received_return_policy.md` | 七天无理由退货 |

知识域必须显式配置，不能全库无差别检索。

### 8.4 RRF

```text
score = 1 / (k + rank_dense) + 1 / (k + rank_sparse)
```

RRF 只依赖排名，适合融合不同分数量纲的召回结果。

### 8.5 Rerank

当前实现是轻量余弦重排：

```text
query + candidate text
  → embedding
  → cosine similarity
  → ranked candidate ids
```

如果未来替换为 Cross-Encoder 或 LLM Rerank，需要保留：

- 候选数上限；
- 超时；
- 失败回退；
- 公开 rerank 信号。

### 8.6 Workflow 状态

```text
class RefundApprovalState(TypedDict, total=False):
    workflow_id: str
    order_id: str
    runtime_user_id: str
    eligibility: dict
    checkpoint: dict
    approval: dict
    business_recheck: dict
    result: dict
```

状态只保存必要信息，不保存系统 Prompt 和 hidden reasoning。

---

## 9. 安全、Trace 与评估

### 9.1 信任等级

| 来源 | 可信度 | 模型可见 |
|---|---|---:|
| Runtime Context | 1.0 | 部分字段 |
| Tool Fact | 0.95 | 是 |
| Workflow State | 0.95 | 是 |
| RAG Citation | 0.8 | 是 |
| Session Memory | 0.6 | 是 |
| History | 0.4 | 是 |
| User Message | 0.3 | 是 |

用户消息的指令不能覆盖高可信来源。

### 9.2 Source Guard

命中模式：

- 忽略之前指令；
- system / developer prompt；
- 扮演或改写系统；
- 输出 hidden reasoning。

命中后：

```text
tainted = true
sanitized_content = "[tainted-source-redacted]"
trace event = security_blocked
response = avoid protected content
```

### 9.3 Trace 边界

允许：

- route / tool / rag / workflow / hook / cost / security；
- 工具名和参数摘要；
- citation source；
- 状态和耗时；
- risk level。

禁止：

- hidden CoT；
- 系统 Prompt；
- 原始攻击文本；
- PII 和支付信息。

### 9.4 Evaluation 设计

评估不是只跑 answer matching：

```text
Case
  ├── answer signals
  ├── expected_tools
  ├── forbidden_tools
  ├── expected_citations
  ├── expected_trace_events
  ├── expected_session_state
  └── forbidden_text
```

回归失败需要归因到 Prompt、RAG、Tool、Context、Workflow 或 EvaluationExpectation。

### 9.5 Cost Governance

成本摘要维度：

- 路径：light / heavy；
- 模型调用次数；
- Prompt token 估算；
- Observation token 估算；
- 缓存命中；
- 压缩前后差异。

成本摘要不是供应商账单，也不能替代实际 token 账单。

---

## 10. 测试方案

### 10.1 测试金字塔

| 层 | 目标 | 当前入口 |
|---|---|---|
| Contract | Schema、解析、安全、路由辅助 | `python -m unittest discover -s tests -v` |
| Regression | 13 条固定 case | `POST /eval/run` |
| Resume | HITL、token、漂移、幂等 | `cases.yml` |
| Frontend | 编译、类型和构建 | `npm run build` |
| Backend | Java 编译和打包 | `mvn -B -DskipTests package` |
| Smoke | 服务启动、health、核心请求 | Docker Compose |

### 10.2 测试原则

- Unit 测试不访问真实模型；
- 回归 case 必须检查路径，不只检查回答；
- 故障注入必须可重复；
- 离线模式必须显式开启；
- 测试失败必须返回具体缺失项；
- 新增能力必须增加 contract 或 regression case。

### 10.3 需求追踪

```text
spec.md requirement
  → plan.md design item
  → tasks.md task
  → tests/ or cases.yml
  → Trace evidence
```

禁止出现“实现了但没有验收方式”的 P0 需求。

---

## 11. 部署与运行

### 11.1 Docker Compose

```text
frontend :5173
  │
  ├── agent-service :8000
  └── ecommerce-service :8081
          │
          └── mysql :3306

chroma :8001
```

### 11.2 启动

```bash
cp .env.example .env
docker compose up -d --build
docker compose ps
```

### 11.3 健康检查

```text
GET /health
```

### 11.4 回归验证

```bash
curl -s -X POST http://127.0.0.1:8000/eval/run
```

### 11.5 回滚

- Agent 服务可独立回滚；
- Java 后端可独立回滚；
- 数据库迁移必须有 forward 和 rollback 方案；
- Workflow checkpoint 持久化后需要兼容旧版本格式；
- 回滚前必须确认没有正在等待审批的 workflow。

---

## 12. 工程规范

### 12.1 提交前缀

```text
feat(agent):
fix(hitl):
fix(rag):
docs(spec):
test(eval):
chore(repo):
```

### 12.2 变更顺序

1. 更新 spec；
2. 更新 plan；
3. 修改实现；
4. 增加或修改 regression case；
5. 更新 Trace 和文档；
6. 运行本地检查；
7. 提交并观察 CI。

### 12.3 代码边界

- Router 不执行工具；
- Tool 不生成最终回答；
- RAG 不修改业务状态；
- Workflow 不调用任意工具；
- Frontend 不接收 hidden CoT；
- Java 后端不依赖 Agent Prompt。

---

## 13. 交付里程碑

### Phase A · Contract

- 定义 ChatRequest / ChatResponse；
- 定义 RoutePlan / ToolCallTrace / TraceEvent；
- 建立错误语义和风险枚举。

### Phase B · Query Path

- Tool Runtime；
- Hybrid RAG；
- RRF + Rerank；
- Citation；
- 最终回答。

### Phase C · Workflow and HITL

- 未发货退款；
- 签收后退货；
- Checkpoint + Resume Token；
- 业务字段复核；
- 幂等。

### Phase D · Safety and Observability

- Source Guard；
- 公开 Trace；
- 成本摘要；
- 降级和转人工。

### Phase E · Evaluation

- 固定 cases；
- 结构化结果；
- 反馈归因；
- case backfill。

### Phase F · Delivery

- Docker Compose；
- CI；
- README / DEV_SPEC / specs；
- 回滚和验收步骤。

---

## 14. 已知边界

- Workflow checkpoint 和部分状态仍在进程内；
- Source Guard 当前是正则初筛；
- Reranker 是轻量实现；
- 回归集规模有限；
- Agent 与业务后端是跨语言服务，需要联调；
- Mypy 尚未清零；
- 真实资金动作不在系统范围内。

---

## 15. 演进路线

### 15.1 状态持久化

- Checkpoint 存入 PostgreSQL；
- Trace 和 Eval 持久化；
- 多实例共享会话状态；
- 增加状态迁移和版本兼容。

### 15.2 安全增强

- 输入归一化；
- LLM-as-Guard；
- Session 风险积分；
- 跨轮攻击检测；
- 工具输出二次扫描。

### 15.3 检索增强

- Query Rewrite；
- Hybrid Fusion 权重实验；
- Cross-Encoder Reranker；
- 索引版本和蓝绿切换；
- 更大的 Golden Set。

### 15.4 业务治理

- 审批权限矩阵；
- 更细的风险分级；
- 真实工单系统集成；
- 客服质检和人工接管。

### 15.5 可观测性

- OpenTelemetry；
- Metrics 和告警；
- 工具错误率；
- 模型延迟和成本；
- Workflow 暂停时长和审批吞吐。

---

## 16. 术语表

| 术语 | 定义 |
|---|---|
| Runtime Context | 调用方确认的身份、会员、风险和订单上下文 |
| Tool Fact | 从业务后端读取的实时事实 |
| Citation | 由 RAG 命中的知识来源和片段 |
| HITL | Human-in-the-loop，人工审批 |
| Checkpoint | Workflow 暂停时保存的状态快照 |
| Resume Token | 恢复暂停流程的一次性校验凭据 |
| Frozen Field | 进入审批后不可无校验变化的关键字段 |
| Public Trace | 可对外展示的事件记录，不含 hidden CoT |
| Regression Case | 用于防止能力退化的固定测试场景 |
| Failure Attribution | 把失败定位到 Prompt、RAG、Tool、Context、Workflow 或期望值 |