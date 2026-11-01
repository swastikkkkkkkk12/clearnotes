# ClearNotes

ClearNotes turns audio recordings into timestamped transcripts, decisions, and tasks. This repository is being built phase by phase; Phase 0 provides the local service skeleton and health checks.

## Phase 0 services

| Service | Local address | Health check |
| --- | --- | --- |
| Spring Boot backend | http://localhost:8080 | http://localhost:8080/actuator/health |
| FastAPI transcription service | http://localhost:8000 | http://localhost:8000/health |
| MySQL 8.4 | localhost:3306 | Docker health check |

## Run locally with Docker Compose

Docker Desktop (or another Docker Engine with Compose) is required.

```sh
cp .env.example .env
docker compose up --build
```

Wait for the services to start, then verify the health endpoints:

```sh
curl http://localhost:8080/actuator/health
curl http://localhost:8000/health
docker compose ps
```

Expected responses include `{"status":"UP"}` from Spring Boot and `{"status":"UP","service":"clearnotes-transcription"}` from FastAPI. Compose waits for MySQL's health check before starting the backend.

Stop the services with `docker compose down`. The named `mysql_data` volume keeps local database files when containers are stopped; `docker compose down -v` also deletes that volume.

The credentials in `.env.example` are for local development only. Do not use them in a deployed environment.

## Planned project areas

The React frontend, schema migrations, authentication, audio processing, and evaluation tooling will be added in their corresponding phases.
