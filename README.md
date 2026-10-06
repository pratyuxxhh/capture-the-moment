# Capture-The-Moment — Run Guide & Profiles

This repository supports two execution profiles:
1. **Local Development Profile (`local`)** — Runs infrastructure in Docker (MySQL + MinIO) and apps locally on your machine.
2. **Full Docker Profile (`full`)** — Runs everything containerized in Docker using the latest published Docker Hub images.

---

## 🛠️ Prerequisites

Ensure you have a `.env` file in the root directory containing your Hugging Face access token:
```text
HF_TOKEN=hf_your_token_here
WHISPER_MODEL=base
```

---

## 💻 1. Local Development Profile (`local`)

In this mode, Docker runs only the database and storage services. You run Spring Boot and Python directly on your host machine for fast development and live reload.

### Step 1: Start Infrastructure (MySQL & MinIO)
Run from the root directory:
```powershell
docker compose up -d
```
- **MySQL**: `localhost:3307` (Database: `myapp_local`, User: `spring`, Password: `spring`)
- **MinIO S3**: `http://localhost:9000` (Bucket: `capture-the-moment-local`)
- **MinIO Console UI**: `http://localhost:9001` (User: `admin`, Password: `admin12345`)

### Step 2: Run Spring Boot Backend
```powershell
cd capture-the-moment
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```
- **Spring Boot URL**: `http://localhost:8080`

### Step 3: Run Transcription Service (Optional / as needed)
```powershell
cd transcription-service
.venv\Scripts\activate
uvicorn app.main:app --reload --port 8000
```
- **Transcription Service URL**: `http://localhost:8000`
- **Swagger Docs**: `http://localhost:8000/docs`

### Stop Local Infrastructure:
```powershell
docker compose down
```

---

## 🐳 2. Full Docker Profile (`full`)

Runs all 4 services inside Docker (`mysql`, `minio`, `transcription-service`, `capture-the-moment`) using the latest pushed images from Docker Hub.

### Step 1: Start Everything
Run from the root directory:
```powershell
docker compose --profile full up -d
```
- **Spring Boot Web App**: `http://localhost:8080`
- **Transcription Service**: `http://localhost:8000`
- **MinIO Console UI**: `http://localhost:9001`
- **MySQL Database**: `localhost:3307` (Schema: `myapp_docker`)
- **MinIO S3 Bucket**: `capture-the-moment-docker`

### Step 2: (Optional) With NVIDIA GPU Acceleration
If you have an NVIDIA GPU and NVIDIA Container Toolkit installed:
```powershell
docker compose -f docker-compose.yml -f docker-compose.gpu.yml --profile full up -d
```

### Stop Full Stack:
```powershell
docker compose --profile full down
```

---

## 🚀 3. Build & Publish to Docker Hub

To build fresh images, push them to Docker Hub, and restart the running Docker containers with the new versions, execute the included script:

```powershell
.\build_and_publish.ps1
```

---

## 📊 Summary of Environments & Separation

| Resource | Local Profile (`local`) | Full Docker Profile (`full`) |
| :--- | :--- | :--- |
| **Spring Boot Profile** | `local` | `docker` |
| **MySQL Schema** | `myapp_local` | `myapp_docker` |
| **MinIO S3 Bucket** | `capture-the-moment-local` | `capture-the-moment-docker` |
| **Spring Boot Host** | Local machine (`localhost:8080`) | Docker container (`8080:8080`) |
| **Transcription Host** | Local machine (`localhost:8080`) | Docker container (`8000:8000`) |
| **MinIO Endpoint** | `http://localhost:9000` | `http://minio:9000` |
