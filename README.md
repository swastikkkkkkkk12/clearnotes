# ClearNotes

ClearNotes turns audio recordings into timestamped transcripts, evidence-linked decisions and tasks, and meeting reminders. The local backend, database schema, authentication, asynchronous transcription, and Ollama note extraction are implemented; the React review interface and exports remain planned.

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
- `POST /api/recordings` requires a bearer token and multipart fields `file` and optional `deleteAudioAfterProcessing` (`true` or `false`). It accepts MP3, WAV, M4A, OGG, WebM, and FLAC files up to 100 MB, stores the audio in a Docker volume, and creates a `QUEUED` processing job.
- `GET /api/jobs/{jobId}` requires the same bearer token and returns the job status and timestamps. The authenticated user's ownership is checked for every status lookup. Jobs transcribe audio, use local Ollama for evidence-linked decisions and tasks, and derive meeting reminders only from explicit transcript dates and times.
- `GET /api/recordings/{recordingId}/notes` requires a bearer token and returns evidence-linked decisions, tasks, and meeting details. It returns a Google Calendar pre-filled link for an exact-date all-day meeting or a meeting with explicit start and end times. Relative dates never create a guessed event. If the transcript gives a start time but no end time, ClearNotes leaves the link empty and asks you to add the duration in Google Calendar. Review and save linked events yourself, then choose a notification.
- `POST http://localhost:8000/transcribe` accepts multipart field `file` and optional `language` (for example `en`). It returns the detected language and transcript segments with `start`, `end`, and `text`. The default CPU model is `tiny`; the model downloads on the first transcription and is cached in a Docker volume.

Try the transcription endpoint with an audio file:

```sh
curl -X POST http://localhost:8000/transcribe \
  -F 'file=@./meeting.wav' \
  -F 'language=en'
```

The backend connects to Ollama running on the Mac at `host.docker.internal:11434`. Ensure Ollama is running and has the configured model (default `llama3.2:3b`); the model is available locally and can be checked with `ollama list`.

Passwords are stored as BCrypt hashes. Access tokens expire after one hour. Requests for a recording must use an owner-scoped database lookup; the repository provides `findByIdAndUser_Id` for this purpose.

## Planned project areas

The React frontend, exports, and evaluation tooling are planned for later phases.
