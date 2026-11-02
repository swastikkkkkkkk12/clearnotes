import logging
import os
import re
import tempfile
import threading
from pathlib import Path

from faster_whisper import WhisperModel
from fastapi import FastAPI, File, Form, HTTPException, UploadFile
from pydantic import BaseModel, Field

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("clearnotes.transcription")

MODEL_NAME = os.getenv("WHISPER_MODEL", "tiny")
MODEL_DEVICE = os.getenv("WHISPER_DEVICE", "cpu")
COMPUTE_TYPE = os.getenv("WHISPER_COMPUTE_TYPE", "int8")
CPU_THREADS = int(os.getenv("WHISPER_CPU_THREADS", "4"))
MAX_AUDIO_BYTES = int(os.getenv("MAX_AUDIO_BYTES", str(100 * 1024 * 1024)))
CHUNK_SIZE = 1024 * 1024
ALLOWED_EXTENSIONS = {".mp3", ".wav", ".m4a", ".ogg", ".webm", ".flac"}

app = FastAPI(title="ClearNotes Transcription Service")
_model: WhisperModel | None = None
_model_lock = threading.Lock()


class TranscriptSegment(BaseModel):
    start: float = Field(ge=0)
    end: float = Field(ge=0)
    text: str


class TranscriptionResponse(BaseModel):
    language: str
    language_probability: float = Field(ge=0, le=1)
    duration_seconds: float = Field(ge=0)
    segments: list[TranscriptSegment]


def get_model() -> WhisperModel:
    """Load the CPU model on first use and reuse it for later requests."""
    global _model
    if _model is None:
        with _model_lock:
            if _model is None:
                logger.info("Loading faster-whisper model %s on %s", MODEL_NAME, MODEL_DEVICE)
                _model = WhisperModel(
                    MODEL_NAME,
                    device=MODEL_DEVICE,
                    compute_type=COMPUTE_TYPE,
                    cpu_threads=CPU_THREADS,
                )
    return _model


def has_audio_signature(extension: str, header: bytes) -> bool:
    if extension == ".mp3":
        return header.startswith(b"ID3") or (len(header) >= 2 and header[0] == 0xFF and header[1] & 0xE0 == 0xE0)
    if extension == ".wav":
        return len(header) >= 12 and header.startswith(b"RIFF") and header[8:12] == b"WAVE"
    if extension == ".m4a":
        return len(header) >= 8 and header[4:8] == b"ftyp"
    if extension == ".ogg":
        return header.startswith(b"OggS")
    if extension == ".webm":
        return header.startswith(bytes((0x1A, 0x45, 0xDF, 0xA3)))
    if extension == ".flac":
        return header.startswith(b"fLaC")
    return False


def save_validated_audio(file: UploadFile) -> str:
    filename = Path(file.filename or "").name
    extension = Path(filename).suffix.lower()
    if extension not in ALLOWED_EXTENSIONS:
        raise HTTPException(400, "Supported audio formats are MP3, WAV, M4A, OGG, WebM, and FLAC.")
    temp_path: str | None = None
    header = bytearray()
    total = 0
    try:
        with tempfile.NamedTemporaryFile(prefix="clearnotes-", suffix=extension, delete=False) as temp:
            temp_path = temp.name
            while chunk := file.file.read(CHUNK_SIZE):
                total += len(chunk)
                if total > MAX_AUDIO_BYTES:
                    raise HTTPException(413, "Audio file exceeds the 100 MB upload limit.")
                if len(header) < 12:
                    header.extend(chunk[: 12 - len(header)])
                temp.write(chunk)
        if total == 0:
            raise HTTPException(400, "The uploaded audio file is empty.")
        if not has_audio_signature(extension, bytes(header)):
            raise HTTPException(400, "The file contents do not match a supported audio format.")
        return temp_path
    except Exception:
        if temp_path:
            Path(temp_path).unlink(missing_ok=True)
        raise


@app.get("/health")
def health_check() -> dict[str, str | bool]:
    """Indicate the service is running without downloading model weights."""
    return {"status": "UP", "service": "clearnotes-transcription", "model_loaded": _model is not None}


@app.post("/transcribe", response_model=TranscriptionResponse)
def transcribe(
    file: UploadFile = File(...),
    language: str | None = Form(default=None),
) -> TranscriptionResponse:
    if language is not None:
        language = language.strip().lower() or None
        if language and not re.fullmatch(r"[a-z]{2,3}", language):
            raise HTTPException(422, "Language must be a two- or three-letter code, such as 'en'.")

    audio_path = save_validated_audio(file)
    try:
        try:
            model = get_model()
        except Exception as error:
            logger.exception("Could not load faster-whisper model")
            raise HTTPException(503, "The transcription model could not be loaded.") from error

        try:
            segments, info = model.transcribe(audio_path, beam_size=5, language=language, vad_filter=True)
            # faster-whisper returns a generator; iterating it performs the transcription.
            result = [
                TranscriptSegment(start=round(segment.start, 3), end=round(segment.end, 3), text=segment.text.strip())
                for segment in segments
                if segment.text.strip()
            ]
        except Exception as error:
            logger.exception("Audio transcription failed")
            raise HTTPException(422, "The audio could not be decoded or transcribed.") from error

        return TranscriptionResponse(
            language=info.language,
            language_probability=round(info.language_probability, 4),
            duration_seconds=round(info.duration, 3),
            segments=result,
        )
    finally:
        Path(audio_path).unlink(missing_ok=True)
