# Tasks: Safety and Regression Evaluation

- [x] T-SAFE-01 实现四路 Source Guard
- [x] T-SAFE-02 实现 tainted 标记和脱敏
- [x] T-SAFE-03 实现公开 Trace schema
- [x] T-SAFE-04 实现 hidden CoT 边界
- [x] T-EVAL-01 实现 13 条固定 case
- [x] T-EVAL-02 检查 answer signals
- [x] T-EVAL-03 检查 tools 和 citations
- [x] T-EVAL-04 检查 trace 和 session_state
- [x] T-EVAL-05 实现反馈归因
- [x] T-EVAL-06 实现 case backfill
- [x] T-TEST-01 添加安全契约测试
- [ ] T-PROD-01 Layer 2 输入归一化
- [ ] T-PROD-02 LLM-as-Guard 对可疑输入做语义分类
- [ ] T-PROD-03 Session 风险积分和跨轮攻击检测

## Traceability

| Requirement | Test / Evidence |
|---|---|
| R-SAFE-01 | test_safety.py |
| R-SAFE-02 | injection case |
| R-SAFE-03 | trace schema assertions |
| R-SAFE-04 | forbidden_text |
| R-EVAL-01 | expected_signals |
| R-EVAL-02 | expected/forbidden tools |
| R-EVAL-03 | citations/trace/session state |
| R-EVAL-04 | feedback attribution |
| R-EVAL-05 | backfilled_case |
