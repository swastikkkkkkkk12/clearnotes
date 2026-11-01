from fastapi import FastAPI

app = FastAPI(title="ClearNotes Transcription Service")


@app.get("/health")
def health_check() -> dict[str, str]:
    """Small readiness endpoint for Docker Compose and local verification."""
    return {"status": "UP", "service": "clearnotes-transcription"}
