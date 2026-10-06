# Transcription Service

Local CPU transcription and speaker diarization. **MVC, no database.**

| Layer | Folder | Role |
| --- | --- | --- |
| Model | `app/models` | JSON schemas and in-memory dataclasses |
| View | `app/views` | Upload HTML |
| Controller | `app/controllers` | HTTP routes |
| Service | `app/services` | Whisper, pyannote, alignment, temp files |

Nothing is persisted in PostgreSQL, Redis, or any other store. Uploads are deleted after each request.

Whisper produces text + timestamps. Pyannote produces anonymous speaker intervals (`SPEAKER_00`). Overlap alignment merges them. CUDA is not required.

## Why it fails after only adding a token

`HF_TOKEN` authenticates you. It does **not** grant the gated model by itself.

You must also, while logged into **the same Hugging Face account** as the token:

1. Open [pyannote/speaker-diarization-community-1](https://huggingface.co/pyannote/speaker-diarization-community-1)
2. Click **Agree and access repository**
3. Wait about a minute, then restart the app

Until that is done, Hugging Face returns **403** and diarization cannot start.

## Hugging Face token

```text
HF_TOKEN=hf_your_token_here
```

No quotes, no spaces around `=`. Keep `.env` out of git. Use a **read** token.

## Local setup

```bash
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
uvicorn app.main:app --reload --reload-exclude uploads --reload-exclude .venv
```

The HTTP server starts immediately. Whisper and pyannote download on the **first** `POST /transcribe`.

- Docs: http://localhost:8000/docs
- Form: http://localhost:8000/
- Health: http://localhost:8000/health

Optional: `WHISPER_MODEL=turbo` in `.env` (slower, more RAM). Default is `base`.

## API

`POST /transcribe` — `multipart/form-data` field `file`. JSON only.

```json
{
  "filename": "meeting.wav",
  "language": "en",
  "language_probability": 0.98,
  "segments": [
    {
      "speaker": "SPEAKER_00",
      "start": 0.0,
      "end": 4.2,
      "text": "Let's discuss the database."
    }
  ]
}
```

`SPEAKER_00` is not a person's name.

## Testing

```bash
curl -X POST "http://localhost:8000/transcribe" -F "file=@audio/test.mp3"
```

## Layout

```text
app/main.py
app/controllers/     HTTP
app/models/          schemas (no DB)
app/views/           HTML
app/services/        transcription, diarization, alignment
uploads/             temp audio only
```
