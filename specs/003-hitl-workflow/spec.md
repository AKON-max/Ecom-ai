# Feature Specification: Refund and Return HITL

| 字段 | 值 |
|---|---|
| Spec ID | `003-hitl-workflow` |
| Status | implemented |
| Version | 1.0.0 |
| Workflow engine | LangGraph StateGraph |
| Regression source | 3 条 resume cases + unshipped/received return cases |

## 1. 背景

退款和退货是高风险、可改变资金或售后状态的流程。Agent 可以判断资格、收集事实和发起审批，但不能直接执行真实资金退款。

## 2. 目标

- 对未发货退款和签收后退货执行确定性资格检查。
- 流程在人工审批处暂停，并返回 `resume_token`。
- 恢复时校验 token、checkpoint、冻结字段和幂等键。
- 拒绝或漂移时阻止执行，并给出明确原因。
- 重复审批不产生第二个业务申请。

## 3. 功能需求

| ID | 优先级 | 需求 | 验证 |
|---|---|---|---|
| R-HITL-01 | P0 | 未发货退款必须进入 workflow，不能只返回普通 FAQ。 | `unshipped-refund-hitl` |
| R-HITL-02 | P0 | 已发货退款必须在 workflow 前阻止，并说明规则。 | `shipped-refund-blocked-before-workflow` |
| R-HITL-03 | P0 | 签收后退货按时间窗口和签收状态判断资格。 | `received-return-workflow` |
| R-HITL-04 | P0 | 等待审批时返回 `paused`、`workflow_id` 和 `resume_token`。 | response contract |
| R-HITL-05 | P0 | Resume 必须校验 checkpoint 和 token。 | `refund-resume-invalid-token-blocked` |
| R-HITL-06 | P0 | Resume 必须复核冻结订单字段，漂移时 blocked。 | resume drift tests |
| R-HITL-07 | P0 | 审批操作必须幂等。 | `refund-resume-approved-idempotent` |
| R-HITL-08 | P1 | 拒绝和需要补充信息都返回可读原因。 | rejected / needs_more_info paths |
| R-HITL-09 | P0 | 不执行真实退款或修改真实余额。 | code review + forbidden text |

## 4. 状态机

```text
draft
  → eligibility_checked
  → require_approval
      ├── approved → business_recheck → submit_once → completed
      ├── rejected → rejected
      └── needs_more_info → paused
```

## 5. 暂停响应契约

```json
{
  "session_id": "session-U1001-xxxx",
  "workflow_id": "refund-xxxx",
  "status": "paused",
  "answer": "该申请需要人工审批。",
  "resume_result": {},
  "workflow": {
    "pending_action": "require_approval",
    "resume_token": "..."
  },
  "business_recheck": {},
  "session_state": {}
}
```

## 6. 失败语义

| 场景 | 行为 |
|---|---|
| token 不匹配 | blocked，不恢复流程 |
| checkpoint 不存在 | blocked，提示重新发起 |
| 订单字段与冻结快照不一致 | blocked，要求人工重新核验 |
| 审批重复提交 | 返回既有结果，不重复创建申请 |
| 上游售后接口失败 | 保持 paused 或转人工，不伪装成功 |

## 7. 安全与合规

- 审批人和角色必须记录在 Resume 请求和 Trace 中。
- 冻结字段至少包括 order_id、user_id、fulfillment_status、payment_status 和金额。
- 资金动作只记录“模拟审批结果”，不写入真实余额。

## 8. 验收标准

- AC-01：未发货退款停在 `require_approval` 并返回 resume token。
- AC-02：已发货退款在 workflow 前被规则阻止。
- AC-03：无效 token 返回 blocked。
- AC-04：缺少 checkpoint 返回 blocked。
- AC-05：审批通过后重复恢复保持幂等。
- AC-06：任何响应都不声称“已真实退款成功”。
