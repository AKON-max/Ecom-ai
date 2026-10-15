# Feature Specification: Safety and Regression Evaluation

- Status: implemented
- Last reviewed: 2026-10-15

## Problem

External text is untrusted, hidden chain-of-thought is never returned, and fixed cases prevent capability regressions.

## Requirements

R1: scan user, tool, RAG, and history input. R2: public trace only. R3: fixed cases check answer and path. R4: feedback can backfill a case.

## Acceptance criteria

- Injection attempts cannot disclose prompts or hidden reasoning.
- /eval/run with 13 fixed cases

## Out of scope

Add LLM-as-guard and continuous CI evaluation.