#!/usr/bin/env bash
set -euo pipefail

# 1) Asegúrate de estar en la raíz del proyecto
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# 2) Compilar y ensamblar el fat JAR
echo "=== 🛠 Compilando y ensamblando con sbt ==="
sbt clean assembly

# 3) Crear la red Docker si no existe
NETWORK="superset-net"
if ! docker network ls --format '{{.Name}}' | grep -qw "${NETWORK}"; then
  echo "=== 🌐 Creando red Docker '${NETWORK}' ==="
  docker network create "${NETWORK}"
else
  echo "=== 🌐 La red '${NETWORK}' ya existe ==="
fi

# 4) Ir al directorio docker
cd docker

# 5) Reconstruir solo la imagen validation-engine
echo "=== 🐳 Reconstruyendo la imagen Docker 'validation-engine' ==="
docker-compose build validation-engine

# 6) Arrancar el servicio validation-engine y esperar a que termine
echo "=== ▶️ Lanzando 'validation-engine' (se detendrá al finalizar) ==="
docker-compose up --abort-on-container-exit validation-engine
