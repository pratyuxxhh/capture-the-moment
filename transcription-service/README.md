# transcription-service (FastAPI)

The speech-to-text microservice of [Capture the Moment](../README.md). It takes one audio file and returns **who said what, and when**:

1. **faster-whisper** turns speech into text segments with timestamps and detects the language.
2. **pyannote.audio** (`pyannote/speaker-diarization-community-1`) finds the speaker turns: `SPEAKER_00`, `SPEAKER_01`, …
3. **Alignment** gives each Whisper segment the speaker whose turn overlaps it the most (`UNKNOWN` if none does).

The service is **stateless**. It has no database. Each upload is written to `uploads/` and deleted when the request finishes. The Spring Boot app calls it and stores the results. It has **no authentication**, so keep port 8000 local.

---

## Run locally

**Requires:** Python 3.11 (recommended; it matches the Docker image), internet on the first run to download models, and Hugging Face access (next section).

```powershell
cd transcription-service
python -m venv .venv
.venv\Scripts\activate                 # macOS/Linux: source .venv/bin/activate
pip install -r requirements.txt
copy .env.example .env                 # macOS/Linux: cp .env.example .env
# edit .env and set HF_TOKEN
uvicorn app.main:app --reload --port 8000 --reload-exclude uploads --reload-exclude .venv
```

| URL | What |
| --- | --- |
| http://localhost:8000/ | simple upload page for testing without Spring |
| http://localhost:8000/docs | Swagger UI |
| http://localhost:8000/health | `{"status": "UP"}` |

The server starts immediately. Whisper and pyannote load on the **first** `POST /transcribe`, so that first request is slow because it downloads the weights into the Hugging Face cache.

Quick test:

```bash
curl -X POST "http://localhost:8000/transcribe" -F "file=@audio/test.mp3"
```

---

## Hugging Face access (required for speaker labels)

The pyannote model is **gated**. A token alone is not enough.

1. Create a **read** token at https://huggingface.co/settings/tokens.
2. Logged in with the **same account**, open [pyannote/speaker-diarization-community-1](https://huggingface.co/pyannote/speaker-diarization-community-1) and click **Agree and access repository**.
3. Wait about a minute, then restart the service.

Until both steps are done, `/transcribe` returns **403** with an explanation.

---

## Configuration (`.env`)

The service reads **`transcription-service/.env`**, not the repo root.

| Variable | Default | Purpose |
| --- | --- | --- |
| `HF_TOKEN` | none (required) | Hugging Face token. Write it without quotes or spaces. `HUGGING_FACE_HUB_TOKEN` also works |
| `WHISPER_MODEL` | `base` | `tiny`, `base`, `small`, `medium`, `large-v3`, `turbo`. Bigger models are more accurate, slower and use more RAM |

Hardware is picked automatically. If `torch.cuda.is_available()` is true, the service uses the GPU with `float16`; otherwise it uses the CPU with `int8`. The startup log shows which one it chose.

---

## API

### `POST /transcribe`

Send `multipart/form-data` with the field `file`. Allowed types: `.mp3`, `.wav`, `.m4a`, `.flac`, `.ogg`.

```json
{
  "filename": "meeting.wav",
  "language": "en",
  "language_probability": 0.98,
  "segments": [
    { "speaker": "SPEAKER_00", "start": 0.0, "end": 4.2, "text": "Let's discuss the database." }
  ]
}
```

| Status | When |
| --- | --- |
| 400 | no file, or an unsupported extension |
| 403 | `HF_TOKEN` missing or invalid, or gated model access not accepted |
| 500 | model or processing failure |

Speaker labels are anonymous clusters, not real names. Requests are processed **one at a time** because a global lock keeps memory use bounded.

### Other routes

- `GET /health` returns the health check.
- `GET /` serves the upload page.
- `GET /query?query=x` returns a test echo.

---

## Code layout (MVC, no database)

```text
app/
  main.py                    FastAPI app; creates MeetingService at startup
  controllers/               HTTP routes: transcription, health, page
  services/
    meeting_service.py       orchestrates one request: save → transcribe → diarize → align → delete
    transcription_service.py faster-whisper (lazy-loaded)
    diarization_service.py   pyannote pipeline (lazy-loaded), audio decoded to 16 kHz
    alignment_service.py     speaker ↔ segment by maximum time overlap
    storage_service.py       temp files in uploads/
    hf_auth.py               validates HF_TOKEN and gated access before downloading
    config.py                paths, allowed types, device selection, .env loading
  models/                    Pydantic response schemas + dataclasses
  views/index.html           test upload page
uploads/                     temporary files only (git-ignored)
audio/                       put local test audio here (git-ignored)
```

---

## Docker image

`Dockerfile` builds on `python:3.11-slim`, installs ffmpeg, runs as a non-root user and exposes port 8000. Pass `HF_TOKEN` as an environment variable when you run it. It is not used during development. To publish, use [`../scripts/build-and-publish.ps1`](../scripts/build-and-publish.ps1).
