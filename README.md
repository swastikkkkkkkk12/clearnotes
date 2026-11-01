# ClearNotes

ClearNotes turns audio recordings into timestamped transcripts, decisions, and tasks. The project is being built phase by phase; Phases 0–2 provide the local service skeleton, database schema, and authentication.

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
openssl rand -base64 32
# Copy the generated value into .env after JWT_SECRET=
docker compose up --build
```

Keep `.env` private and untracked. The JWT signing secret must be a unique Base64 value containing at least 32 random bytes.

Wait for the services to start, then verify the health endpoints:

```sh
curl http://localhost:8080/actuator/health
curl http://localhost:8000/health
docker compose ps
```

Expected responses include `{"status":"UP"}` from Spring Boot and `{"status":"UP","service":"clearnotes-transcription"}` from FastAPI. Compose waits for MySQL's health check before starting the backend.

Stop the services with `docker compose down`. The named `mysql_data` volume keeps local database files when containers are stopped; `docker compose down -v` also deletes that volume.

The credentials in `.env.example` are for local development only. Do not use them in a deployed environment.

## Authentication API

- `POST /api/auth/register` accepts `{"email":"you@example.com","password":"at-least-12-characters"}` and returns a bearer token.
- `POST /api/auth/login` accepts the same fields and returns a bearer token.
- `GET /api/auth/me` requires `Authorization: Bearer <accessToken>` and returns the signed-in user's ID and email.

Passwords are stored as BCrypt hashes. Access tokens expire after one hour. Requests for a recording must use an owner-scoped database lookup; the repository provides `findByIdAndUser_Id` for this purpose.

## Planned project areas

The React frontend, audio processing pipeline, notes extraction, exports, and evaluation tooling will be added in their corresponding phases.
