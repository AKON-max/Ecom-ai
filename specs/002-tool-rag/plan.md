# Technical Plan: Tool and RAG Authority

| 字段 | 值 |
|---|---|
| Spec | `002-tool-rag` |
| Status | implemented |
| Key modules | `tools/tool_runtime.py`, `tools/planning.py`, `integrations/ecommerce_client.py`, `rag/hybrid_retrieval.py`, `rag/documents.py` |
| External dependencies | Spring Boot E-commerce API、Embedding API、可选 Reranker |
| Rollback | 回退 Agent 服务；知识索引可恢复旧版本 |

## 1. Tool Runtime

```text
RoutePlan.required_tools
  → tool whitelist lookup
  → runtime user/order resolution
  → argument schema validation
  → clarification if needed
  → E-commerce API request
  → error classification
  → ToolCallTrace
```

### 参数解析优先级

1. 从 Runtime Context 订单列表匹配 `order_id`；
2. 从用户文本提取订单号；
3. 与 Runtime 订单做精确匹配；
4. 唯一命中后执行；
5. 多候选或零候选直接返回澄清。

禁止选择“第一个订单”作为默认。

## 2. E-commerce Client

职责：

- 统一 Base URL；
- 服务身份 Header；
- timeout；
- HTTP 状态分类；
- 响应结构解析；
- 离线镜像开关；
- 业务错误转换为 Tool error。

| 错误 | 分类 |
|---|---|
| connection error | `SERVICE_UNAVAILABLE` |
| timeout | `SERVICE_TIMEOUT` |
| 401 / 403 | `SERVICE_AUTH_FAILED` |
| 404 | `ORDER_NOT_FOUND` |
| 5xx | `UPSTREAM_ERROR` |

## 3. RAG Pipeline

```text
load knowledge documents
  → split / metadata
  → dense embedding
  → sparse index
  → domain filter
  → recall
  → RRF
  → rerank
  → citation
```

索引版本参与缓存 key。真实 Embedding 和离线字符向量必须通过显式环境变量切换。

## 4. 缓存与幂等

- 知识索引按版本缓存。
- 相同文件 hash 不重复构建。
- 工具只读请求可以有限重试。
- 写业务动作不允许通过工具自动重试。
- Citation 只从命中记录构造，不缓存模型生成文本。

## 5. 可观测性

必须记录：

- tool name；
- arguments 的白名单字段；
- latency；
- status / error_type；
- retrieval domain；
- recall count；
- rerank before/after rank；
- citation ids。

禁止记录原始用户隐私和完整业务响应。

## 6. 测试策略

| 测试 | 内容 |
|---|---|
| Tool registry | 白名单和只读约束 |
| Runtime ownership | 跨账号订单 |
| Clarification | 多候选和缺参 |
| Degradation | timeout 和 5xx |
| Retrieval | dense / sparse / RRF |
| Rerank | 正常和 fallback |
| Citation | 来源和 snippet |
| Regression | cases.yml |

## 7. 回滚

Agent 服务可独立回滚。若索引文件变化导致质量下降，恢复上一 index version 并重跑回归 case。