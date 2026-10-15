# AI Engineering

## Routing

The service combines deterministic guards with model-based routing. Refund, return, security, and degradation paths retain rule-based boundaries because they carry authority or state-machine semantics.

## RAG

- Markdown knowledge is converted into chunks with stable metadata.
- Retrieval combines pre-retrieval, keyword scoring, vector similarity, and a lightweight reranker.
- Answers carry citations.
- Low-confidence results do not invent policy text and fall back to human transfer.

## Tools

Tools are read-only unless a workflow explicitly owns a high-risk action. The runtime validates arguments before tool execution and records Action / Observation pairs in the public trace.

## Workflow and HITL

Refund and return flows use a LangGraph state graph. Paused runs expose a resume token. Resume verifies the checkpoint, frozen business facts, and idempotency key.

## Safety

User, tool, RAG, and history inputs pass source scanning. The public trace exposes route, tool, RAG, hook, and cost events, but never hidden chain-of-thought.

## Evaluation

`cases.yml` contains fixed regression cases. Evaluation checks answer signals, expected and forbidden tools, citations, trace events, session state, workflow state, and safety boundaries.