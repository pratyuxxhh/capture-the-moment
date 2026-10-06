import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.controllers.health_controller import router as health_router
from app.controllers.page_controller import router as page_router
from app.controllers.transcription_controller import router as transcription_router
from app.services.config import (
    CUDA_AVAILABLE,
    GPU_NAME,
    UPLOADS_DIR,
    WHISPER_COMPUTE_TYPE,
    WHISPER_DEVICE,
    load_environment,
)
from app.services.meeting_service import MeetingService

load_environment()

logging.basicConfig(level=logging.INFO, format="%(message)s")
logger = logging.getLogger("transcription")


@asynccontextmanager
async def lifespan(app: FastAPI):
    UPLOADS_DIR.mkdir(parents=True, exist_ok=True)
    logger.info("CUDA available: %s", CUDA_AVAILABLE)
    if CUDA_AVAILABLE:
        logger.info("GPU: %s", GPU_NAME)
        logger.info("CUDA GPU detected: %s. Using GPU.", GPU_NAME)
    else:
        logger.info("No CUDA GPU detected. Using CPU.")
    logger.info("Device: %s", WHISPER_DEVICE)
    logger.info("Compute type: %s", WHISPER_COMPUTE_TYPE)
    # Models stay in memory only. Nothing is written to a database.
    app.state.meeting_service = MeetingService()
    logger.info("API is ready. Whisper and pyannote load on the first POST /transcribe.")
    yield


app = FastAPI(
    title="Transcription Service",
    description="GPU-accelerated transcription when available, with CPU fallback.",
    version="0.3.0",
    lifespan=lifespan,
)

app.include_router(page_router)
app.include_router(health_router)
app.include_router(transcription_router)
