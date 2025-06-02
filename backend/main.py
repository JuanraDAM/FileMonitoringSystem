# backend/main.py

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from dotenv import load_dotenv
from sqlalchemy import text
from app.db.session import engine

load_dotenv()

app = FastAPI(title="File Monitoring Service")

@app.on_event("startup")
async def create_users_table():
    sql = text("""
    CREATE TABLE IF NOT EXISTS users (
        id SERIAL PRIMARY KEY,
        email VARCHAR(255) UNIQUE NOT NULL,
        hashed_password VARCHAR(255) NOT NULL,
        is_active BOOLEAN NOT NULL DEFAULT TRUE,
        created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
        updated_at TIMESTAMPTZ
    );
    """)
    async with engine.begin() as conn:
        await conn.execute(sql)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"], allow_methods=["*"], allow_headers=["*"],
)

from app.api.health import router as health_router
from app.api.auth   import router as auth_router
from app.api.files  import router as files_router

app.include_router(health_router, prefix="/health")
app.include_router(auth_router,   prefix="/auth")
app.include_router(files_router,  prefix="/files")
