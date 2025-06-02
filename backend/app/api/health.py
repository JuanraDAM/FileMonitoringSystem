# backend/app/api/health.py
from fastapi import APIRouter
from pydantic import BaseModel
from datetime import datetime

router = APIRouter()

class HealthResponse(BaseModel):
    status: str
    timestamp: datetime

@router.get("/", response_model=HealthResponse)
async def health_check():
    """
    Endpoint de ejemplo para verificar que la API está viva.
    """
    return HealthResponse(status="ok", timestamp=datetime.utcnow())
