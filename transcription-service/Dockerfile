FROM python:3.11-slim

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends ffmpeg \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install \
    --no-cache-dir \
    --prefer-binary \
    --default-timeout=300 \
    --retries=10 \
    -r requirements.txt

# The image does not require an NVIDIA runtime. When one is provided by the
# host, the PyTorch wheel can use CUDA; otherwise torch.cuda.is_available()
# remains false and the application selects CPU inference.

COPY app/ ./app/

RUN useradd --create-home --shell /usr/sbin/nologin appuser \
    && mkdir -p /home/appuser/.cache/huggingface /app/uploads \
    && chown -R appuser:appuser /app /home/appuser

ENV HF_HOME=/home/appuser/.cache/huggingface \
    PYTHONUNBUFFERED=1

USER appuser

EXPOSE 8000

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
