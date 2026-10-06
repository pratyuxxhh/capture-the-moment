import os
from pathlib import Path

import torch
from dotenv import load_dotenv

PROJECT_ROOT = Path(__file__).resolve().parents[2]
UPLOADS_DIR = PROJECT_ROOT / "uploads"
VIEWS_DIR = Path(__file__).resolve().parents[1] / "views"

ALLOWED_EXTENSIONS = {".mp3", ".wav", ".m4a", ".flac", ".ogg"}
DIARIZATION_PIPELINE_ID = "pyannote/speaker-diarization-community-1"
DIARIZATION_MODEL_URL = (
    "https://huggingface.co/pyannote/speaker-diarization-community-1"
)
WHISPER_MODEL_SIZE = os.getenv("WHISPER_MODEL", "base")
CUDA_AVAILABLE = torch.cuda.is_available()
WHISPER_DEVICE = "cuda" if CUDA_AVAILABLE else "cpu"
WHISPER_COMPUTE_TYPE = "float16" if CUDA_AVAILABLE else "int8"
GPU_NAME = torch.cuda.get_device_name(0) if CUDA_AVAILABLE else None


def load_environment() -> None:
    load_dotenv(PROJECT_ROOT / ".env", override=True)


def get_hf_token() -> str:
    raw = os.getenv("HF_TOKEN") or os.getenv("HUGGING_FACE_HUB_TOKEN") or ""
    return raw.strip().strip('"').strip("'")


def gated_model_help() -> str:
    return (
        "HF_TOKEN is valid, but this account cannot download "
        f"{DIARIZATION_PIPELINE_ID}. Open {DIARIZATION_MODEL_URL} while logged "
        "into the same Hugging Face account, click Agree and access repository, "
        "wait about a minute, then restart the app."
    )


load_environment()
# Keep Hugging Face client libraries in sync with the stripped .env value.
if get_hf_token():
    os.environ["HF_TOKEN"] = get_hf_token()
    os.environ["HUGGING_FACE_HUB_TOKEN"] = get_hf_token()
