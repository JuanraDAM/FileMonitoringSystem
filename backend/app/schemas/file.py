# File: backend/app/schemas/file.py
from pydantic import BaseModel, Field
from typing import Optional

class FileConfigBase(BaseModel):
    has_header: bool = Field(..., description="Indica si tiene cabecera")
    delimiter: str = Field(..., min_length=1, max_length=1)
    quote_char: str = Field(..., min_length=1, max_length=1)
    escape_char: str = Field(..., min_length=1, max_length=1)
    date_format: Optional[str] = Field(None, description="Formato de fecha")
    timestamp_format: Optional[str] = Field(None, description="Formato de timestamp")
    partition_columns: Optional[str] = Field(None, description="Columnas de partición (coma-separated)")

class FileConfigCreate(FileConfigBase):
    file_format: str = Field(..., description="Formato del fichero, p.ej. 'csv'")
    file_name: str   = Field(..., description="Nombre del fichero")
    path: str        = Field(..., description="Ruta donde está el fichero")

class FileConfigUpdate(BaseModel):
    has_header: Optional[bool] = None
    delimiter: Optional[str]   = None
    quote_char: Optional[str]  = None
    escape_char: Optional[str] = None
    date_format: Optional[str] = None
    timestamp_format: Optional[str] = None
    partition_columns: Optional[str] = None

class FileConfigResponse(FileConfigCreate):
    id: int

    class Config:
        orm_mode = True