from fastapi import APIRouter, Depends, File, Request, UploadFile

from app.models.schemas import TranscribeResponse
from app.services.meeting_service import MeetingService

router = APIRouter()


def get_meeting_service(request: Request) -> MeetingService:
    return request.app.state.meeting_service


@router.post("/transcribe", response_model=TranscribeResponse)
def transcribe(
    file: UploadFile | None = File(default=None),
    meeting_service: MeetingService = Depends(get_meeting_service),
) -> TranscribeResponse:
    return meeting_service.transcribe_upload(file)
