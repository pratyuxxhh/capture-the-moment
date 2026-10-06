import logging
import threading
import time

from fastapi import HTTPException, UploadFile, status

from app.models.schemas import TranscribeResponse
from app.services.alignment_service import AlignmentService
from app.services.diarization_service import DiarizationService
from app.services.storage_service import StorageService
from app.services.transcription_service import TranscriptionService

logger = logging.getLogger("transcription")


class MeetingService:
    """Orchestrates upload → Whisper → pyannote → overlap alignment. No database."""

    def __init__(self) -> None:
        self._lock = threading.Lock()
        self.storage = StorageService()
        self.transcription = TranscriptionService()
        self.diarization = DiarizationService()
        self.alignment = AlignmentService()

    def transcribe_upload(self, file: UploadFile) -> TranscribeResponse:
        original_name, saved_path = self.storage.save_upload(file)
        started = time.perf_counter()
        try:
            logger.info("Audio received")
            with self._lock:
                logger.info("Transcription started")
                transcription = self.transcription.transcribe(str(saved_path))
                logger.info("Transcription completed")

                logger.info("Diarization started")
                diarization = self.diarization.diarize(str(saved_path))
                logger.info("Diarization completed")

            segments = self.alignment.merge(transcription.segments, diarization)
            logger.info("Alignment completed")
            logger.info(
                "Processing time: %.2f seconds",
                time.perf_counter() - started,
            )
            return TranscribeResponse(
                filename=original_name,
                language=transcription.language,
                language_probability=transcription.language_probability,
                segments=segments,
            )
        except HTTPException:
            raise
        except Exception as exc:
            logger.exception("Unable to process audio file")
            raise HTTPException(
                status_code=_status_for(exc),
                detail=_client_error_detail(exc),
            ) from None
        finally:
            self.storage.delete(saved_path)


def _status_for(exc: Exception) -> int:
    text = str(exc).lower()
    if "hf_token" in text or "gated" in text or "huggingface" in text:
        return status.HTTP_403_FORBIDDEN
    return status.HTTP_500_INTERNAL_SERVER_ERROR


def _client_error_detail(exc: Exception) -> str:
    text = str(exc)
    lowered = text.lower()
    if "agree and access" in lowered or "cannot download" in lowered:
        return text
    if "401" in text or "unauthorized" in lowered or "rejected hf_token" in lowered:
        return (
            "Hugging Face rejected the token. Check HF_TOKEN at "
            "https://huggingface.co/settings/tokens"
        )
    if "403" in text or "gated" in lowered or "restricted" in lowered:
        from app.services.config import gated_model_help

        return gated_model_help()
    if "hf_token" in lowered:
        return "HF_TOKEN is not set. Add it to .env and restart."
    return "Unable to process audio file"
