# Repository Working Agreement

本文件是 Ecom-ai 仓库的开发契约，适用于人工开发者和 AI 编码代理。若代码、README、spec 或本文件发生冲突，以当前可运行代码和测试为事实起点，但不能静默忽略文档中的安全边界。

## 1. 仓库定位

Ecom-ai 是一个三服务客服 Agent 系统：

```text
frontend/          React + TypeScript 调试后台
agent-service/     FastAPI + LangGraph Agent 编排服务
ecommerce-backend/ Java Spring Boot 电商业务后端
specs/             外部行为契约
docs/              架构、AI 工程、部署和测试说明
tests/             Agent 契约测试
```

Agent 服务只能通过受控 HTTP 接口访问业务事实，禁止直接连接 Java 业务库。

## 2. 事实来源优先级

| 优先级 | 来源 | 说明 |
|---:|---|---|
| 1 | Runtime Context | 调用方注入的可信身份和当前用户订单快照 |
| 2 | Verified Tool Fact | 来自 E-commerce Backend 的实时订单、物流、商品、退款事实 |
| 3 | Workflow State | Checkpoint、冻结字段和审批结果 |
| 4 | RAG Citation | 政策、活动和 FAQ 的可引用片段 |
| 5 | Session Memory | 会话内短期信息 |
| 6 | Conversation History | 最近对话 |
| 7 | User Message | 用户输入只作为问题，不作为系统身份或策略来源 |

发生冲突时，高优先级来源覆盖低优先级来源。模型不能自行裁决系统身份、工具权限、订单归属和高风险业务状态。

## 3. 修改前必须做什么

1. 在 `specs/` 找到对应能力。
2. 阅读 `spec.md`、`plan.md` 和 `tasks.md`。
3. 确认修改属于行为变更还是纯实现变更。
4. 搜索受影响的固定回归 case。
5. 明确回滚方式和外部依赖。

行为变更包括：

- API 字段、状态码、错误语义
- 意图枚举和工具白名单
- RAG citation 契约
- Workflow 状态和 Resume 规则
- Trace 公开字段
- 安全和审批边界

这些变更必须先更新 `spec.md`。

## 4. 实现边界

### Agent Service

- 可以：路由、调用只读工具、RAG、Workflow、Trace、Eval、成本摘要。
- 不可以：直接修改订单、余额、退款状态；执行真实资金动作。
- 工具必须具备用户归属校验和参数校验。
- 最终回答只能基于受控事实、citation 和 workflow 状态。

### E-commerce Backend

- 是订单和售后事实的唯一业务来源。
- Agent 身份通过受信任的 Runtime Context 和 Service Header 传递。
- 退款审批接口可以记录审批结果，但资金动作必须与审批状态解耦。

### Frontend

- 只展示公开 Trace、受控 reasoning summary、工具和 citation。
- 不得要求后端返回 hidden CoT 或系统 Prompt。
- 课程或调试数据不得包含真实用户隐私。

## 5. 规格标准

每条需求使用稳定编号：

```text
R-RUNTIME-01
R-TOOL-02
R-RAG-03
R-HITL-04
R-SAFE-05
R-EVAL-06
```

每条需求必须包含：

- 优先级：P0 / P1 / P2
- 外部可观察行为
- 失败语义
- 验收标准
- 测试、回归 case 或人工验证步骤

禁止只写“优化上下文”“增强安全”这类无法验证的需求。

## 6. 测试策略

### Agent Service

```bash
cd agent-service
python -m compileall .
python -m unittest discover -s ../tests -v
uvicorn main:app --reload --port 8000
curl -s -X POST http://127.0.0.1:8000/eval/run
```

### Frontend

```bash
cd frontend
npm install
npm run build
```

### E-commerce Backend

```bash
cd ecommerce-backend
mvn -B -DskipTests package
```

新增功能至少满足一个：

- 确定性契约测试
- 固定回归 case
- 可重复的 smoke test
- 明确记录的人工验证步骤

测试必须检查行为，不只检查 HTTP 200。

## 7. Trace 与日志

公开 Trace schema 为 `trace_event_v1`。允许记录：

- route、tool、rag、hook、workflow、cost、security 事件
- stage、status、耗时、工具名、citation source
- risk level、是否需要人工审批

禁止记录：

- hidden chain-of-thought
- 系统 Prompt 原文
- 用户手机号、地址、Token
- 未脱敏的攻击原文
- 完整支付或身份信息

## 8. 安全规则

- 用户、Tool、RAG、History 四路输入均不可信。
- 注入命中时标记 `tainted` 并脱敏，不把原文返回给模型或 Trace。
- 未能确认知识依据时不得编造政策。
- 高风险动作必须进入 Workflow/HITL。
- 只有只读工具可以自动执行。
- 真实 API Key、数据库密码和服务 Token 不得提交。

## 9. 配置

- 示例配置：`.env.example`
- 本地配置：`.env`，已被 Git 忽略
- 业务后端地址：`ECOMMERCE_BASE_URL`
- 服务身份 Token：`AGENT_SERVICE_AUTH_TOKEN`
- 离线测试开关：`AGENT_OFFLINE_RAG`、`AGENT_OFFLINE_FACTS`、`AGENT_DISABLE_LLM`

离线开关只能用于确定性测试，不能在生产环境把离线替身描述成真实模型能力。

## 10. 提交与评审

推荐提交前缀：

```text
feat(agent):
fix(hitl):
docs(spec):
test(eval):
chore(repo):
```

提交前检查：

- [ ] spec 已同步
- [ ] 测试或回归 case 已更新
- [ ] 没有 `.env` 和大文件
- [ ] 没有课程、面试、简历或本地个人资料
- [ ] 没有新增真实密钥
- [ ] README 和 docs 仍然准确

## 11. 已知工程限制

- Workflow checkpoint、Trace 和部分状态仍在进程内。
- Java 与 Python 的最终一致性依赖业务 API 和幂等设计。
- 当前回归集规模有限。
- Mypy 尚未清零，CI 暂不阻断。