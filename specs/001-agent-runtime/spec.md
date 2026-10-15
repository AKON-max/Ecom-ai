# Feature Specification: Agent Runtime Contract

- Status: implemented
- Last reviewed: 2026-10-15

## Problem

Agent service routes each request through runtime context, intent routing, one or more controlled paths, and a final answer based on trusted facts.

## Requirements

R1: bind identity to Runtime Context. R2: block high-risk operations without approval. R3: record public trace events. R4: return structured tools, citations, workflow, trace, and cost state.

## Acceptance criteria

- A request outside the trusted identity cannot query another user order.
- tests/test_agent_contract.py and /eval/run

## Out of scope

Add distributed state and shared tracing.