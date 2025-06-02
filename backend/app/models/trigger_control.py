# backend/app/models/trigger_control.py
from sqlalchemy import (
    Column, Integer, String, Text,
    ForeignKey, DateTime, func, VARCHAR
)
from sqlalchemy.orm import relationship
from app.db.base import Base

class TriggerControl(Base):
    __tablename__ = "trigger_control"

    id               = Column(Integer, primary_key=True, index=True)
    file_config_id   = Column(Integer, ForeignKey("file_configuration.id"), nullable=False)
    file_name        = Column(VARCHAR(255), nullable=False)
    field_name       = Column(VARCHAR(255), nullable=True)
    environment      = Column(VARCHAR(50),  nullable=False)
    validation_flag  = Column(VARCHAR(50),  nullable=False)
    error_message    = Column(Text,          nullable=True)
    logged_at        = Column(DateTime(timezone=True), nullable=False, server_default=func.now())

    file_configuration = relationship("FileConfiguration", backref="triggers")
