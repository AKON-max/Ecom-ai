# Tasks: Safety and Regression Evaluation

## Phase 1 · Source Guard

- [x] T-SAFE-01 四路输入归一化
- [x] T-SAFE-02 指令覆盖检测
- [x] T-SAFE-03 系统 Prompt 请求检测
- [x] T-SAFE-04 tainted 标记
- [x] T-SAFE-05 可疑文本脱敏
- [x] T-SAFE-06 安全事件记录

## Phase 2 · Trace

- [x] T-TRACE-01 trace_event_v1
- [x] T-TRACE-02 category / stage / status
- [x] T-TRACE-03 target / ids / summary / signals
- [x] T-TRACE-04 safety 字段
- [x] T-TRACE-05 forbidden field 过滤
- [x] T-TRACE-06 public trace summary

## Phase 3 · Evaluation

- [x] T-EVAL-01 cases.yml 加载
- [x] T-EVAL-02 answer signals
- [x] T-EVAL-03 tool expectations
- [x] T-EVAL-04 citation expectations
- [x] T-EVAL-05 trace expectations
- [x] T-EVAL-06 session state expectations
- [x] T-EVAL-07 forbidden text
- [x] T-EVAL-08 Workflow/Resume cases
- [x] T-EVAL-09 结构化 failure summary

## Phase 4 · Feedback

- [x] T-FEEDBACK-01 FeedbackRequest
- [x] T-FEEDBACK-02 FailureAttribution
- [x] T-FEEDBACK-03 backfilled case
- [x] T-FEEDBACK-04 feedback submit API

## Phase 5 · Verification

- [x] T-TEST-01 clean / tainted inputs
- [x] T-TEST-02 hidden CoT boundary
- [x] T-TEST-03 citation/trace assertions
- [x] T-TEST-04 13 case regression
- [x] T-TEST-05 unit tests without LLM
- [ ] T-PROD-01 输入归一化
- [ ] T-PROD-02 LLM-as-Guard
- [ ] T-PROD-03 Session risk score
- [ ] T-PROD-04 feedback 持久化和人工审核流

## Traceability

| Requirement | Task | Verification |
|---|---|---|
| R-SAFE-01 | T-SAFE-01 | test_safety |
| R-SAFE-02 | T-SAFE-04 | injection case |
| R-SAFE-03 | T-SAFE-05 | sanitized content |
| R-TRACE-01 | T-TRACE-01 | schema test |
| R-TRACE-02 | T-TRACE-02 | trace assertions |
| R-EVAL-01 | T-EVAL-02 | /eval/run |
| R-EVAL-02 | T-EVAL-03 | tool assertions |
| R-EVAL-03 | T-EVAL-04 | citation/state |
| R-FEEDBACK-01 | T-FEEDBACK-01 | feedback submit |
| R-FEEDBACK-02 | T-FEEDBACK-02 | attribution |