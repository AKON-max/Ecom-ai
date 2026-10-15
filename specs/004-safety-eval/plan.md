# Technical Plan: Safety and Regression Evaluation

| 字段 | 值 |
|---|---|
| Spec | `004-safety-eval` |
| Status | implemented |
| Modules | `safety/source_guard.py`, `observability/trace.py`, `evals/runner.py`, `feedback/attribution.py` |
| Regression | `agent-service/cases.yml` |
| Rollback | 回退规则或 eval runner，不涉及业务数据库 |

## 1. Source Guard 架构

```text
Input Sources
  ├── user_message
  ├── history_messages
  ├── tool_output
  └── rag_text
        │
        ▼
  normalize + regex scan
        │
        ├── clean → model context
        └── tainted → redact → security event → safe context
```

### 检测项

| Category | Pattern |
|---|---|
| instruction_override | 忽略之前/以上/所有指令 |
| system_prompt_request | system/developer/prompt |
| role_override | 你现在必须/扮演/改写系统 |
| hidden_reasoning_request | 输出隐藏推理/内部策略 |
| prompt_exfiltration | 把系统提示词发给我 |

## 2. Trace Normalizer

职责：

- 统一 `trace_event_v1`；
- 过滤禁止字段；
- 聚合耗时的公开摘要；
- 给安全事件附加 `safety`；
- 不修改业务状态。

禁止字段：

```text
chain_of_thought
hidden_reasoning
system_prompt
raw_user_input
payment_token
phone
address
```

## 3. Evaluation Runner

```text
load cases.yml
  → select case or full set
  → execute /chat or /chat/resume
  → collect actual outputs
  → compare each expectation group
  → build EvalCaseResult
  → summary by failure category
```

比较维度：

- signals；
- tools；
- citations；
- trace events；
- session state；
- forbidden text；
- workflow state。

## 4. Failure Attribution

输入：

- FeedbackRequest
- trace events
- EvalCaseResult

输出：

- module
- category
- evidence
- suggested_fix

模块枚举：

```text
Prompt
RAG
Tool
Context
Workflow
EvaluationExpectation
```

## 5. Case Backfill

```text
negative feedback
  → base case lookup
  → attribution
  → build_backfilled_case
  → append runtime case
  → return FeedbackRecord
```

当前 backfill 只存在进程内，未自动合并到 `cases.yml`。进入永久回归集前必须人工评审。

## 6. 风险与限制

| 风险 | 处理 |
|---|---|
| 正则绕过 | 后续输入归一化 + LLM Guard |
| 误杀正常文本 | 只在明确模式命中时 tainted |
| 评估期望过时 | 更新 case 时同步 spec |
| provider 波动 | `llm` 测试分离 |
| 反馈污染 | 人工审核后再固化 |
| 进程重启丢失 backfill | 当前限制，后续持久化 |

## 7. 验证

- tainted / clean 单元测试；
- Trace forbidden fields；
- injection case；
- 13 条回归；
- feedback attribution；
- backfilled case。

## 8. 回滚

规则和 eval runner 可以独立回滚。如果新规则误拦截正常生产流量，应先降低规则范围或将规则降级为 observation，同时保留安全事件记录。