# Developer Specification (DEV_SPEC)

> 本文档描述 Ecom-ai 的设计目标、技术选择、核心链路、模块职责、测试方案和演进边界。
> 代码是最终事实来源；若实现与本文档冲突，先以代码和测试调查，再同步修订文档。

## 目录

1. 项目概述
2. 设计理念
3. 核心能力
4. 技术选型
5. 系统架构与模块设计
6. 关键业务流程
7. 数据与安全模型
8. 测试方案
9. 部署与运行
10. 已知边界与演进路线

---

## 1. 项目概述

### 1.1 项目定位

Ecom-ai 是一个面向电商售后场景的客服 Agent 系统，目标是把自然语言对话、实时业务事实、知识检索、工具调用、售后工作流、人工审批、Trace 和评估组织成一条可控的生产级链路。

系统不是单纯的聊天机器人，也不是让模型直接操作数据库。它采用显式边界：

- Python Agent 负责路由、编排、RAG、Tool、Workflow、安全和回答组装。
- Java 业务后端负责订单、物流、商品、退款、审批和会员事实。
- React 调试后台负责展示回答、引用、工具、Trace、评估和成本。

### 1.2 设计目标

- **实时事实可信**：订单和物流不能由模型猜测。
- **只读默认**：Agent 自动执行的工具默认只读。
- **高风险动作受控**：退款、退货和资金动作必须经过资格判断和人工审批。
- **知识回答可引用**：政策和 FAQ 回答必须携带 citation。
- **执行过程可解释**：公开 route、tool、rag、workflow、hook、cost 和 security 事件。
- **质量可回归**：固定 case 同时检查答案和执行路径，而不是只检查 HTTP 200。
- **组件可替换**：模型、Embedding、Reranker、向量库和知识文件通过配置和工厂切换。

### 1.3 设计原则

#### 事实来源优先级

```text
Runtime Context
  > Verified Tool Fact
  > Workflow State
  > RAG Citation
  > Session Memory
  > Conversation History
  > User Message
```

用户文本无法覆盖系统身份、订单归属、工具权限和审批边界。

#### 模型参与但不能裁决

模型可以判断意图、选择工具候选和组织语言，但不能：

- 修改 Runtime 用户身份；
- 跳过工具白名单；
- 直接执行资金动作；
- 编造订单状态或政策依据；
- 返回 hidden chain-of-thought。

#### 只暴露公开 Trace

系统记录可以用于调试和审计的事件，但不记录系统 Prompt 原文、隐藏推理、完整支付信息和敏感个人字段。

---

## 2. 核心能力

### 2.1 Intent Routing

支持的主要意图：

- `general_chat`
- `order_query`
- `refund_status_query`
- `refund_request`
- `return_request`
- `faq_query`
- `promotion_query`
- `product_query`
- `low_confidence_query`
- `degradation_request`
- `security_request`

路由输出为 `RoutePlan`，包括：

- `intent`
- `needs_rag`
- `needs_business_tools`
- `required_tools`
- `tool_candidates`
- `knowledge_domains`
- `entity_refs`
- `risk_level`
- `requires_workflow`
- `confidence`
- `source`：模型约束路由或确定性 fallback

### 2.2 Business Tools

只读工具提供实时事实：

- `get_order_detail`
- `get_order_logistics`
- `get_refund_status`
- `search_products`

工具调用前后执行：

1. Runtime 订单归属校验；
2. 参数完整性校验；
3. 缺参澄清；
4. 超时和错误分类；
5. ToolCallTrace 记录；
6. Observation 压缩。

### 2.3 Hybrid RAG

知识范围包括：

- 退款政策；
- 发票规则；
- 活动和会员规则；
- 退货政策。

检索链路：

```text
query
  → pre-retrieval
  → keyword scoring
  → embedding similarity
  → domain / version filter
  → candidate pool
  → lightweight reranker
  → citation assembly
```

低置信结果不生成确定性政策，直接转人工。

### 2.4 Workflow 与 HITL

两条 LangGraph 工作流：

- 未发货退款；
- 签收后退货。

流程在 `require_approval` 暂停，返回 `resume_token`。恢复时校验：

- workflow checkpoint；
- resume token；
- 当前 Runtime 身份；
- 冻结业务字段；
- idempotency key。

冻结字段漂移时返回 `blocked`，不继续执行。

### 2.5 Safety Guard

安全输入来源：

- user message
- tool output
- RAG text
- history

处理原则：

- 正则初筛；
- tainted 标记；
- 攻击原文脱敏；
- 公开安全事件；
- 不泄露系统 Prompt 和 hidden reasoning；
- 跨轮语义攻击和安全模型是后续增强方向。

### 2.6 Trace、Evaluation 与 Cost

Trace 类型：

