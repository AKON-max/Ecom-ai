# Technical Plan: Refund and Return HITL

| 字段 | 值 |
|---|---|
| Spec | `003-hitl-workflow` |
| Status | implemented |
| Engine | LangGraph StateGraph |
| Modules | `workflows/after_sale_graph.py`, `workflows/resume.py`, `state/session_state.py` |
| Persistence | 当前进程内；生产目标为 PostgreSQL |
| Rollback | 回退 Agent 服务；无真实资金回滚 |

## 1. 图节点

### RefundApprovalGraph

```text
check_eligibility
  → create_checkpoint
  → require_approval
  → recheck_frozen_fields
  → submit_simulated_request
  → complete
```

### ReceivedReturnGraph

```text
check_return_window
  → create_checkpoint
  → require_approval
  → recheck_frozen_fields
  → submit_simulated_request
  → complete
```

每个节点只处理一个职责：

- 资格节点不创建 checkpoint；
- checkpoint 节点不判断审批结果；
- 审批节点不改业务事实；
- 复核节点不调用提交；
- 提交节点不再做新的资格判断。

## 2. Checkpoint Store

接口：

```python
class WorkflowCheckpointStore(Protocol):
    def save(self, checkpoint: dict) -> None: ...
    def get(self, workflow_id: str) -> dict | None: ...
    def update(self, workflow_id: str, patch: dict) -> dict: ...
```

当前实现为进程内字典，因此：

- 单实例可恢复；
- 进程重启后丢失；
- 多实例不共享；
- 生产迁移需要数据库事务和版本字段。

## 3. Resume

```text
Receive ChatResumeRequest
  → validate reviewer / decision
  → load checkpoint
  → compare resume_token
  → reload order facts
  → compare frozen_fields_hash
  → compute idempotency_key
  → execute once
  → return ChatResumeResponse
```

## 4. 冻结字段哈希

```text
frozen_hash = sha256(canonical_json({
  order_id,
  runtime_user_id,
  fulfillment_status,
  payment_status,
  amount,
  signed_at
}))
```

哈希变化即 blocked，不允许用新的订单事实覆盖 checkpoint。

## 5. 幂等与并发

- 同一 idempotency key 只允许一次提交；
- 重复请求返回既有结果；
- 并发审批使用锁或数据库唯一约束；
- 已 completed 的 workflow 不可重新提交；
- rejected / blocked 状态不能直接 approved。

## 6. 错误映射

| 错误 | 返回状态 |
|---|---|
| checkpoint missing | blocked |
| token mismatch | blocked |
| frozen fields drift | blocked |
| upstream timeout | paused / transfer_to_human |
| invalid decision | 422 |
| unauthorized reviewer | blocked |
| duplicate approval | completed / existing result |

## 7. 验证

- 未发货退款；
- 已发货退款；
- 签收后退货；
- 无效 token；
- 缺少 checkpoint；
- 批准幂等；
- 拒绝；
- needs_more_info；
- 并发重复审批。

## 8. 回滚

Agent 服务可独立回滚。若 checkpoint schema 变化，必须支持旧版本读取或显式阻止恢复并转人工。