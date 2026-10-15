# Technical Plan: Refund and Return HITL

| 字段 | 值 |
|---|---|
| Spec | `003-hitl-workflow` |
| Status | implemented |
| Key modules | `workflows/after_sale_graph.py`, `workflows/resume.py`, `state/session_state.py` |
| State storage | 进程内 checkpoint（当前版本） |
| Production migration | PostgreSQL / Redis shared checkpoint store |

## 1. 图编排

### RefundApprovalGraph

```text
check_eligibility
  → build_checkpoint
  → require_approval
  → recheck_frozen_fields
  → submit_simulated_request
  → completed
```

### ReceivedReturnGraph

```text
check_received_window
  → build_checkpoint
  → require_approval
  → recheck_frozen_fields
  → submit_simulated_request
  → completed
```

## 2. 冻结字段

Checkpoint 至少冻结：

- `order_id`
- `runtime_user_id`
- `fulfillment_status`
- `payment_status`
- `signed_at`
- `amount`
- `workflow_id`

恢复时重新读取订单事实；任一字段漂移则返回 `blocked`。

## 3. 幂等键

建议格式：

```text
idempotency_key = sha256(workflow_id + reviewer_id + decision + frozen_hash)
```

如果同一键已经存在，返回第一次处理结果，不再提交业务申请。

## 4. 风险

| 风险 | 缓解 |
|---|---|
| 用户修改订单号 | checkpoint + token + 冻结字段校验 |
| 审批重复点击 | idempotency key |
| 进程重启丢失 checkpoint | 当前限制；生产必须持久化 |
| 上游接口失败 | paused / transfer_to_human |
| 审批越权 | reviewer_id + reviewer_role 记录与校验 |

## 5. 回滚

Agent 服务可独立回滚。由于不执行真实资金动作，回滚不涉及财务数据修复。
