# Ecom-ai

Ecom-ai 是一个面向电商售后场景的客服 Agent 系统，覆盖实时订单与物流查询、RAG 知识问答、工具调用、退款/退货 HITL 工作流、Checkpoint 恢复、安全边界、Trace、Eval 和成本治理。

## 服务组成

| 目录 | 职责 |
|---|---|
| `agent-service/` | FastAPI Agent 编排、Tool、RAG、LangGraph Workflow、Trace 与 Eval |
| `ecommerce-backend/` | Java Spring Boot 电商业务后端，提供订单、物流、退款、商品和审批事实 |
| `frontend/` | React + TypeScript 调试后台，展示回答、引用、工具调用、Trace 和评测 |
| `specs/` | 功能规格和验收标准 |
| `docs/` | 架构、AI 工程、部署和测试说明 |
| `tests/` | 不依赖外部模型的契约测试 |

## 开发规格

完整设计说明见 [`specs/DEV_SPEC.md`](specs/DEV_SPEC.md)。

## 架构

```text
浏览器调试后台
   │
   ├── /chat /eval /trace ──► FastAPI Agent :8000
   │                              │
   │                              ├── RAG / Tool / Workflow / Eval
   │                              └── OpenAI-compatible model APIs
   │
   └── 订单 / 物流 / 退款事实 ──► Spring Boot :8081 ──► MySQL
```

Agent 服务不会执行真实资金退款。高风险售后动作只做资格判断、审批暂停和恢复，真实业务边界由业务后端控制。

## AI 链路

```text
用户问题
  → Runtime Context 与可信身份
  → 规则 Guard + 模型意图路由
  → Tool 查询 / RAG 检索 / Workflow / 降级 / 安全路径
  → 受控事实 + citations + observation
  → 模型生成最终话术
  → Trace / Eval / Cost 记录
```

核心工程能力：

- 只读 Tool 与参数校验
- 混合 RAG、Query Rewrite、Reranker 和 Citation
- LangGraph 售后工作流、HITL、Checkpoint、resume token 和幂等
- Prompt Injection 防御与公开 Trace 边界
- 13 条固定回归 case
- 轻/重路径成本摘要与 Observation 压缩

## 快速开始

```bash
cp .env.example .env
docker compose up -d --build
```

服务地址：

- 调试前端：`http://localhost:5173`
- Agent API：`http://localhost:8000/docs`
- 电商业务 API：`http://localhost:8081/swagger-ui/index.html`

不使用 Docker 时：

```bash
python -m venv .venv
pip install -r requirements.txt
cd agent-service
uvicorn main:app --reload --port 8000
```

## 验证

```bash
python -m compileall -q agent-service
python -m unittest discover -s tests -v
```

Agent 回归评测：

```bash
curl -s -X POST http://127.0.0.1:8000/eval/run
```

## 当前边界

- 模型与 Embedding 需要外部 OpenAI-compatible 服务。
- 当前 Agent 状态、Trace 和 Workflow Checkpoint 以单实例内存实现为主。
- 退款审批是模拟流程，不执行真实资金动作。
- 没有生产级 FinOps、工单系统、客服质检或统一监控平台。