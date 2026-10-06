# Pyannote answers "who spoke when?" (anonymous labels like SPEAKER_00).
# It does not transcribe words and does not identify real people.

from app.models.domain import DiarizationSegment
from app.services.config import (
    DIARIZATION_PIPELINE_ID,
    WHISPER_DEVICE,
    gated_model_help,
)
from app.services.hf_auth import prepare_hf_auth


class DiarizationService:
    def __init__(self) -> None:
        self._pipeline = None

    def load(self) -> None:
        if self._pipeline is not None:
            return

        import torch
        from huggingface_hub.errors import GatedRepoError, HfHubHTTPError
        from pyannote.audio import Pipeline

        token = prepare_hf_auth()
        try:
            pipeline = Pipeline.from_pretrained(
                DIARIZATION_PIPELINE_ID,
                token=token,
            )
        except (GatedRepoError, HfHubHTTPError) as exc:
            raise RuntimeError(gated_model_help()) from exc

        if pipeline is None:
            raise RuntimeError(gated_model_help())

        pipeline.to(torch.device(WHISPER_DEVICE))
        self._pipeline = pipeline

    def diarize(self, file_path: str) -> list[DiarizationSegment]:
        import torch
        from faster_whisper.audio import decode_audio

        self.load()
        assert self._pipeline is not None

        # Decode with PyAV (same as Whisper). Avoid torchcodec file-path decode on Windows.
        audio = decode_audio(file_path, sampling_rate=16000)
        waveform = torch.as_tensor(audio, device=WHISPER_DEVICE).to(dtype=torch.float32)
        if waveform.ndim == 1:
            waveform = waveform.unsqueeze(0)
        output = self._pipeline({"waveform": waveform, "sample_rate": 16000})
        return [
            DiarizationSegment(
                start=round(float(start), 2),
                end=round(float(end), 2),
                speaker=_normalize_speaker_label(speaker),
            )
            for start, end, speaker in _iter_speaker_turns(output)
        ]


def _normalize_speaker_label(label: object) -> str:
    text = str(label)
    digits = "".join(character for character in text if character.isdigit())
    if digits == "":
        return "UNKNOWN"
    return f"SPEAKER_{int(digits):02d}"


def _iter_speaker_turns(output):
    diarization = (
        getattr(output, "exclusive_speaker_diarization", None)
        or getattr(output, "speaker_diarization", output)
    )

    if hasattr(diarization, "itertracks"):
        for turn, _, speaker in diarization.itertracks(yield_label=True):
            yield turn.start, turn.end, speaker
        return

    for turn, speaker in diarization:
        yield turn.start, turn.end, speaker
