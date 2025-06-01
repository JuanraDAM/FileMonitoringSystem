#!/usr/bin/env bash
set -euo pipefail

# Nombre del contenedor y rutas
CONTAINER="hadoop-namenode"
TMP_DIR="/tmp/bank_accounts"
HDFS_DIR="/data/bank_accounts"
HOST_SRC="./src/main/resources/files/bank_accounts"

# 1) preparamos tmp en el contenedor
docker exec "${CONTAINER}" mkdir -p "${TMP_DIR}"

# 2) copiamos todos los CSV desde tu host al tmp del contenedor
docker cp "${HOST_SRC}/." "${CONTAINER}:${TMP_DIR}/"

# 3) desde dentro del contenedor: subimos a HDFS y limpiamos tmp
docker exec "${CONTAINER}" bash -c "
  set -eo pipefail
  # aseguramos directorio destino en HDFS
  hdfs dfs -mkdir -p ${HDFS_DIR}
  # subimos cada CSV
  for f in ${TMP_DIR}/*.csv; do
    echo \"Uploading \$f to HDFS...\"
    hdfs dfs -copyFromLocal -f \"\$f\" ${HDFS_DIR}/
  done
  # limpiamos solo /tmp/bank_accounts dentro del contenedor
  echo \"Cleaning up ${TMP_DIR}...\"
  rm -rf ${TMP_DIR}/*
"

echo "✅ Todos los archivos han sido cargados en HDFS sin afectar al resto de tu clúster."
