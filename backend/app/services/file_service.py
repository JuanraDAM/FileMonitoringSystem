
import os
import shutil
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.config import settings
from app.models.file_configuration import FileConfiguration

async def save_and_register_file(file, db: AsyncSession) -> int:
    # 1) Guardamos localmente en upload_dir
    os.makedirs(settings.upload_dir, exist_ok=True)
    dest = os.path.join(settings.upload_dir, file.filename)
    with open(dest, "wb") as out:
        shutil.copyfileobj(file.file, out)

    # 2) Determinamos el path fijo para BD (HDFS)
    hdfs_path = settings.hdfs_dir.rstrip('/') + '/'

    # 3) Buscamos un registro existente solo por file_name
    q = select(FileConfiguration).where(
        FileConfiguration.file_name == file.filename
    )
    res = await db.execute(q)
    fc = res.scalar_one_or_none()

    # 4) Si existe, actualizamos; si no, creamos nuevo) Si existe, actualizamos; si no, creamos nuevo
    if fc:
        fc.has_header        = True
        fc.delimiter         = ","
        fc.quote_char        = '"'
        fc.escape_char       = "\\"
        fc.date_format       = "yyyy-MM-dd"
        fc.timestamp_format  = "yyyy-MM-dd HH:mm:ss"
        fc.partition_columns = None
    else:
        fc = FileConfiguration(
            file_format       = "csv",
            path              = hdfs_path,
            file_name         = file.filename,
            has_header        = True,
            delimiter         = ",",
            quote_char        = '"',
            escape_char       = "\\",
            date_format       = "yyyy-MM-dd",
            timestamp_format  = "yyyy-MM-dd HH:mm:ss",
            partition_columns = None,
        )
        db.add(fc)

    # 5) Commit y refrescar para persistir cambios
    await db.commit()
    await db.refresh(fc)

    return fc.id
