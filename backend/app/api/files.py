import os
from fastapi import APIRouter, Depends, HTTPException, UploadFile, File, status
from fastapi.responses import FileResponse
from sqlalchemy import select, delete
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.session import get_db
from app.core.security import get_current_user
from app.core.config import settings
from app.models.file_configuration import FileConfiguration
from app.models.trigger_control import TriggerControl
from app.schemas.file import FileConfigResponse, FileConfigUpdate
from app.schemas.trigger import TriggerControlLog
from app.services.file_service import save_and_register_file
from app.services.hdfs_sync import push_file_to_hdfs
from subprocess import CalledProcessError

# Se monta en main.py bajo el prefijo /files
router = APIRouter(tags=["files"])

@router.post("/upload", status_code=status.HTTP_201_CREATED)
async def upload_file(
    file: UploadFile = File(...),
    db: AsyncSession = Depends(get_db),
    _=Depends(get_current_user),
):
    """Sube un archivo, lo guarda localmente y registra su configuración."""
    file_id = await save_and_register_file(file, db)
    return {"file_config_id": file_id}

@router.post("/push/{file_name}", status_code=status.HTTP_200_OK)
def push_to_hdfs(
    file_name: str,
    _=Depends(get_current_user)
):
    """Empuja el fichero local a HDFS, manejando espacio y replicas."""
    try:
        push_file_to_hdfs(file_name)
        return {"message": f"Pushed {file_name}"}
    except FileNotFoundError as e:
        raise HTTPException(status_code=404, detail=str(e))
    except RuntimeError as e:
        detail = str(e)
        if "Insufficient HDFS space" in detail:
            raise HTTPException(status_code=507, detail=detail)
        raise HTTPException(status_code=500, detail=detail)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Unexpected error: {e}")

@router.get("/", response_model=list[FileConfigResponse])
async def list_file_configs(
    db: AsyncSession = Depends(get_db),
    _=Depends(get_current_user),
):
    """Lista todas las configuraciones de archivos."""
    result = await db.execute(select(FileConfiguration))
    return result.scalars().all()

@router.get("/logs", response_model=list[TriggerControlLog])
async def list_trigger_logs(
    db: AsyncSession = Depends(get_db),
    _=Depends(get_current_user),
):
    """Lista todos los logs de validación (trigger_control)."""
    result = await db.execute(select(TriggerControl))
    return result.scalars().all()

@router.get("/{config_id}", response_model=FileConfigResponse)
async def get_file_config(
    config_id: int,
    db: AsyncSession = Depends(get_db),
    _=Depends(get_current_user),
):
    """Obtiene una configuración de fichero por su ID."""
    fc = await db.get(FileConfiguration, config_id)
    if not fc:
        raise HTTPException(status_code=404, detail="Configuración no encontrada")
    return fc

@router.patch("/{config_id}", response_model=FileConfigResponse)
async def update_file_config(
    config_id: int,
    update: FileConfigUpdate,
    db: AsyncSession = Depends(get_db),
    _=Depends(get_current_user),
):
    """Actualiza de forma parcial los parámetros de parseo de un fichero."""
    fc = await db.get(FileConfiguration, config_id)
    if not fc:
        raise HTTPException(status_code=404, detail="Configuración no encontrada")
    for field, value in update.dict(exclude_unset=True).items():
        setattr(fc, field, value)
    await db.commit()
    await db.refresh(fc)
    return fc

@router.delete("/{config_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_file_config(
    config_id: int,
    db: AsyncSession = Depends(get_db),
    _=Depends(get_current_user),
):
    """Elimina la configuración y sus dependencias (trigger_control) y el fichero local."""
    fc = await db.get(FileConfiguration, config_id)
    if not fc:
        raise HTTPException(status_code=404, detail="Configuración no encontrada")

    # Eliminar primero logs dependientes
    await db.execute(delete(TriggerControl).where(TriggerControl.file_config_id == config_id))

    # Eliminar fichero local
    local_path = os.path.join(fc.path, fc.file_name)
    if os.path.isfile(local_path):
        os.remove(local_path)

    # Eliminar el registro
    await db.delete(fc)
    await db.commit()

@router.get("/download/{file_name}", response_class=FileResponse)
async def download_file(
    file_name: str,
    _=Depends(get_current_user),
):
    """Descarga el fichero local con streaming de FileResponse."""
    full_path = os.path.join(settings.upload_dir, file_name)
    if not os.path.isfile(full_path):
        raise HTTPException(status_code=404, detail="Fichero no encontrado")
    return FileResponse(path=full_path, filename=file_name)
