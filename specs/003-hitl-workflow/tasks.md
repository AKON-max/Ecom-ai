# Tasks: Refund and Return HITL

- [x] T-HITL-01 实现未发货退款资格判断
- [x] T-HITL-02 实现已发货退款阻止
- [x] T-HITL-03 实现签收后退货窗口判断
- [x] T-HITL-04 实现 LangGraph 两套 StateGraph
- [x] T-HITL-05 生成 workflow_id 和 resume_token
- [x] T-HITL-06 实现冻结字段复核
- [x] T-HITL-07 实现批准、拒绝和补充信息
- [x] T-HITL-08 实现幂等恢复
- [x] T-TEST-01 添加 3 条 Resume 回归 case
- [ ] T-PROD-01 Checkpoint 持久化
- [ ] T-PROD-02 审批越权和角色权限矩阵

## Traceability

| Requirement | Test / Evidence |
|---|---|
| R-HITL-01 | unshipped-refund-hitl |
| R-HITL-02 | shipped-refund-blocked-before-workflow |
| R-HITL-03 | received-return-workflow |
| R-HITL-04 | response contract |
| R-HITL-05 | invalid-token case |
| R-HITL-06 | drift validation |
| R-HITL-07 | approved-idempotent case |
| R-HITL-08 | rejected / needs_more_info |
| R-HITL-09 | forbidden text assertions |
