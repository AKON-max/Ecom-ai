# Contributing

- Keep changes scoped to one capability.
- Preserve deterministic safety boundaries even when the model route changes.
- Add regression cases for new tool, RAG, workflow and safety behavior.
- Do not commit `course.env`, `.env`, model keys or local service tokens.
- Keep public trace free of hidden chain-of-thought and sensitive data.

## Checks

```bash
python -m compileall -q agent-service
python -m unittest discover -s tests -v
cd frontend && npm run build
cd ecommerce-backend && mvn -B -DskipTests package
```