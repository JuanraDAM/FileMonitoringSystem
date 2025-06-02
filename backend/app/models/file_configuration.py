# backend/app/models/file_configuration.py
from sqlalchemy import (
    Column, Integer, String, Boolean,
    CHAR, VARCHAR,
)
from app.db.base import Base

class FileConfiguration(Base):
    __tablename__ = "file_configuration"

    id                = Column(Integer, primary_key=True, index=True)
    file_format       = Column(VARCHAR(50),  nullable=False)
    path              = Column(VARCHAR(500), nullable=False)
    file_name         = Column(VARCHAR(255), nullable=False)
    has_header        = Column(Boolean,     nullable=False, default=True)
    delimiter         = Column(CHAR(1),     nullable=False, default=",")
    quote_char        = Column(CHAR(1),     nullable=False, default='"')
    escape_char       = Column(CHAR(1),     nullable=False, default="\\")
    date_format       = Column(VARCHAR(50), nullable=True,  default="yyyy-MM-dd")
    timestamp_format  = Column(VARCHAR(50), nullable=True,  default="yyyy-MM-dd HH:mm:ss")
    partition_columns = Column(VARCHAR(255), nullable=True)
