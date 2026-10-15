# Deployment and Local Development

## Environment

```bash
cp .env.example .env
```

Required variables:

- `AGENT_OPENAI_API_KEY`
- `AGENT_OPENAI_BASE_URL`
- `AGENT_OPENAI_MODEL`
- `AGENT_EMBEDDING_MODEL`
- `AGENT_SERVICE_AUTH_TOKEN`
- `MYSQL_ROOT_PASSWORD`

## Docker Compose

```bash
docker compose up -d --build
docker compose ps
```

Ports:

| Service | Port |
|---|---:|
| Frontend | 5173 |
| Agent API | 8000 |
| E-commerce API | 8081 |
| Chroma | 8001 |
| MySQL | 3306 |

## Backend without Docker

```bash
python -m venv .venv
pip install -r requirements.txt
cd agent-service
uvicorn main:app --reload --port 8000
```

## Frontend without Docker

```bash
cd frontend
npm install
npm run dev
```

## Verification

```bash
python -m compileall -q agent-service
python -m unittest discover -s tests -v
cd frontend && npm run build
curl -s -X POST http://127.0.0.1:8000/eval/run
```