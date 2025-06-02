# backend/app/db/session.py
from sqlalchemy.ext.asyncio import create_async_engine, AsyncSession
from sqlalchemy.orm import sessionmaker

from app.core.config import settings

# Crear el engine asíncrono
engine = create_async_engine(
    settings.database_url,
    echo=True,             # DEBUG: ver SQLs en consola
    pool_pre_ping=True,    # detecta conexiones muertas
)

# Crear factory de sesiones
AsyncSessionLocal = sessionmaker(
    engine,
    class_=AsyncSession,
    expire_on_commit=False,
)

# Dependencia para inyectar session en endpoints
async def get_db():
    async with AsyncSessionLocal() as session:
        yield session
