# Testing

## Contract tests

`tests/` contains dependency-light tests for routing helpers, context parsing, and safety invariants.

## Agent regression

`agent-service/cases.yml` defines fixed regression scenarios. Run:

```bash
curl -s -X POST http://127.0.0.1:8000/eval/run
```

The runner checks:

- expected answer signals
- expected and forbidden tools
- RAG citations
- public trace events
- session state and workflow state
- HITL and resume boundaries

## Frontend

```bash
cd frontend
npm run build
```