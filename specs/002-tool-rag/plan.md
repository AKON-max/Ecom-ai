# Technical Plan: Tool and RAG Authority

| 字段 | 值 |
|---|---|
| Spec | `002-tool-rag` |
| Status | implemented |
| Key modules | `tools/tool_runtime.py`, `rag/hybrid_retrieval.py`, `rag/documents.py`, `integrations/ecommerce_client.py` |
| External dependencies | E-commerce Backend、Embedding API、可选 Reranker API |

## 1. Tool 执行路径

```text
RoutePlan.required_tools
  → whitelist check
  → runtime entity resolution
  → required argument validation
  → E-commerce API call
  → timeout/error classification
  → ToolCallTrace
  → Observation 压缩
```

工具超时不会重试写操作。只读查询可进行一次受控重试；失败后进入降级路径。

## 2. RAG 执行路径

```text
query
  → pre-retrieval 规则
  → keyword scoring
  → embedding similarity
  → domain / version filter
  → candidate pool
  → lightweight reranker
  → citation assembly
```

索引版本参与缓存 key。离线回归通过显式 `AGENT_OFFLINE_RAG=1` 使用字符向量，不能把离线替身伪装成真实语义检索。

## 3. 数据边界

- Tool facts 的优先级高于模型知识。
- Runtime Context 只允许当前用户订单参与工具解析。
- RAG 只引用配置中的 Knowledge Domain。
- Citation 的 `source`、`title` 和 `snippet` 必须来自检索结果。

## 4. 风险

| 风险 | 缓解 |
|---|---|
| 模型传入错误订单号 | 与 Runtime 订单列表比对 |
| 工具返回未知字段 | 归一化 + 保留原始摘要 |
| Embedding 服务不可用 | 明确降级/离线开关 |
| Reranker 超时 | 回退融合排序 |
| Citation 拼接错误 | 只从命中 chunk 构造 |

## 5. 回滚

回退 Agent 服务即可。RAG 索引与业务后端接口保持向后兼容；若知识文件发生破坏性变更，需要恢复上一 index version。