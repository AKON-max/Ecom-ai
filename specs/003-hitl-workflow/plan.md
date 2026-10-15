# Technical Plan: Refund and Return HITL

## Affected modules

- gent-service/
- commerce-backend/
- rontend/

## Approach

The behavior is implemented in the current service and verified through fixed cases. This document records the maintained contract rather than a historical implementation log.

## Risks

- State is process-local in the current version.
- Model providers may be unavailable; deterministic guards must remain authoritative.
- Safety rules are layered and must not rely on model compliance alone.

## Verification

resume and idempotency cases in cases.yml