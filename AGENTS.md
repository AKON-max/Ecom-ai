# Repository Working Agreement

## Source of truth

1. `agent-service/`, `ecommerce-backend/`, `frontend/` and `specs/` are the maintained project.
2. `agent-service/cases.yml` contains the fixed regression contract.
3. `docs/` records architecture, AI engineering, deployment and testing decisions.
4. Do not claim production behavior that is not present in the service.

## Change workflow

1. Read the matching spec under `specs/`.
2. Update the spec when public behavior changes.
3. Add or update a regression case or contract test.
4. Run the service, frontend and backend checks that apply.
5. Never commit local model keys, MySQL passwords, service tokens or course snapshots.