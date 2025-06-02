# backend/app/services/hdfs_sync.py

import os
import subprocess
from subprocess import CalledProcessError

from app.core.config import settings

def push_file_to_hdfs(file_name: str):
    container   = settings.hdfs_namenode_container
    local_path  = os.path.join(settings.upload_dir, file_name)
    remote_dir  = settings.hdfs_dir
    tmp_dir     = settings.hdfs_tmp_dir
    remote_tmp  = f"{tmp_dir}/{file_name}"
    remote_file = f"{remote_dir}/{file_name}"

    # 0) Comprobar que el fichero local existe
    if not os.path.isfile(local_path):
        raise FileNotFoundError(f"Local not found: {local_path}")

    # 1) Obtener tamaño local y espacio libre en HDFS
    local_size = os.path.getsize(local_path)
    report = subprocess.check_output(
        ["docker", "exec", container, "hdfs", "dfsadmin", "-report"],
        text=True,
        stderr=subprocess.DEVNULL
    )
    remaining = 0
    for line in report.splitlines():
        if line.strip().startswith("DFS Remaining:"):
            # "DFS Remaining: 760250368 (725.03 MB)"
            remaining = int(line.split()[2])
            break
    if remaining < local_size:
        raise RuntimeError(f"Insufficient HDFS space: need {local_size} bytes, have {remaining} bytes")

    try:
        # 2) Salir de safe-mode
        subprocess.run(
            ["docker", "exec", container, "hdfs", "dfsadmin", "-safemode", "leave"],
            check=True,
        )

        # 3) Asegurar tmp en contenedor
        subprocess.run(
            ["docker", "exec", container, "mkdir", "-p", tmp_dir],
            check=True,
        )

        # 4) Copiar local -> tmp
        subprocess.run(
            ["docker", "cp", local_path, f"{container}:{remote_tmp}"],
            check=True,
        )

        # 5) Limpiar residuos en HDFS (.COPYING_ y antiguo)
        for path in (f"{remote_file}._COPYING_", remote_file):
            subprocess.run(
                ["docker", "exec", container, "hdfs", "dfs", "-rm", "-f", path],
                check=False,
            )

        # 6) Desde bash: crear dir, forzar réplica=1, copiar y limpiar tmp
        cmd = f"""
          set -eo pipefail
          hdfs dfs -mkdir -p {remote_dir}
          hdfs dfs -setrep -R 1 {remote_dir}
          hdfs dfs -copyFromLocal -f {remote_tmp} {remote_dir}/
          rm -f {remote_tmp}
        """
        subprocess.run(
            ["docker", "exec", container, "bash", "-c", cmd],
            check=True,
        )

    except CalledProcessError as e:
        raise RuntimeError(f"HDFS push failed: {e}") from e

    print(f"[hdfs_sync] '{file_name}' pushed to HDFS at {remote_dir}/")
