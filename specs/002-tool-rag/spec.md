# Feature Specification: Tool and RAG Authority

- Status: implemented
- Last reviewed: 2026-10-15

## Problem

Realtime business facts come from tools; policy and FAQ answers come from cited RAG evidence. The model may route but may not invent facts or citations.

## Requirements

R1: validate tool arguments. R2: expose only read-only tools outside workflows. R3: return citations for policy answers. R4: low confidence transfers to a human.

## Acceptance criteria

- A missing or ambiguous order number asks for clarification or blocks execution.
- case order-logistics-tool-path and promotion-reranker-citation

## Out of scope

Add production vector storage and stronger reranking.