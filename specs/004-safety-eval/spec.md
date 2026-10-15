# Feature Specification: Safety and Regression Evaluation

| 字段 | 值 |
|---|---|
| Spec ID | `004-safety-eval` |
| Status | implemented |
| Version | 1.0.0 |
| Key modules | `safety/source_guard.py`, `observability/trace.py`, `evals/runner.py`, `feedback/attribution.py` |
| Regression source | `agent-service/cases.yml` 13 条固定 case |

## 1. 背景

Agent 的输入可能来自用户、Tool、RAG 和历史消息。任何外部文本都可能包含指令覆盖、隐私提取或策略诱导。与此同时，模型和 Prompt 调整可能导致既有能力退化。

因此系统需要同时具备安全边界和回归证据。

## 2. 功能需求

| ID | 优先级 | 需求 | 验证 |
|---|---|---|---|
| R-SAFE-01 | P0 | 扫描 user、tool、RAG、history 四路输入。 | `test_safety.py` |
| R-SAFE-02 | P0 | 命中注入模式的内容必须标记 tainted 并从模型输入脱敏。 | `prompt-injection-trace-boundary` |
| R-SAFE-03 | P0 | 公开 Trace 不得包含 hidden CoT、系统 Prompt 或敏感字段。 | Trace schema assertions |
| R-SAFE-04 | P0 | 安全请求必须拒绝泄露，并说明无法提供内部信息。 | forbidden_text assertions |
| R-EVAL-01 | P0 | 每个 case 必须检查 answer signals。 | `/eval/run` |
| R-EVAL-02 | P0 | 每个 case 可检查 expected / forbidden tools。 | `/eval/run` |
| R-EVAL-03 | P0 | 每个 case 可检查 citations、trace events 和 session_state。 | `/eval/run` |
| R-EVAL-04 | P1 | 反馈可归因到 Prompt、RAG、Tool、Context、Workflow 或期望值。 | `feedback/submit` |
| R-EVAL-05 | P1 | 失败反馈可回填为临时 regression case。 | `feedback_submit` |

## 3. 安全事件契约

```json
{
  "event_type": "security_blocked",
  "schema_version": "trace_event_v1",
  "category": "safety",
  "stage": "source_guard",
  "status": "blocked",
  "signals": ["instruction_override"],
  "safety": {
    "tainted": true,
    "sources": ["user"],
    "redacted": true
  },
  "payload": {}
}
```

安全事件不能保存攻击原文。

## 4. 回归评测设计

每条 case 包含：

- `case_id`
- `user_message` 或 `start_user_message`
- `runtime_context`
- `expected_signals`
- `expected_tools` / `forbidden_tools`
- `expected_citations` / `forbidden_citation_hits`
- `expected_trace_events`
- `expected_session_state`
- `forbidden_text`

回归不只看最终回答，还检查执行路径。

## 5. 失败归因

| 模块 | 典型问题 |
|---|---|
| Prompt | 回答风格、边界说明缺失 |
| RAG | 召回错误、citation 缺失 |
| Tool | 参数错误、超时、工具选择错误 |
| Context | 信任级别、冲突处理、压缩丢失 |
| Workflow | HITL、resume、幂等错误 |
| EvaluationExpectation | case 期望本身不合理 |

## 6. 验收标准

- AC-01：注入请求返回 blocked，且响应不含系统 Prompt。
- AC-02：攻击原文不进入公开 Trace。
- AC-03：13 条固定 case 可单独运行，也可全量运行。
- AC-04：失败结果返回结构化缺失项，不只返回 passed=false。
- AC-05：负面反馈生成 attribution 和 backfilled case。
- AC-06：unit tests 不访问真实模型。

## 7. 当前局限

- Source Guard 当前是正则初筛，不是 LLM 语义 Guard。
- 回归集规模较小，不能代表真实流量分布。
- 失败归因采用规则和结构化信号，不是训练模型。
