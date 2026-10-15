# Tasks: Refund and Return HITL

## Phase 1 · Eligibility

- [x] T-HITL-01 未发货退款资格
- [x] T-HITL-02 已发货退款阻止
- [x] T-HITL-03 签收后退货窗口
- [x] T-HITL-04 冻结字段定义

## Phase 2 · Graph and Checkpoint

- [x] T-HITL-05 RefundApprovalGraph
- [x] T-HITL-06 ReceivedReturnGraph
- [x] T-HITL-07 workflow_id / checkpoint
- [x] T-HITL-08 resume_token
- [x] T-HITL-09 pending_action 响应

## Phase 3 · Resume

- [x] T-HITL-10 token 校验
- [x] T-HITL-11 checkpoint 校验
- [x] T-HITL-12 冻结字段漂移校验
- [x] T-HITL-13 approved / rejected / needs_more_info
- [x] T-HITL-14 idempotency key
- [x] T-HITL-15 reviewer identity Trace

## Phase 4 · Verification

- [x] T-TEST-01 unshipped-refund-hitl
- [x] T-TEST-02 shipped-refund-blocked
- [x] T-TEST-03 received-return
- [x] T-TEST-04 invalid-token
- [x] T-TEST-05 missing-checkpoint
- [x] T-TEST-06 approved-idempotent
- [ ] T-PROD-01 PostgreSQL checkpoint
- [ ] T-PROD-02 审批角色权限矩阵
- [ ] T-PROD-03 并发锁和跨实例幂等

## Traceability

| Requirement | Task | Test |
|---|---|---|
| R-HITL-01 | T-HITL-01 | unshipped |
| R-HITL-02 | T-HITL-02 | shipped blocked |
| R-HITL-03 | T-HITL-03 | received return |
| R-HITL-04 | T-HITL-07 | response contract |
| R-HITL-05 | T-HITL-10 | invalid token |
| R-HITL-06 | T-HITL-12 | drift tests |
| R-HITL-07 | T-HITL-14 | idempotent |
| R-HITL-08 | T-HITL-13 | resume decisions |
| R-HITL-09 | T-HITL-04 | forbidden text |