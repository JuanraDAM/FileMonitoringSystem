from pydantic import BaseModel, Field
from datetime import datetime
from typing import Optional

class TriggerControlLog(BaseModel):
    id: int
    file_config_id: int
    file_name: str
    field_name: Optional[str]
    environment: str
    validation_flag: str
    error_message: Optional[str]
    logged_at: datetime

    class Config:
        orm_mode = True
