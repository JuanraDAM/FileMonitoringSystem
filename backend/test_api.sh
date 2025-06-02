#!/usr/bin/env bash
set -euo pipefail

# ——— Determinar la ruta absoluta del script ———
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# ——— Parámetros ———
BASE_URL="http://localhost:8000"
EMAIL="usuario@ejemplo.com"
PASSWORD="miPassword123"

# ——— Localizar el CSV ———
if [[ -f "$SCRIPT_DIR/uploaded_files/bank_accounts.csv" ]]; then
  FILE_PATH="$SCRIPT_DIR/uploaded_files/bank_accounts.csv"
elif [[ -f "$SCRIPT_DIR/bank_accounts.csv" ]]; then
  FILE_PATH="$SCRIPT_DIR/bank_accounts.csv"
else
  echo "ERROR: no existe bank_accounts.csv ni en uploaded_files ni en la raíz del proyecto"
  exit 1
fi
FILE_NAME="$(basename "$FILE_PATH")"
echo "Usando archivo: $FILE_PATH"

# 1) Health check
echo -e "\n==> GET  $BASE_URL/health/"
curl -i "$BASE_URL/health/"

# 2) Register
echo -e "\n==> POST $BASE_URL/auth/register"
curl -i -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}"

# 3) Login + extraer token
echo -e "\n==> POST $BASE_URL/auth/login"
TOKEN=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}" \
  | jq -r .access_token)
echo "→ Token obtenido: $TOKEN"

# 4) Upload
echo -e "\n==> POST $BASE_URL/files/upload"
curl -i -X POST "$BASE_URL/files/upload" \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@${FILE_PATH}"

# 5) Push to HDFS
echo -e "\n==> POST $BASE_URL/files/push/$FILE_NAME"
curl -i -X POST "$BASE_URL/files/push/$FILE_NAME" \
  -H "Authorization: Bearer $TOKEN"

# 6) List configurations
echo -e "\n==> GET $BASE_URL/files/"
curl -i "$BASE_URL/files/" \
  -H "Authorization: Bearer $TOKEN"

# 7) List trigger logs
echo -e "\n==> GET $BASE_URL/files/logs"
curl -i "$BASE_URL/files/logs" \
  -H "Authorization: Bearer $TOKEN"

# 8) Get config by ID (1)
echo -e "\n==> GET $BASE_URL/files/1"
curl -i "$BASE_URL/files/1" \
  -H "Authorization: Bearer $TOKEN"

# 9) Update config ID=1 (PATCH)
echo -e "\n==> PATCH $BASE_URL/files/1"
curl -i -X PATCH "$BASE_URL/files/1" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"delimiter":";","quote_char":"\""}'

# 10) Delete config ID=1
echo -e "\n==> DELETE $BASE_URL/files/1"
curl -i -X DELETE "$BASE_URL/files/1" \
  -H "Authorization: Bearer $TOKEN"

# 11) Download file
echo -e "\n==> GET $BASE_URL/files/download/$FILE_NAME"
curl -J -o "$FILE_NAME" "$BASE_URL/files/download/$FILE_NAME" \
  -H "Authorization: Bearer $TOKEN"

echo -e "\n✅ ¡Pruebas completadas!"

