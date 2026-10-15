# Feature Specification: Agent Runtime Contract

| 字段 | 值 |
|---|---|
| Spec ID | `001-agent-runtime` |
| Status | implemented |
| Version | 1.0.0 |
| Owner | Agent Service |
| Related modules | `agent-service/api`, `agent-service/agents`, `agent-service/context`, `agent-service/observability` |
| Regression source | `agent-service/cases.yml`, `tests/test_agent_contract.py` |

## 1. 背景

Ecom-ai 的 Agent 同时处理普通咨询、订单查询、RAG 问答、退款售后流程、服务降级和 Prompt 攻击。模型可以参与意图判断，但不能替代系统对身份、工具权限、业务状态和审批边界的判断。

Runtime 的职责是把一次 `/chat` 请求转换成可解释、可测试、可追踪的执行结果。

## 2. 目标

- 将调用方提供的可信身份绑定到本轮执行上下文。
- 将 LLM 路由结果约束到业务白名单。
- 在 Tool、RAG、Workflow、降级和安全路径之间选择唯一主路径。
- 返回结构化 `answer`、`citations`、`tool_calls`、`session_state`。
- 产生不包含隐藏推理内容的公开 Trace。

## 3. 非目标

- 不负责真实资金退款。
- 不允许 Agent 直接修改 Java 业务库。
- 不在本规格内承诺多实例共享状态。
- 不把模型自由生成的文本当作业务事实。

## 4. 角色

| 角色 | 职责 |
|---|---|
| 调试前端 | 注入 Runtime Context，展示回答、工具、引用、Trace 和成本 |
| Agent Service | 路由、工具执行、RAG、工作流、安全和响应组装 |
| E-commerce Backend | 提供订单、物流、商品、退款、会员和审批事实 |
| Reviewer | 对高风险售后流程做人工审批 |

## 5. 功能需求

| ID | 优先级 | 需求 | 验证 |
|---|---|---|---|
| R-RUNTIME-01 | P0 | `ChatRequest` 必须包含 `session_id`、`runtime_user_id` 和 `user_message`；身份字段由调用方确认，不能从用户文本推断。 | `tests/test_runtime_context.py` |
| R-RUNTIME-02 | P0 | 订单、退款和用户上下文必须校验 `userId` 归属，禁止跨账号读取。 | `cases.yml:order-logistics-tool-path` |
| R-RUNTIME-03 | P0 | LLM 路由只能产出 `Intent` 枚举、白名单工具和受控 Knowledge Domain。 | `RoutePlan` Schema + 路由回归 case |
| R-RUNTIME-04 | P0 | 高风险退款、退货和安全请求必须走确定性 Guard，不依赖模型单独判断。 | `prompt-injection-trace-boundary`、退款 HITL cases |
| R-RUNTIME-05 | P1 | 一次请求的主执行路径必须写入 `session_state.route`，包括 intent、risk、tools、RAG 和 workflow 状态。 | `/eval/run` 的 `expected_session_state` |
| R-RUNTIME-06 | P1 | 响应必须包含公开 `reasoning_summary`，不得返回 hidden CoT。 | `prompt-injection-trace-boundary` |
| R-RUNTIME-07 | P1 | 形成最终回答前必须完成 Runtime Context、路由、Tool/RAG/Workflow、最终话术和成本事件。 | Trace 事件顺序检查 |
| R-RUNTIME-08 | P2 | 请求失败必须降级为可解释结果或转人工，不能返回供应商异常正文。 | 降级 case + provider stub |

## 6. API 契约

### Request

```json
{
  "session_id": "session-U1001-xxxx",
  "runtime_user_id": "U1001",
  "runtime_nickname": "张三",
  "runtime_member_level": "gold",
  "runtime_risk_level": "low",
  "user_message": "帮我查一下订单物流",
  "reasoning_view": "summary",
  "debug": true,
  "runtime_context": {"currentUserOrders": []},
  "history_messages": []
}
```

### Response

```json
{
  "session_id": "session-U1001-xxxx",
  "answer": "...",
  "citations": [],
  "tool_calls": [],
  "clarification": null,
  "reasoning_summary": ["..."],
  "reasoning_content": null,
  "session_state": {
    "route": {},
    "tool": {},
    "rag": {},
    "workflow": {},
    "trace": {}
  }
}
```

## 7. 执行状态

```text
received
  → runtime_context_built
  → route_decided
  → one_of(
       tool_finished,
       rag_retrieved,
       workflow_paused/workflow_completed,
       degradation_selected,
       security_blocked
     )
  → answer_composed
  → cost_recorded
  → completed
```

任何阶段出现不可恢复错误时进入 `failed`，但响应仍应返回结构化错误摘要。

## 8. 失败语义

| 场景 | 行为 |
|---|---|
| 缺少 session 或身份 | 返回 422，不进入 Agent |
| Runtime 中不存在目标订单 | 返回“未找到可访问订单”，不调用上游猜测 |
| 工具超时或上游 5xx | 进入降级路径，建议转人工 |
| 模型路由不可用 | 使用确定性 fallback 路由 |
| Workflow 等待审批 | 返回 paused + resume token |
| Prompt 攻击 | 阻断受保护内容，公开安全 Trace |

## 9. 安全与隐私

- `runtime_user_id` 和 `risk_level` 属于系统边界字段，不进入模型可见上下文。
- 公开 Trace 只保存路由、工具摘要、RAG 来源、Hook 和成本事件。
- 不记录 hidden reasoning、系统 Prompt 原文和敏感业务字段。

## 10. 性能与容量

| 指标 | 当前目标 |
|---|---:|
| 普通 FAQ RAG P50 | < 2s（不含模型供应商波动） |
| 工具查询额外开销 | < 300ms |
| Trace 事件数 | 每轮 4–12 条 |
| 会话上下文 | 16 条历史消息、压缩阈值 90 token 估算值 |

## 11. 验收标准

- AC-01：`python -m compileall -q agent-service` 通过。
- AC-02：`python -m unittest discover -s tests -v` 全部通过。
- AC-03：`POST /eval/run` 的 13 条固定 case 全部通过。
- AC-04：跨账号订单无法被查询。
- AC-05：Prompt 攻击响应不包含系统 Prompt 或 hidden reasoning。
- AC-06：没有模型 Key 时，非 RAG 主路径仍可返回确定性结果。
