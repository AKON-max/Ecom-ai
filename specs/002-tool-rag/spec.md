# Feature Specification: Tool and RAG Authority

| 字段 | 值 |
|---|---|
| Spec ID | `002-tool-rag` |
| Status | implemented |
| Version | 2.0.0 |
| Owner | Tool / Retrieval |
| Scope | 实时业务事实、知识检索、Citation、澄清和失败降级 |
| Related | `specs/001-agent-runtime`, `specs/004-safety-eval` |
| Evidence | `tools/tool_runtime.py`, `integrations/ecommerce_client.py`, `rag/`, `cases.yml` |

## 1. 背景

客服业务事实分为两类：

1. **实时事实**：订单、物流、商品、库存、退款进度、会员和审批状态。
2. **稳定知识**：退款政策、发票规则、活动规则、会员权益和退货规则。

实时事实必须来自业务后端；稳定知识必须来自 RAG。模型不能把参数记忆当事实，也不能在没有 citation 时编造规则。

## 2. 目标

- 为每类问题选择唯一正确的权威来源。
- 防止跨账号订单读取。
- 防止缺参时模型猜测订单。
- 返回可审计的 ToolCallTrace 和 Citation。
- 在 Tool/RAG 故障时给出可解释降级。
- 让检索质量可以通过固定 case 回归。

## 3. 非目标

- Agent 不维护订单和退款状态。
- Agent 不执行写业务动作。
- 当前不实现生产级分布式向量库。
- 当前 Reranker 不是 Cross-Encoder。

## 4. 权限模型

| 能力 | Agent | E-commerce Backend | RAG |
|---|---:|---:|---:|
| 读取订单详情 | 通过 Tool | ✅ | ❌ |
| 读取物流 | 通过 Tool | ✅ | ❌ |
| 查询退款进度 | 通过 Tool | ✅ | ❌ |
| 修改退款状态 | ❌ | 受审批控制 | ❌ |
| 解释退款政策 | 通过 RAG | ❌ | ✅ |
| 解释活动规则 | 通过 RAG | ❌ | ✅ |

## 5. 功能需求

### 5.1 Tool

| ID | 优先级 | 需求 | 验证 |
|---|---|---|---|
| R-TOOL-01 | P0 | 工具必须通过固定白名单注册。 | registry contract |
| R-TOOL-02 | P0 | 工具调用前必须校验 Runtime 用户归属。 | cross-account case |
| R-TOOL-03 | P0 | 缺参或多候选订单不得猜测，必须 clarification。 | clarification case |
| R-TOOL-04 | P0 | 自动执行工具必须为只读。 | tool registry review |
| R-TOOL-05 | P1 | Tool 结果必须记录 arguments、output_summary、status、risk、error_type、next_action。 | trace assertions |
| R-TOOL-06 | P1 | 业务 API timeout/5xx 必须进入降级。 | degradation case |

### 5.2 RAG

| ID | 优先级 | 需求 | 验证 |
|---|---|---|---|
| R-RAG-01 | P0 | 知识查询必须限定 Knowledge Domain 和索引版本。 | hybrid search tests |
| R-RAG-02 | P0 | 回答必须返回 source、title、snippet 和 score。 | citation cases |
| R-RAG-03 | P0 | 无命中或低置信时不得编造规则。 | low-confidence case |
| R-RAG-04 | P1 | Dense 和 Sparse 召回通过 RRF 融合。 | fusion tests |
| R-RAG-05 | P1 | Rerank 必须可关闭并失败回退。 | rerank fallback tests |
| R-RAG-06 | P1 | 离线和真实 Embedding 模式必须公开区分。 | offline mode test |

### 5.3 安全

| ID | 优先级 | 需求 | 验证 |
|---|---|---|---|
| R-AUTH-01 | P0 | Tool / RAG / History 文本均作为不可信输入。 | source guard |
| R-AUTH-02 | P0 | 工具参数中的 order_id 必须与 Runtime 订单匹配。 | cross-account case |
| R-AUTH-03 | P0 | RAG 低置信不得触发自动业务动作。 | workflow boundary |

## 6. Tool Catalog

### 6.1 get_order_detail

```json
{
  "tool_name": "get_order_detail",
  "arguments": {"order_id": "SO20260602103000009-a1000009"},
  "status": "success",
  "risk_level": "low",
  "next_action": "answer_user"
}
```

校验：

1. 订单号格式；
2. Runtime 用户匹配；
3. 若缺失，返回候选订单；
4. 若无候选，返回未找到。

### 6.2 get_order_logistics

输入订单快照，输出物流事件、当前状态和预计送达信息。该工具不接受用户自报物流单号。

### 6.3 get_refund_status

只读查询退款申请和审批状态。不得把它用于修改退款状态。

### 6.4 search_products

按关键词或商品 ID 查询商品、价格、库存和活动。商品事实不能由模型生成。

## 7. RAG Pipeline

```text
Query
  → QueryProcessor
  → domain / version filter
  → keyword retrieval
  → dense retrieval
  → RRF
  → candidate pool
  → lightweight rerank
  → Citation assembly
```

### 7.1 Domain

| Domain | 典型问题 |
|---|---|
| `after_sale` | 未发货退款 |
| `invoice` | 发票准备时间 |
| `promotion` | 满减叠加 |
| `member` | 金卡券 |
| `return` | 七天无理由退货 |

### 7.2 分数与排序

- Dense：cosine similarity。
- Sparse：BM25 / keyword score。
- Fusion：RRF，不直接相加不同量纲分数。
- Rerank：当前为轻量 embedding cosine。

Rerank 失败时回退 RRF Top-K，不允许整个 RAG 请求失败。

## 8. Citation 契约

```json
{
  "source": "promotion_policy.md",
  "title": "618 满减规则",
  "snippet": "满减与会员券的叠加规则...",
  "score": 0.82,
  "retrieval_stage": "pre_retrieval",
  "metadata": {"knowledge_domain": "promotion"}
}
```

约束：

- `source` 必须来自索引文件；
- `snippet` 必须来自命中 chunk；
- `score` 必须来自实际检索或 rerank；
- 不允许模型自由生成 citation。

## 9. 失败语义

| 场景 | 行为 |
|---|---|
| Runtime 归属失败 | 安全阻断 |
| 订单不存在 | 明确未找到 |
| 多候选订单 | clarification |
| Tool timeout | 降级转人工 |
| RAG 无命中 | 说明无依据 |
| RAG 低置信 | 转人工 |
| Rerank 失败 | 回退 RRF |
| Embedding 不可用 | 显式离线/降级模式 |

## 10. 验收标准

- AC-01：物流 case 调用订单和物流工具。
- AC-02：退款进度查询不调用写工具。
- AC-03：活动/会员问题返回 citation。
- AC-04：跨账号订单无法读取。
- AC-05：缺参订单触发 clarification。
- AC-06：低置信触发转人工。
- AC-07：13 条回归 case 中所有 Tool/RAG 检查通过。

## 11. 需求追踪

| 需求 | 计划 | 任务 | 测试 |
|---|---|---|---|
| R-TOOL-01 | §2 | T-TOOL-01 | registry contract |
| R-TOOL-02 | §3 | T-TOOL-02 | cross-account |
| R-TOOL-03 | §4 | T-TOOL-03 | clarification |
| R-RAG-01 | §5 | T-RAG-01 | hybrid tests |
| R-RAG-02 | §6 | T-RAG-02 | citation cases |
| R-RAG-03 | §7 | T-RAG-03 | low confidence |
| R-AUTH-01 | §8 | T-SAFE-01 | source guard |