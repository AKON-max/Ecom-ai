# Tasks: Agent Runtime Contract

## Phase 1 · Contract and Schema

- [x] T-RUNTIME-01 定义 ChatRequest / ChatResponse
- [x] T-RUNTIME-02 定义 RoutePlan / Intent / RiskLevel
- [x] T-RUNTIME-03 定义 ToolCallTrace / Citation / ClarificationRequest
- [x] T-RUNTIME-04 定义 TraceEvent v1
- [x] T-RUNTIME-05 定义错误和降级语义

## Phase 2 · Runtime

- [x] T-RUNTIME-06 Runtime Context 归一化
- [x] T-RUNTIME-07 模型路由 + policy constraints
- [x] T-RUNTIME-08 deterministic fallback
- [x] T-RUNTIME-09 Tool / RAG / Workflow / Degradation / Security 主路径
- [x] T-RUNTIME-10 最终话术合成
- [x] T-RUNTIME-11 session_state 组装
- [x] T-RUNTIME-12 Trace、Eval 信号和 Cost

## Phase 3 · Verification

- [x] T-TEST-01 Schema contract tests
- [x] T-TEST-02 Runtime Context tests
- [x] T-TEST-03 Tool/RAG/HITL/Security regression cases
- [x] T-TEST-04 cross-account tests
- [x] T-TEST-05 Trace forbidden-field checks
- [ ] T-PROD-01 多实例状态外置
- [ ] T-PROD-02 持久化 Trace / Session

## Traceability

| Requirement | Task | Verification |
|---|---|---|
| R-RUNTIME-01 | T-RUNTIME-01 | Schema validation |
| R-RUNTIME-02 | T-RUNTIME-06 | runtime context tests |
| R-ROUTE-01 | T-RUNTIME-02 | RoutePlan contract |
| R-ROUTE-03 | T-RUNTIME-07 | policy constraint tests |
| R-ROUTE-04 | T-RUNTIME-09 | HITL/security cases |
| R-EXEC-01 | T-RUNTIME-09 | route trace |
| R-EXEC-02 | T-RUNTIME-09 | cross-account |
| R-OUT-01 | T-RUNTIME-01 | response contract |
| R-OUT-02 | T-RUNTIME-12 | trace tests |