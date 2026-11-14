# Build and publish both Docker images to Docker Hub.
#
# NOT part of the normal development loop. Day-to-day you run the two services
# directly on your machine (see README.md). Run this only when you want to
# publish a release of both images at once.
#
# Usage (from anywhere):
#   docker login            # once
#   .\scripts\build-and-publish.ps1

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot

Write-Host "==> 1. Building and publishing Spring Boot app..." -ForegroundColor Cyan
docker build --pull -t pratyuxxhh/capture-the-moment:latest "$repoRoot\capture-the-moment"
docker push pratyuxxhh/capture-the-moment:latest

Write-Host "==> 2. Building and publishing Transcription Service..." -ForegroundColor Cyan
docker build --pull -t pratyuxxhh/transcription-service:latest "$repoRoot\transcription-service"
docker push pratyuxxhh/transcription-service:latest

Write-Host "==> Done. Both images pushed." -ForegroundColor Green
