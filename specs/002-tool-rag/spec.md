# Feature Specification: Tool and RAG Authority

| 字段 | 值 |
|---|---|
| Spec ID | `002-tool-rag` |
| Status | implemented |
| Version | 1.0.0 |
| Related modules | `tools/tool_runtime.py`, `rag/`, `integrations/ecommerce_client.py` |
| Regression source | `cases.yml` 的 Tool/RAG/商品/活动 cases |

## 1. 背景

客服问题分为两类事实：

- 实时事实：订单状态、物流、库存、退款状态、会员信息；
- 稳定知识：退款政策、发票规则、活动规则、退货规则。

实时事实必须调用业务系统；稳定规则必须通过 RAG 返回引用。模型不得用参数记忆代替这两类事实。

## 2. 功能需求

| ID | 优先级 | 需求 | 验证 |
|---|---|---|---|
| R-TOOL-01 | P0 | 只读工具必须校验 Runtime 用户和订单归属。 | cross-account tool test |
| R-TOOL-02 | P0 | 缺参或多候选订单不得猜测，必须返回 clarification。 | clarification case |
| R-TOOL-03 | P0 | Agent 不暴露创建退款、修改订单、扣余额等写工具。 | Tool registry contract |
| R-TOOL-04 | P1 | Tool 结果必须记录 arguments、output_summary、status、risk 和 next_action。 | Trace assertions |
| R-RAG-01 | P0 | 政策问题必须返回 citation，不在无依据时生成确定性政策。 | citation cases |
| R-RAG-02 | P0 | 检索按 Knowledge Domain 和 index version 限定范围。 | hybrid search tests |
| R-RAG-03 | P1 | 规则召回先形成候选池，再执行轻量 rerank。 | rerank trace assertions |
| R-RAG-04 | P1 | 低置信结果必须转人工，不得编造规则。 | low-confidence-transfer-boundary |
| R-AUTH-01 | P0 | 工具和 RAG 外部文本均视为不可信输入，进入 Source Guard。 | safety tests |

## 3. Tool 契约

```json
{
  "tool_name": "get_order_logistics",
  "arguments": {"order_id": "SO..."},
  "output_summary": "物流状态：运输中",
  "status": "success",
  "tool_source": "function",
  "risk_level": "low",
  "needs_human_approval": false,
  "next_action": "answer_user"
}
```

## 4. RAG 契约

```json
{
  "source": "promotion_policy.md",
  "title": "618 满减规则",
  "snippet": "...",
  "score": 0.82,
  "retrieval_stage": "pre_retrieval",
  "metadata": {"knowledge_domain": "promotion"}
}
```

## 5. 失败语义

| 场景 | 行为 |
|---|---|
| 工具超时 | 记录 error ToolCall，返回降级答案或转人工 |
| 订单不存在 | 不调用模型猜测，返回未找到 |
| 订单归属不符 | 拒绝读取，记录安全事件 |
| Query 无明确订单号 | 返回候选订单澄清 |
| RAG 无命中 | 明确说明知识库无依据，或转人工 |
| RAG 低置信 | 不生成确定性结论 |

## 6. 验收标准

- AC-01：物流 case 必须调用 `get_order_detail` 和 `get_order_logistics`。
- AC-02：退款状态查询只调用只读工具。
- AC-03：活动/会员问题返回对应 citation。
- AC-04：商品问题和活动规则问题可以在同一回答中分层。
- AC-05：低置信问题触发转人工。
- AC-06：Tool 或 RAG 外部文本不能改变系统策略。
