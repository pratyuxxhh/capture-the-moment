# Capture-The-Moment — Local Development

Run MySQL and MinIO in Docker while running the Spring Boot and transcription services locally.

## Prerequisites

Create a `.env` file in the root directory:

```text
HF_TOKEN=hf_your_token_here
WHISPER_MODEL=base
```

## Run locally

Start the database and object storage:

```powershell
docker compose up -d
```

Run the Spring Boot backend:

```powershell
cd capture-the-moment
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

Run the transcription service when needed:

```powershell
cd transcription-service
.venv\Scripts\activate
uvicorn app.main:app --reload --port 8000
```

Services:

- Web app: `http://localhost:8080`
- Transcription API: `http://localhost:8000`
- Swagger: `http://localhost:8000/docs`
- MinIO console: `http://localhost:9001`

Stop the local infrastructure:

```powershell
docker compose down
```
