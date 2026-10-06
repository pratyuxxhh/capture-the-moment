# Build and Publish Helper Script for Capture-The-Moment
# Usage: Run commands individually or execute this script in PowerShell

# 1. Login to Docker Hub (Run once)
# docker login

Write-Host "==> 1. Building and publishing Spring Boot app..." -ForegroundColor Cyan
Set-Location -Path "$PSScriptRoot\capture-the-moment"
docker build --pull -t pratyuxxhh/capture-the-moment:latest .
docker push pratyuxxhh/capture-the-moment:latest

Write-Host "==> 2. Building and publishing Transcription Service..." -ForegroundColor Cyan
Set-Location -Path "$PSScriptRoot\transcription-service"
docker build --pull -t pratyuxxhh/transcription-service:latest .
docker push pratyuxxhh/transcription-service:latest

Set-Location -Path "$PSScriptRoot"

Write-Host "==> 3. Pulling and restarting latest containers in Docker..." -ForegroundColor Cyan
docker compose --profile full pull capture-the-moment transcription-service
docker compose --profile full up -d --force-recreate capture-the-moment transcription-service

Write-Host "==> Deployment complete! Checking container status..." -ForegroundColor Green
docker compose --profile full ps
