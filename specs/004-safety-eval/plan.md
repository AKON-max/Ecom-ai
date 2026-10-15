# Technical Plan: Safety and Regression Evaluation

| 字段 | 值 |
|---|---|
| Spec | `004-safety-eval` |
| Status | implemented |
| Key modules | `safety/source_guard.py`, `observability/trace.py`, `evals/runner.py`, `feedback/attribution.py` |
| Test entry | `POST /eval/run`, `python -m unittest discover -s tests -v` |

## 1. Source Guard

```text
sources = [
  ("user", request.user_message),
  ("history", history_text),
  ("tool", tool_output),
  ("rag", retrieved_text)
]
  → regex inspection
  → tainted flag
  → sanitized text
  → public safety summary
```

命中模式只保存 category 和 source，不保存原文。

## 2. Evaluation Runner

```text
load cases.yml
  → execute /chat or /chat/resume
  → collect answer
  → collect tool_calls
  → collect citations
  → collect trace events
  → collect session_state
  → compare expectations
  → EvalCaseResult
```

`EvalCaseResult` 必须列出 missing/unexpected/forbidden 的具体项，便于定位。

## 3. Feedback Loop

```text
negative feedback
  → load trace + eval result
  → FailureAttributor
  → build_backfilled_case
  → append runtime case
  → expose in response
```

回填 case 当前仅存在进程内，进程重启后丢失。

## 4. 风险与限制

| 风险 | 缓解 |
|---|---|
| 正则绕过 | 输入归一化 + 后续 LLM Guard 路线图 |
| 攻击原文泄漏 | 只记录 category，不记录内容 |
| case 期望过时 | 变更时同步 spec 和 expected |
| feedback 污染 | 人工评审后再进入永久 case |
| provider 波动 | llm 测试分离 |

## 5. 回滚

回退 evaluation runner 或 source guard 不影响业务后端。若新规则误拦截，可通过配置禁用，但必须保留安全事件可观察性。
