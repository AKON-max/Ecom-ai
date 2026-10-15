# Tasks: Agent Runtime Contract

## Phase 1 · Contract

- [x] T-RUNTIME-01 定义 `ChatRequest` / `ChatResponse` Schema
- [x] T-RUNTIME-02 定义 `Intent`、`RiskLevel` 和 `RoutePlan`
- [x] T-RUNTIME-03 定义公开 `TraceEvent`
- [x] T-RUNTIME-04 定义跨账号订单归属规则

## Phase 2 · Implementation

- [x] T-RUNTIME-05 实现 Runtime Context 摘要
- [x] T-RUNTIME-06 实现模型路由 + 确定性 fallback
- [x] T-RUNTIME-07 实现 Tool / RAG / Workflow / Guard 主路径
- [x] T-RUNTIME-08 实现最终话术合成
- [x] T-RUNTIME-09 实现 Trace 和成本摘要

## Phase 3 · Verification

- [x] T-RUNTIME-10 契约测试
- [x] T-RUNTIME-11 13 条固定回归 case
- [x] T-RUNTIME-12 跨账号与 Prompt 攻击边界验证
- [ ] T-RUNTIME-13 多实例共享状态实现

## Requirement traceability

| Requirement | Tests |
|---|---|
| R-RUNTIME-01 | `test_runtime_context.py` |
| R-RUNTIME-02 | `order-logistics-tool-path` |
| R-RUNTIME-03 | `RoutePlan` contract |
| R-RUNTIME-04 | HITL + security cases |
| R-RUNTIME-05 | expected_session_state |
| R-RUNTIME-06 | prompt-injection-trace-boundary |
| R-RUNTIME-07 | Trace assertions |
| R-RUNTIME-08 | degradation case |
