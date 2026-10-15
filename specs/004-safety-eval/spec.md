# Feature Specification: Safety and Regression Evaluation

| 字段 | 值 |
|---|---|
| Spec ID | `004-safety-eval` |
| Status | implemented |
| Version | 2.0.0 |
| Owner | Safety / Evaluation |
| Related | `specs/001-agent-runtime`, `specs/002-tool-rag`, `specs/003-hitl-workflow` |
| Evidence | `safety/source_guard.py`, `observability/trace.py`, `evals/runner.py`, `feedback/attribution.py`, `cases.yml` |

## 1. 背景

Agent 输入来自多个不可信来源：用户、Tool、RAG 和历史消息。模型调用还受 Prompt、模型版本和供应商行为影响。

因此系统必须同时具备：

- 输入安全边界；
- 公开 Trace 边界；
- 固定回归评估；
- 失败归因；
- 可控的 case 回填。

## 2. 安全目标

- 防止指令覆盖和系统 Prompt 提取。
- 防止 Tool/RAG 文本伪装成系统指令。
- 防止 hidden CoT 对外暴露。
- 防止敏感字段进入公开 Trace。
- 安全失败时不执行高风险动作。

## 3. 评估目标

- 检测回答退化。
- 检测工具选择退化。
- 检测 Citation 退化。
- 检测 Workflow/HITL 退化。
- 检测 session_state 和 Trace 契约变化。
- 将用户反馈转化为可复现输入。

## 4. 功能需求

### 4.1 Source Guard

| ID | 优先级 | 需求 | 验证 |
|---|---|---|---|
| R-SAFE-01 | P0 | 扫描 user、tool、rag、history 四路输入。 | test_safety |
| R-SAFE-02 | P0 | 命中注入模式必须标记 tainted。 | injection case |
| R-SAFE-03 | P0 | 可疑文本必须脱敏后进入模型。 | sanitized content test |
| R-SAFE-04 | P0 | Trace 不保存攻击原文。 | trace assertion |
| R-SAFE-05 | P1 | 安全事件必须包含 source、category、status。 | Trace schema |

### 4.2 Trace

| ID | 优先级 | 需求 | 验证 |
|---|---|---|---|
| R-TRACE-01 | P0 | Trace schema 固定为 `trace_event_v1`。 | schema test |
| R-TRACE-02 | P0 | 公开事件类型限定为 route/tool/rag/workflow/hook/cost/security。 | schema assertions |
| R-TRACE-03 | P0 | 不包含 hidden CoT、系统 Prompt、PII 和支付信息。 | forbidden fields test |
| R-TRACE-04 | P1 | 事件包含 category、stage、status、target、ids、summary、signals、safety、payload。 | TraceEvent contract |

### 4.3 Evaluation

| ID | 优先级 | 需求 | 验证 |
|---|---|---|---|
| R-EVAL-01 | P0 | case 必须检查 answer signals。 | /eval/run |
| R-EVAL-02 | P0 | case 支持 expected / forbidden tools。 | /eval/run |
| R-EVAL-03 | P0 | case 支持 citations、trace events 和 session_state。 | /eval/run |
| R-EVAL-04 | P0 | 失败结果必须返回具体缺失项。 | EvalCaseResult |
| R-EVAL-05 | P1 | 支持单 case 和全量运行。 | EvalRunRequest |
| R-EVAL-06 | P1 | 支持 Workflow start + resume case。 | resume cases |

### 4.4 Feedback

| ID | 优先级 | 需求 | 验证 |
|---|---|---|---|
| R-FEEDBACK-01 | P1 | 负面反馈绑定 session_id、case_id、answer 和 comment。 | FeedbackRequest |
| R-FEEDBACK-02 | P1 | 可归因到 Prompt/RAG/Tool/Context/Workflow/EvaluationExpectation。 | FailureAttribution |
| R-FEEDBACK-03 | P1 | 可回填为临时 regression case。 | FeedbackRecord |

## 5. Source Guard 处理流程

```mermaid
flowchart LR
    U[User] --> SG[Source Guard]
    T[Tool Output] --> SG
    R[RAG Text] --> SG
    H[History] --> SG
    SG -->|clean| M[Model Context]
    SG -->|tainted| X[Redact]
    X --> S[security event]
    S --> M2[Safe Context]
```

命中模式当前包括：

- 忽略之前/以上/所有指令；
- system / developer prompt；
- 扮演或改写系统；
- 输出隐藏/内部推理；
- 请求系统提示词。

## 6. TraceEvent 契约

```json
{
  "event_type": "security_blocked",
  "schema_version": "trace_event_v1",
  "category": "security",
  "stage": "source_guard",
  "name": "instruction_override",
  "status": "blocked",
  "target": {},
  "ids": {"session_id": "..."},
  "summary": {"redacted": true},
  "signals": ["tainted_source"],
  "safety": {
    "tainted": true,
    "sources": ["user"],
    "redacted": true
  },
  "payload": {}
}
```

禁止字段：

- chain_of_thought
- hidden_reasoning
- system_prompt
- raw_user_input（攻击原文）
- phone / address / payment_token

## 7. Evaluation Case 契约

```yaml
- case_id: unshipped-refund-hitl
  user_message: SO... 还没发货，我现在能退款吗？
  runtime_context:
    currentUserOrders: []
  expected_signals:
    - 人工审批
    - require_approval
  expected_tools:
    - get_order_detail
  expected_citations:
    - refund_before_shipping
  expected_trace_events:
    - workflow_completed
    - human_approval_required
  expected_session_state:
    - workflow.pending_action=require_approval
  forbidden_text:
    - 已退款成功
```

## 8. 失败归因

| 模块 | 证据 | 示例 |
|---|---|---|
| Prompt | 回答缺少边界说明 | 高风险动作没有解释审批 |
| RAG | citation 缺失或来源错误 | 检索到错误活动规则 |
| Tool | 参数、状态或调用错误 | 订单号解析失败 |
| Context | 信任等级或压缩错误 | Runtime 身份丢失 |
| Workflow | checkpoint、resume 或幂等错误 | token 漂移未阻断 |
| EvaluationExpectation | case 期望本身不合理 | expected_signal 拼写错误 |

## 9. 验收标准

- AC-01：注入文本被标记为 tainted。
- AC-02：响应不包含系统 Prompt 和 hidden CoT。
- AC-03：公开 Trace 不含禁止字段。
- AC-04：13 条固定 case 可运行。
- AC-05：失败结果返回具体缺失项。
- AC-06：负面反馈产生归因和 backfilled case。
- AC-07：unit tests 不访问真实模型。

## 10. 需求追踪

| 需求 | 计划 | 任务 | 测试 |
|---|---|---|---|
| R-SAFE-01 | §2 | T-SAFE-01 | test_safety |
| R-SAFE-02 | §2 | T-SAFE-02 | injection case |
| R-SAFE-03 | §2 | T-SAFE-03 | sanitized content |
| R-TRACE-01 | §3 | T-TRACE-01 | schema test |
| R-TRACE-02 | §3 | T-TRACE-02 | trace assertions |
| R-EVAL-01 | §4 | T-EVAL-01 | /eval/run |
| R-EVAL-02 | §4 | T-EVAL-02 | tools assertions |
| R-EVAL-03 | §4 | T-EVAL-03 | citations/state |
| R-FEEDBACK-01 | §5 | T-FEEDBACK-01 | feedback submit |
| R-FEEDBACK-02 | §5 | T-FEEDBACK-02 | attribution |