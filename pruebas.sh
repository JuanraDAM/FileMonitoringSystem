#!/usr/bin/env zsh
set -euo pipefail

# ─── Variables ────────────────────────────────────────────────────────────────
PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
JAR="$PROJECT_ROOT/target/scala-2.12/Fin_de_Grado-assembly-0.1.0-SNAPSHOT.jar"
CSV_LOCAL_DIR="$PROJECT_ROOT/src/main/resources/files/bank_accounts"
HDFS_CSV_DIR="/data/bank_accounts"
SPARK_SUBMIT="${SPARK_HOME:-/opt/spark}/bin/spark-submit"

echo "=== 🛠 COMPILANDO Y ARMADO (sbt assembly) ==="
cd "$PROJECT_ROOT"
sbt clean assembly

echo "=== 🧪 INICIANDO PRUEBAS ==="

# 1) Postgres dentro del contenedor superset-db
echo -n "🔌 Probando conexión PostgreSQL... "
if docker exec superset-db \
     psql -U superset -d superset -c "SELECT 1" -tA >/dev/null 2>&1; then
  echo "✅ OK"
else
  echo "❌ FALLÓ" && exit 1
fi

## 2) Subir carpeta bank_accounts al HDFS
#echo -n "⬆️ Subiendo carpeta bank_accounts a HDFS... "
#docker cp "$CSV_LOCAL_DIR" hadoop-namenode:/tmp/bank_accounts
#docker exec hadoop-namenode bash -c "\
#  hdfs dfs -mkdir -p $HDFS_CSV_DIR && \
#  hdfs dfs -put -f /tmp/bank_accounts/* $HDFS_CSV_DIR/
#"
#echo "✅ Subida completada"

# 3) Verificar existencia del CSV en HDFS
echo -n "📁 Probando existencia de $HDFS_CSV_DIR en HDFS... "
if docker exec hadoop-namenode \
     hdfs dfs -test -e "$HDFS_CSV_DIR"; then
  echo "✅ Existe"
else
  echo "❌ No existe" && exit 1
fi

# 4) Verificar existencia del JAR
echo -n "📦 Comprobando JAR en $JAR... "
if [[ -f "$JAR" ]]; then
  echo "✅ OK"
else
  echo "❌ No encontrado" && exit 1
fi

# 5) Lanzar Spark job en modo CLIENT (logs en tu consola)
echo "🚀 Lanzando Spark job en modo CLIENT (logs en tu consola)…"

START_NS=$(date +%s%N)

"$SPARK_SUBMIT" \
  --class Main \
  --master spark://localhost:7077 \
  --deploy-mode client \
  --conf spark.driver.host=host.docker.internal \
  "$JAR"

  END_NS=$(date +%s%N)

echo "✅ Job finalizado."
ELAPSED_NS=$((END_NS - START_NS))
ELAPSED_MS=$((ELAPSED_NS / 1000000))
ELAPSED_S=$(awk "BEGIN { printf \"%.3f\", $ELAPSED_MS/1000 }")

echo "⏱ Tiempo total de ejecución: ${ELAPSED_MS} ms (${ELAPSED_S} s)"