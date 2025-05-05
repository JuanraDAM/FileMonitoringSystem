#!/usr/bin/env bash
set -euo pipefail

### CONFIGURACIÓN ###
# Nombre del contenedor NameNode y Spark Master
NN_CONTAINER="hadoop-namenode"
MASTER_CONTAINER="spark-master"

# Directorio HDFS donde subir el JAR
HDFS_DIR="/jars"

# Ruta relativa a tu fat-JAR en el proyecto
JAR_LOCAL_PATH="target/scala-2.12/Fin_de_Grado-assembly-0.1.0-SNAPSHOT.jar"
JAR_NAME="$(basename "$JAR_LOCAL_PATH")"

# URL completo desde Spark
HDFS_JAR_PATH="hdfs://hadoop-namenode:9000${HDFS_DIR}/${JAR_NAME}"


echo "🔍 Comprobando existencia de JAR local: $JAR_LOCAL_PATH"
if [ ! -f "$JAR_LOCAL_PATH" ]; then
  echo "❌ No se encontró el JAR en $JAR_LOCAL_PATH"
  exit 1
fi

echo "📦 Copiando $JAR_NAME al contenedor NameNode ($NN_CONTAINER)..."
docker cp "$JAR_LOCAL_PATH" "$NN_CONTAINER":/tmp/"$JAR_NAME"

echo "🚀 Subiendo JAR a HDFS ($HDFS_DIR)..."
docker exec "$NN_CONTAINER" bash -c "\
  hdfs dfs -mkdir -p '$HDFS_DIR' && \
  hdfs dfs -put -f '/tmp/$JAR_NAME' '$HDFS_DIR/' \
"

echo "🧹 Limpiando JAR temporal en NameNode..."
docker exec "$NN_CONTAINER" rm -f /tmp/"$JAR_NAME"

echo "✨ Lanzando Spark job en cluster mode..."
docker exec "$MASTER_CONTAINER" bash -c "\
  /opt/bitnami/spark/bin/spark-submit \
    --class Main \
    --master spark://spark-master:7077 \
    --deploy-mode cluster \
    --supervise \
    --conf spark.driver.host=spark-driver \
    --conf spark.driver.bindAddress=0.0.0.0 \
    --conf spark.default.parallelism=12 \
    '$HDFS_JAR_PATH' \
"

echo "✅ Job enviado correctamente. Revisa los logs en el Spark UI (http://localhost:8080)."
