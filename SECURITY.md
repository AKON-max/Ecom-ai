# Security Policy

## Secrets

Only `.env.example` is tracked. Never commit real model API keys, MySQL passwords or service tokens.

If a credential was committed, rotate it first, then remove it from the branch and inspect Git history.

## Agent safety boundaries

- Tools are read-only unless a workflow owns the high-risk action.
- Refund and return flows require approval and idempotent resume.
- Public traces must not expose hidden reasoning or sensitive fields.
- User, tool, RAG and history text are untrusted inputs.