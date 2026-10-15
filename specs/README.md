# Specifications

本目录保存 Ecom-ai 的维护型工程规格。规格描述“系统必须提供什么行为”，实现代码和测试负责证明行为真实存在。

## 状态定义

| 状态 | 含义 |
|---|---|
| draft | 设计或讨论中，尚未成为实现约束 |
| accepted | 已评审，开发必须遵循 |
| implemented | 当前代码已满足验收标准 |
| superseded | 已被新规格替代 |

## 文档结构

每个能力目录包含：

```text
spec.md    外部契约、失败语义、验收标准和测试映射
plan.md    实现方案、依赖、时序、风险和回滚
tasks.md   分阶段任务和需求追踪
```

## 追踪规则

1. 每条需求必须有稳定编号，例如 `R-TOOL-03`。
2. 每条验收标准必须指向自动化测试、固定回归 case 或明确的人工验证步骤。
3. 修改 API、状态机、错误码或安全边界时，必须先更新 `spec.md`。
4. 只改变实现方式、不改变外部行为时，更新 `plan.md`。
5. 已部署行为与规格冲突时，以代码现状为调查起点，但不能静默删除规格约束。

## 当前规格

| ID | 能力 | 状态 |
|---|---|---|
| 001 | Agent Runtime Contract | implemented |
| 002 | Tool and RAG Authority | implemented |
| 003 | Refund and Return HITL | implemented |
| 004 | Safety and Regression Evaluation | implemented |