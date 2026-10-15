# Technical Plan: Agent Runtime Contract

| 字段 | 值 |
|---|---|
| Spec | `001-agent-runtime` |
| Status | implemented |
| Depends on | `api/schemas.py`, `agents/customer_service_agent.py`, `tools/runtime_context.py`, `observability/trace.py` |
| Rollback | 回退 Agent 镜像；Java 业务后端与数据库无需变更 |

## 1. 组件边界

| 组件 | 输入 | 输出 | 不得做 |
|---|---|---|---|
| `EcomAiAgent.chat` | `ChatRequest` | `ChatResponse` | 不直接访问 Java 数据库 |
| `RouterClient` | 用户消息 + 受控上下文 | `RoutePlan` | 不绕过工具白名单 |
| `ToolRuntime` | 工具名 + 参数 | `ToolCallTrace` | 不执行写业务动作 |
| `HybridRetrieval` | Query + Domain | citations | 不生成最终话术 |
| `AfterSaleGraph` | 售后意图 + 订单快照 | workflow state | 不执行真实退款 |
| `TraceStore` | 结构化事件 | 公开 Trace | 不保存 hidden CoT |

## 2. 请求时序

```text
POST /chat
  → validate ChatRequest
  → load Runtime Context
  → Source Guard 扫描 history / RAG / tool text
  → deterministic guard
  → router model / deterministic fallback
  → tool / rag / workflow / degradation / security
  → answer model with controlled facts
  → trace + cost
  → ChatResponse
```

## 3. 路由约束

模型只能返回 `Intent`、`required_tools`、`knowledge_domains` 和风险提示。系统在模型输出后执行：

1. Intent 枚举校验；
2. Tool 白名单校验；
3. Runtime Context 订单归属校验；
4. 高风险意图强制切换到 Workflow 或 Guard；
5. Tool 必填参数校验；
6. 需要澄清时返回 `ClarificationRequest`。

## 4. 状态与持久化

当前版本状态保存在进程内：

- `SessionStateStore`
- `TraceStore`
- Workflow checkpoints
- Cost records

限制：多实例部署前必须迁移到 Redis/PostgreSQL 或显式使用 sticky session。

## 5. 风险与回滚

| 风险 | 缓解 |
|---|---|
| 模型路由误判 | 白名单 + 高风险 Guard |
| 工具参数错误 | 后端校验 + clarification |
| Trace 泄露内部推理 | `TraceEventNormalizer` 只保留公开字段 |
| 供应商超时 | 降级转人工 |
| 进程重启丢状态 | 文档声明单实例边界，后续引入持久化 |

回滚策略：回退 Agent 服务版本即可；不需要回滚 Java 业务库。