- `route`
- `tool`
- `rag`
- `hook`
- `workflow`
- `cost`
- `security`

评估检查：

- answer signals；
- expected / forbidden tools；
- citations；
- trace events；
- session state；
- workflow state；
- forbidden text。

成本摘要区分轻路径、重路径、缓存命中、Prompt 片段和 Observation 压缩。

---

## 3. 技术选型

| 层 | 技术 | 选择理由 | 代价 |
|---|---|---|---|
| Agent 服务 | FastAPI | 类型校验、异步 HTTP、OpenAPI 和 Python AI 生态 | 需要自行管理线程和状态边界 |
| Workflow | LangGraph StateGraph | 显式状态、暂停恢复、适合 HITL | 需要理解图和 checkpoint 模型 |
| 模型调用 | OpenAI-compatible API | 可替换供应商 | 输出波动和限流 |
| RAG | 混合检索 + RRF + 轻量 rerank | 兼顾语义和关键词 | 参数增加，需回归评估 |
| 业务后端 | Spring Boot | 完整电商业务、事务和权限模型 | Java 代码量较大 |
| 数据库 | MySQL | 业务事实持久化 | Agent 不直接访问 |
| 前端 | React + TypeScript + Vite | 调试面板开发效率高 | 需要维护 API 类型 |
| 测试 | unittest + 固定 YAML cases | 离线可运行、结构清晰 | case 规模仍需扩大 |

### 3.1 为什么不让 Agent 直连数据库

- 数据库权限边界过大；
- 业务状态机应在业务服务内维护；
- Tool Schema 比任意 SQL 更可控；
- 用户归属和服务身份更容易验证；
- 业务后端可以独立演进和回滚。

### 3.2 为什么采用 HITL

退款和退货涉及资金或售后权益。模型可以判断资格和收集事实，但不能替代审批。HITL 同时提供审计证据和幂等恢复边界。

---

## 4. 系统架构与模块设计

### 4.1 整体架构

```text
React Debug Console
   │
   ├── Chat / Resume / Trace / Eval ──► Agent Service
   │                                      │
   │                                      ├── Intent Router
   │                                      ├── Tool Runtime
   │                                      ├── Hybrid RAG
   │                                      ├── LangGraph Workflow
   │                                      ├── Safety Guard
   │                                      ├── Trace / Eval / Cost
   │                                      └── Model Providers
   │
   └── Order / Logistics / Refund ─────► E-commerce Backend ──► MySQL
```

### 4.2 Agent Service 目录

| 路径 | 职责 |
|---|---|
| `api/routes.py` | Chat、Resume、Trace、Eval、Feedback 路由 |
| `api/schemas.py` | Pydantic 请求响应契约 |
| `agents/customer_service_agent.py` | Agent 主编排 |
| `tools/tool_runtime.py` | 只读业务工具 |
| `tools/planning.py` | 订单号提取、路由辅助 |
| `rag/hybrid_retrieval.py` | 混合召回、RRF、rerank |
| `rag/documents.py` | 知识文件读取和 Citation 转换 |
| `workflows/after_sale_graph.py` | 退款和退货 StateGraph |
| `workflows/resume.py` | Token、冻结字段、幂等恢复 |
| `safety/source_guard.py` | 四路输入扫描 |
| `observability/trace.py` | 公开 Trace |
| `evals/runner.py` | 固定 case 回归 |
| `feedback/attribution.py` | 失败归因和 case backfill |
| `cost/governance.py` | 路径分级和成本摘要 |

### 4.3 Java 后端职责

- 订单和订单项
- 物流和物流事件
- 商品、库存和活动
- 用户、会员和优惠券
- 退款、退货和售后申请
- 人工审批记录
- 服务身份鉴权
- Runtime 订单上下文

Java 后端不包含 Agent Prompt、RAG 和模型调用。

### 4.4 前端职责

- 注入 Runtime Context
- 选择演示用户和问题
- 展示回答、tool_calls、citations
- 展示公开 Trace
- 展示 Eval 结果
- 发起 Resume 审批
- 提交反馈

前端不接收 hidden CoT 和系统 Prompt。

---

## 5. 关键业务流程

### 5.1 物流查询

```text
用户问题
  → 提取订单号
  → Runtime 归属校验
  → get_order_detail
  → get_order_logistics
  → 压缩 Observation
  → 模型生成客服话术
  → Trace
```

不调用 RAG，因为物流是实时业务事实。

### 5.2 政策问答

```text
用户问题
  → intent = promotion_query / faq_query
  → 选择 knowledge domain
  → 混合召回
  → RRF
  → rerank
  → citation
  → 模型回答
```

没有命中或低置信时转人工，不编造规则。

### 5.3 未发货退款

