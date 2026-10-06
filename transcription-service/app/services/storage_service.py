import uuid
from pathlib import Path

from fastapi import HTTPException, UploadFile, status

from app.services.config import ALLOWED_EXTENSIONS, UPLOADS_DIR


class StorageService:
    """Temporary files only. No database."""

    def save_upload(self, file: UploadFile) -> tuple[str, Path]:
        if file is None or not file.filename:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="No audio file was uploaded. Use multipart field 'file'.",
            )

        original_name = Path(file.filename).name
        suffix = Path(original_name).suffix.lower()
        if suffix not in ALLOWED_EXTENSIONS:
            allowed = ", ".join(sorted(ALLOWED_EXTENSIONS))
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"Unsupported file type '{suffix}'. Allowed types: {allowed}",
            )

        UPLOADS_DIR.mkdir(parents=True, exist_ok=True)
        saved_path = UPLOADS_DIR / f"{uuid.uuid4().hex}_{original_name}"
        with saved_path.open("wb") as buffer:
            buffer.write(file.file.read())
        return original_name, saved_path

    def delete(self, path: Path) -> None:
        path.unlink(missing_ok=True)
