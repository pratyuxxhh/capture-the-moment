from huggingface_hub.errors import GatedRepoError, HfHubHTTPError

from app.services.config import (
    DIARIZATION_PIPELINE_ID,
    gated_model_help,
    get_hf_token,
)


def prepare_hf_auth() -> str:
    """Validate HF_TOKEN without loading Whisper or pyannote weights."""
    from huggingface_hub import HfApi, login

    token = get_hf_token()
    if not token:
        raise RuntimeError(
            "HF_TOKEN is not set. Add it to .env (no quotes) and restart."
        )

    login(token=token, add_to_git_credential=False)
    try:
        HfApi(token=token).whoami()
    except Exception as exc:
        raise RuntimeError(
            "Hugging Face rejected HF_TOKEN. Create a new read token at "
            "https://huggingface.co/settings/tokens"
        ) from exc

    try:
        HfApi(token=token).model_info(DIARIZATION_PIPELINE_ID)
    except GatedRepoError as exc:
        raise RuntimeError(gated_model_help()) from exc
    except HfHubHTTPError as exc:
        status = getattr(getattr(exc, "response", None), "status_code", None)
        if status in {401, 403} or "gated" in str(exc).lower():
            raise RuntimeError(gated_model_help()) from exc
        raise

    return token
