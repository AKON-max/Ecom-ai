# Feature Specification: Refund and Return HITL

- Status: implemented
- Last reviewed: 2026-10-15

## Problem

Refund and return flows pause for approval, expose a resume token, verify frozen facts, and remain idempotent on retry.

## Requirements

R1: LangGraph state graph. R2: checkpoint plus resume token. R3: frozen field recheck. R4: no real money movement.

## Acceptance criteria

- A duplicated approval does not create a second application.
- resume and idempotency cases in cases.yml

## Out of scope

Replace in-memory checkpoints with a durable store.