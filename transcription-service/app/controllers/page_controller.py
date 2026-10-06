from fastapi import APIRouter, HTTPException, Query, status
from fastapi.responses import FileResponse, PlainTextResponse

from app.services.config import VIEWS_DIR

router = APIRouter()
INDEX_PATH = VIEWS_DIR / "index.html"


@router.get("/")
def index() -> FileResponse:
    if not INDEX_PATH.is_file():
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Upload page is missing at app/views/index.html",
        )
    return FileResponse(INDEX_PATH)


@router.get("/query", response_class=PlainTextResponse)
def query_text(query: str = Query(..., min_length=1)) -> str:
    return f"Hello, {query}!"