```text
订单查询
  → 状态 = PENDING_SHIPMENT
  → 退款资格检查
  → 创建 checkpoint
  → require_approval
  → 返回 resume_token
```

审批恢复时重新读取订单并校验冻结字段。

### 5.4 服务降级

```text
Tool timeout / upstream 5xx
  → 错误分类
  → 只读重试（受控）
  → 降级话术
  → transfer_to_human
  → Trace
```

### 5.5 Prompt 注入

```text
四路输入
  → Source Guard
  → tainted
  → 脱敏
  → security_blocked
  → 安全拒绝
```

---

## 6. 数据与安全模型

### 6.1 信任边界

| 数据 | 可信来源 | 是否进入模型可见上下文 |
|---|---|---|
| runtime_user_id | 调用方 | 否 |
| risk_level | 调用方 | 否 |
| 订单事实 | Java 后端 | 是，经过摘要 |
| RAG 文本 | 知识库 | 是，带 citation |
| 用户消息 | 用户 | 是，作为问题 |
| Tool 输出 | 业务 API | 是，作为 Observation |

### 6.2 禁止记录

- hidden CoT
- 系统 Prompt 原文
- Token 和密钥
- 手机号、地址、支付标识
- 攻击原文

### 6.3 幂等

- Focus / Tool 重试使用请求级幂等；
- Resume 使用 workflow id、approval id 或冻结字段哈希；
- 审批重复提交必须返回第一次结果。

---

## 7. 测试方案

### 7.1 测试分层

| 层 | 范围 | 入口 |
|---|---|---|
| Contract | Schema、路由、安全和 Runtime Context | `python -m unittest discover -s tests -v` |
| Regression | 13 条固定 case | `POST /eval/run` |
| Resume | 授权、拒绝、无效 token、缺少 checkpoint、幂等 | `cases.yml` |
| Frontend | TypeScript 编译和 Vite 构建 | `npm run build` |
| Backend | Java 编译和打包 | `mvn -B -DskipTests package` |

### 7.2 回归 case 设计

每条 case 必须同时检查：

- 最终 answer signals；
- expected tools；
- forbidden tools；
- expected citations；
- trace events；
- session state；
- workflow state；
- forbidden text。

只检查最终话术会漏掉“答案碰巧正确但路径错误”的退化。

### 7.3 离线能力

- `AGENT_DISABLE_LLM=1`：禁用模型，保留确定性路径。
- `AGENT_OFFLINE_FACTS=1`：允许使用与业务种子一致的镜像事实。
- `AGENT_OFFLINE_RAG=1`：允许使用字符向量进行离线回归。

离线模式必须公开当前模式，不能伪装成真实语义检索或真实业务 API。

---

## 8. 部署与运行

### 8.1 Docker Compose

服务：

- `agent-service`
- `ecommerce-service`
- `frontend`
- `mysql`
- `chroma`

端口：

| 服务 | 端口 |
|---|---:|
| Agent | 8000 |
| E-commerce | 8081 |
| Frontend | 5173 |
| MySQL | 3306 |
| Chroma | 8001 |

### 8.2 启动

```bash
cp .env.example .env
docker compose up -d --build
```

### 8.3 验证

```bash
curl -s http://127.0.0.1:8000/health
curl -s -X POST http://127.0.0.1:8000/eval/run
```

### 8.4 回滚

- Agent 服务可以独立回滚；
- Java 业务后端可以独立回滚；
- 数据库迁移必须提供 downgrade 或恢复方案；
- Workflow checkpoint 持久化落地后，需要版本兼容策略。

---

## 9. 已知边界

- Workflow checkpoint、Trace 和部分状态仍在进程内。
- Source Guard 当前是正则初筛，不是完整语义防御。
- 固定回归集规模有限。
- Mypy 尚未清零，不是当前 CI 阻断项。
- 真实资金动作不在系统范围内。
- Java 和 Python 的最终一致性依赖业务 API、重试和幂等设计。

---

## 10. 演进路线

### 10.1 状态和恢复

- Checkpoint 持久化到 PostgreSQL；
- Trace 和 Eval 结果持久化；
- 多实例共享会话状态。

### 10.2 安全

- 输入归一化；
- LLM-as-Guard；
- Session 风险积分；
- 跨轮攻击检测。

### 10.3 RAG

- 生产级向量库；
- 更强 Reranker；
- Query Rewrite 和 Fusion；
- 索引版本和蓝绿切换。

### 10.4 可观测性

- 统一 Metrics、Tracing 和告警；
- Tool 延迟和错误率；
- 模型供应商错误率；
- Workflow 暂停时长和审批吞吐。

### 10.5 评估

- 扩大 Golden Set；
- 增加跨文档和复杂多跳问题；
- 将回归评估接入 CI/CD；
- 对失败 case 建立自动归因报告。