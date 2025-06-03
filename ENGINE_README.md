# Motor de Validaciones con Scala y Apache Spark

Este proyecto implementa un motor de validaciones distribuido para procesar ficheros bancarios en HDFS, utilizando **Scala**, **Apache Spark** y **PostgreSQL** dentro de un entorno **Docker Compose**.

## Índice

1. [Introducción](#introducción)
   1. [Resumen del proyecto](#introducción)
   2. [Explicación de la aplicación](#explicación-de-la-aplicación)
   3. [Resumen de tecnologías utilizadas](#resumen-de-tecnologías-utilizadas)

2. [Especificación de Requisitos](#especificación-de-requisitos)
   1. [Requisitos funcionales](#requisitos-funcionales)
   2. [Requisitos no funcionales](#requisitos-no-funcionales)

3. [Diseño (Diagramas)](#diseño-diagramas)
   1. [Casos de uso](#casos-de-uso)
   2. [Diagrama entidad-relación](#diagrama-entidad-relación)
   3. [Esquema para BD no relacional](#esquema-para-bd-no-relacional)
   4. [Diagrama de clases del modelo](#diagrama-de-clases-del-modelo)
   5. [Diagramas de secuencia](#diagramas-de-secuencia)

4. [Implementación (GIT & Docker)](#implementación-git--docker)
   1. [Estructura del repositorio](#estructura-del-repositorio)
   2. [Multi-Stage Dockerfile (`docker/Dockerfile.engine`)](#multi-stage-dockerfile-dockerdockerfileengine)
   3. [Docker Compose (`docker/docker-compose.yml`)](#docker-compose-dockerdocker-composeyml)
   4. [Script de reconstrucción (`scripts/rebuild_and_run.sh`)](#script-de-reconstrucción-scriptsrebuild_and_runsh)

5. [Resultado (Manual de usuario)](#resultado-manual-de-usuario)
   1. [Requisitos previos](#requisitos-previos)
   2. [Pasos de uso](#pasos-de-uso)
   3. [Configuración de Superset (breve)](#configuración-de-superset-breve)

6. [Conclusiones](#conclusiones)
   1. [Dificultades](#dificultades)
   2. [Mejoras futuras](#mejoras-futuras)

---

## 1. Introducción

### 1.1 Resumen del proyecto

Este proyecto de Fin de Grado nace de la necesidad de garantizar la **calidad** y la **integridad** de los datos bancarios que llegan periódicamente en forma de ficheros al clúster HDFS. Mediante un motor de validaciones implementado en **Scala** y sobre la plataforma **Apache Spark**, se procesan grandes volúmenes de datos de manera distribuida, aplicando una serie de comprobaciones en varias capas:

* **Estructural**: formato y número de columnas
* **Tipológica**: tipos de datos y rangos válidos
* **Referencial**: unicidad y coherencia con metadatos
* **Funcional**: reglas de negocio específicas del dominio bancario

Cada validación se registra con detalle en una tabla de logs en **PostgreSQL**, permitiendo auditoría, estadísticas de calidad y alertas automatizadas.

El flujo general es:

1. El operador sube un fichero a HDFS.
2. **Main.scala** detecta el fichero, invoca el motor de validaciones.
3. Se ejecutan los validadores en cadena; si falla alguno, se abandona con el código correspondiente.
4. Se registra el resultado en la tabla `trigger_control` de PostgreSQL.
5. Se elimina el fichero de HDFS para evitar reprocesamiento.

Desde la versión actual, al arrancar el contenedor de validación se ejecuta `hdfs dfsadmin -safemode leave` para asegurarse de salir del modo seguro de HDFS.

### 1.2 Explicación de la aplicación

La aplicación se compone de los siguientes módulos:

1. **Main.scala**

   * Implementa un **bucle de polling** que observa el directorio HDFS (`/data/bank_accounts`).
   * Al detectar un fichero, llama a `ExecutionManager.executeFile(path, outputTable)`.
   * Tras procesar, borra el fichero de HDFS.

2. **DbConfig & DBConnection**

   * Leen credenciales desde `db.properties`.
   * Gestionan la creación de conexiones JDBC (PostgreSQL), con retry en caso de fallo.

3. **SparkSessionProvider**

   * Configura la `SparkSession` con parámetros de master, serialización (Kryo), particionamiento y acceso a HDFS.

4. **ExecutionManager**

   * Orquesta las fases de validación.
   * En `executeFile`:

      1. Lee el fichero con `Reader.readFile(...)`.
      2. Llama a `FileSentinel.verifyFiles(...)`.
      3. Si pasa, continúa con `TypeValidator.verifyTyping(...)`.
      4. Luego `ReferentialIntegrityValidator.verifyIntegrity(...)`.
      5. Finalmente `FunctionalValidator.verifyFunctional(...)`.
      6. Si todo OK, registra flag “2”.

5. **Validators**

   * **FileSentinel**: verifica delimitadores, encabezados y número de columnas.
   * **TypeValidator**: comprueba tipos, nullabilidad, longitudes y formato de texto según metadatos (`semantic_layer`).
   * **ReferentialIntegrityValidator**: asegura unicidad de claves primarias.
   * **FunctionalValidator**: aplica reglas de negocio (formato de cuenta, rangos de credit\_score, balance según estado, etc.).

6. **Utils**

   * **Reader**: carga tablas JDBC con particionamiento y ficheros CSV.
   * **Writer**: escribe logs a JDBC (append).

7. **TriggerIdManager**

   * Genera IDs de trigger únicos basándose en el máximo histórico en BD, para evitar colisiones en entornos distribuidos.

Esta arquitectura modular permite:

* Añadir o modificar validadores sin tocar la lógica central.
* Cambiar backend de almacenamiento (p. ej. MongoDB en lugar de PostgreSQL) sin reescribir el núcleo.
* Integrar futuras APIs (FastAPI) o frontends (React) para subir ficheros directamente a HDFS y mostrar resultados en tiempo real.

### 1.3 Resumen de tecnologías utilizadas

* **Scala 2.12**: lenguaje funcional y orientado a objetos para Spark.
* **Apache Spark 3.x**: procesamiento distribuido in-memory.
* **HDFS (Hadoop 3.x)**: almacenamiento distribuido de ficheros.
* **PostgreSQL 13**: base de datos relacional para logs.
* **Kryo**: serializador ligero, con registrator personalizado para `TimestampType` y `ByteBuffer`.
* **SBT**: gestor de proyectos, compilación y ensamblado.
* **Docker & Docker Compose**: contenedorización de Spark, Hadoop, PostgreSQL, Kafka, Superset y el motor de validaciones.
* **TypeSafe Config**: gestión de configuración mediante `application.conf`.
* **ScalaTest**: framework para pruebas unitarias.

---

## 2. Especificación de Requisitos

### 2.1 Requisitos funcionales

1. **Monitorización de HDFS**

   * Detectar automáticamente nuevos ficheros en `/data/bank_accounts`.
2. **Procesamiento por fichero**

   * Cada archivo se procesa de forma independiente.
3. **Validación estructural**

   * Verificar número exacto y orden de columnas conforme a `file_configuration`.
   * Generar flag 32 (Delimiter mismatch), 33 (Header mismatch) o 34 (Column count per row mismatch).
4. **Validación tipológica**

   * Asegurar tipos de datos, rangos, formato de fecha y timestamp según `semantic_layer`.
   * Generar flag 35 (Tipo inválido), 36 (Nulo indebido), 37 (Longitud excedida) o 38 (Formato texto inválido).
5. **Integridad referencial**

   * Comprobar unicidad de claves primarias según metadatos (`semantic_layer`).
   * Generar flag 39 (Duplicado PK).
6. **Validación de negocio**

   * Aplicar reglas:

      * Formato de cuenta (`^[A-Za-z0-9]{10}$`) → flag 40 (Formato inválido).
      * `credit_score` entre 300 y 850 → flag 41 (Fuera de rango).
      * `risk_score` entre 0 y 100 → flag 42 (Fuera de rango).
      * Mayor de 18 años (DOB) → flag 43 (Menor de edad).
      * Si `status`=“Active”, balance >= 0; si “Closed”, balance = 0 → flags 44/45.
      * Si `account_type`=“Checking”, `interest_rate`=0 → flag 46.
      * `overdraft_limit`>=0 y válidos según estado → flag 47.
      * Si `is_joint_account`=“Yes”, `num_transactions`>=2 → flag 48.
      * Si `num_transactions`=0, `avg_transaction_amount`=0 → flag 49.
7. **Registro de resultados**

   * Insertar logs detallados en `trigger_control` con campos:

      * `logged_at` (timestamp)
      * `file_config_id`, `file_name`, `field_name`, `environment`, `validation_flag`, `error_message`
8. **Limpieza de ficheros**

   * Una vez procesados (OK o KO), borrar de HDFS para evitar reprocesamiento.

### 2.2 Requisitos no funcionales

* **Rendimiento**

   * Procesar >1M filas por partición en <1 min.
   * Uso óptimo de particiones Spark y `fetchSize` en JDBC.
* **Escalabilidad**

   * Aumentar nodos executor sin cambios en el código.
   * Volúmenes (`SPARK_LOCAL_DIRS`) configurables para no agotar disco.
* **Disponibilidad**

   * Debe mantenerse activo 24/7.
   * Al arrancar, ejecutar `hdfs dfsadmin -safemode leave` para asegurar que HDFS no esté en modo seguro.
* **Configurabilidad**

   * Parámetros (rutas, credenciales, intervalos de polling) en `application.conf` y variables de entorno.
* **Tolerancia a Fallos**

   * Capturar y loguear excepciones (sin detener el bucle principal).
* **Seguridad**

   * Gestión de permisos HDFS (`hdfs dfs -chmod -R 777`) cuando se suben ficheros.
   * Credenciales BD en `db.properties`, no hardcodeadas.
* **Mantenibilidad**

   * Código documentado con ScalaDoc.
   * Arquitectura modular (SOLID, Clean Architecture).

---

## 3. Diseño (Diagramas)

> A continuación se describen los diagramas que acompañarán esta sección; se incluirán en la versión PDF o en la carpeta `docs/`.

### 3.1 Casos de uso

**Actores**:

* **Operador**: deposita ficheros en HDFS o hace POST a la API.
* **Sistema de validación**: detecta y procesa ficheros.
* **Analista**: consulta resultados en BD (PostgreSQL) o en Superset.

**Flujo principal**:

1. Operador hace `hdfs dfs -put` o llama a la API para subir fichero a HDFS.
2. **Main.scala** detecta fichero → llama a `ExecutionManager.executeFile`.
3. Los validadores ejecutan chequeos en cascada.
4. Se inserta registro en `trigger_control`.
5. Se borra el fichero de HDFS.
6. Analista puede ver logs desde Superset (o consulta directa a BD).

*(Ver `docs/diagrama_casos_de_uso.png` para la imagen completa.)*

### 3.2 Diagrama entidad-relación

**Tabla principal**: `trigger_control`

* `id (serial PK)`
* `logged_at (timestamp)`
* `file_config_id (int FK a file_configuration.id)`
* `file_name (varchar)`
* `field_name (varchar, null)`
* `environment (varchar)`
* `validation_flag (varchar)`
* `error_message (varchar, null)`

**Relaciones**:

* `file_configuration` almacena metadatos de cada fichero (formato, ruta HDFS, delimitador, etc.).
* `semantic_layer` define metadatos de cada campo (nombre, tipo, longitud, PK, nullable, formato).

*(Ver `docs/ERD.png` para la imagen completa.)*

### 3.3 Esquema para BD no relacional

*(Propuesta futura)*

```json
{
  "_id": ObjectId,
  "logged_at": ISODate,
  "file_config_id": Int,
  "file_name": String,
  "validation_flag": String,
  "error_message": String,
  "details": { ... }
}
```

### 3.4 Diagrama de clases del modelo

Paquetes y clases más relevantes:

```
config/
  ├─ DbConfig.scala
  ├─ DBConnection.scala
  └─ SparkSessionProvider.scala

models/
  ├─ FileConfigurationCaseClass.scala   ← case class que mapea file_configuration
  └─ SemanticLayerCaseClass.scala       ← case class que mapea semantic_layer

services/
  ├─ ExecutionManager.scala
  └─ TriggerIdManager.scala

utils/
  ├─ Reader.scala
  ├─ Writer.scala
  └─ FileManager.scala

validators/
  ├─ FileSentinel.scala
  ├─ TypeValidator.scala
  ├─ ReferentialIntegrityValidator.scala
  └─ FunctionalValidator.scala

src/main/scala/Main.scala
```

**Ejemplo `FileConfigurationCaseClass`**:

```scala
package models

case class FileConfigurationCaseClass(
  id: Int,
  file_format: String,
  path: String,
  file_name: String,
  has_header: Boolean,
  delimiter: String,
  quote_char: String,
  escape_char: String,
  date_format: String,
  timestamp_format: String,
  partition_columns: Option[String]
)
```

*(Ver `docs/diagrama_clases.png` para la imagen completa.)*

### 3.5 Diagramas de secuencia

1. **Detección y validación**:

   ```
   Main → HDFS.listStatus()
        → ExecutionManager.executeFile(path)
           → Reader.readFile(...)
           → FileSentinel.verifyFiles(...)
             → logTrigger(...) (flag 32/33/34 o continua)
           → TypeValidator.verifyTyping(...)
             → logTrigger(...) (flag 35/36/37/38 o continua)
           → ReferentialIntegrityValidator.verifyIntegrity(...)
             → logTrigger(...) (flag 39 o continua)
           → FunctionalValidator.verifyFunctional(...)
             → logTrigger(...) (flag 40–49 o “2” OK)
           → escribir a PostgreSQL
           → HDFS.delete(path)
   ```

2. **Registro en BD**:

   ```
   logTrigger → SparkDataFrame → df.write.mode("append").jdbc(...)
   ```

*(Ver `docs/diagrama_secuencia.png` para la imagen completa.)*

---

## 4. Implementación (GIT & Docker)

### 4.1 Estructura del repositorio

```plaintext
Fin_de_Grado/
├── docker/
│   ├── Dockerfile.engine
│   ├── Dockerfile.superset
│   └── docker-compose.yml
├── docs/                     ← diagramas y esquemas en PNG
├── scripts/
│   └── rebuild_and_run.sh    ← script para compilar y ejecutar el motor
├── src/
│   ├── main/
│   │   ├── resources/
│   │   │   ├── application.conf
│   │   │   ├── db.properties
│   │   │   └── files/         ← scripts Python para generar CSV de prueba
│   │   └── scala/             ← código fuente (validadores, ExecutionManager, etc.)
│   └── test/scala/            ← pruebas unitarias (ScalaTest)
├── build.sbt                  ← definiciones SBT
└── README-cluster.md          ← guía del clúster Hadoop/Spark/Kafka/Superset
```

* `docker/`:

   * **Dockerfile.engine**: Multi-stage build para el motor de validaciones Scala+Spark.
   * **Dockerfile.superset**: Dockerfile específico para Superset.
   * **docker-compose.yml**: Orquestador de todos los servicios (HDFS, Spark, PostgreSQL, Superset, Kafka).

* `docs/`: diagramas ER, clases, casos de uso, etc.

* `scripts/rebuild_and_run.sh`: simplifica el flujo “compile → build docker → levantar solo validation-engine”.

### 4.2 Multi-Stage Dockerfile (`docker/Dockerfile.engine`)

```dockerfile
# Stage 1: Build con OpenJDK y SBT
FROM openjdk:11-slim AS builder
WORKDIR /app

# Instalar SBT
RUN apt-get update && apt-get install -y curl gnupg && \
    echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" > /etc/apt/sources.list.d/sbt.list && \
    curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x99E82A75642AC823" | apt-key add - && \
    apt-get update && apt-get install -y sbt && rm -rf /var/lib/apt/lists/*

# Copiar proyecto para compilar
COPY ../project   project/
COPY ../build.sbt build.sbt
COPY ../src       src/
COPY ../db.properties db.properties
COPY ../src/main/resources/application.conf src/main/resources/

# Ensamblar fat JAR
RUN sbt clean assembly

# Stage 2: Runtime Spark
FROM bitnami/spark:3.3.1
WORKDIR /app

# Copiar JAR compilado y configuración
COPY --from=builder /app/target/scala-2.12/Fin_de_Grado-assembly-0.1.0-SNAPSHOT.jar app.jar
COPY ../db.properties    db.properties
COPY ../src/main/resources/application.conf application.conf

# Variables de entorno por defecto (sobre-escribibles en Compose)
ENV INPUT_DIR=/data/bank_accounts \
    OUTPUT_TABLE=trigger_control \
    POLL_INTERVAL_MS=10000

# Puntos de montaje para datos temporales de Spark
# (evita quedarnos sin espacio, montado en tmpfs)
ENV SPARK_LOCAL_DIRS=/tmp/spark_local

# Arranque del motor con Spark
ENTRYPOINT spark-submit \
  --class Main \
  --master spark://spark-master:7077 \
  --deploy-mode client \
  --conf spark.driver.host=validation-engine \
  --conf spark.hadoop.fs.defaultFS=hdfs://hadoop-namenode:9000 \
  /app/app.jar
```

* **Stage 1**: instala SBT y compila el JAR “fat” con todas las dependencias.
* **Stage 2**: parte de la imagen oficial de Spark; copia el JAR, `db.properties` y `application.conf`.
* Expone variables de entorno (pueden sobreescribirse desde `docker-compose.yml`).
* Monta `SPARK_LOCAL_DIRS` en `/tmp/spark_local` para evitar llenar el espacio de contenedor.

### 4.3 Docker Compose (`docker/docker-compose.yml`)

```yaml
version: '3.8'

services:
  superset:
    build:
      context: .
      dockerfile: Dockerfile.superset
    container_name: superset
    ports:
      - "8088:8088"
    environment:
      - SUPERSET_CONFIG_PATH=/app/superset_config.py
    volumes:
      - ./superset_config.py:/app/superset_config.py
    depends_on:
      - superset-db
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8088/health"]
      interval: 30s
      timeout: 10s
      retries: 5
    command: >
      /bin/sh -c "
        superset db upgrade &&
        superset fab create-admin --username admin --firstname Superset --lastname Admin --email admin@superset.com --password 1234 &&
        superset init &&
        superset run -h 0.0.0.0 -p 8088
      "
    networks:
      - superset-net

  superset-db:
    image: postgres:latest
    container_name: superset-db
    environment:
      POSTGRES_DB: superset
      POSTGRES_USER: superset
      POSTGRES_PASSWORD: superset
    volumes:
      - superset-db-data:/var/lib/postgresql/data
    ports:
      - "5432:5432"
    networks:
      - superset-net

  zookeeper:
    image: wurstmeister/zookeeper
    container_name: zookeeper
    ports:
      - "2181:2181"
    networks:
      - superset-net

  kafka:
    image: wurstmeister/kafka
    container_name: kafka
    ports:
      - "9092:9092"
    environment:
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
      KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092
      KAFKA_BROKER_ID: 1
    depends_on:
      - zookeeper
    networks:
      - superset-net

  hadoop-namenode:
    image: bde2020/hadoop-namenode:latest
    container_name: hadoop-namenode
    ports:
      - "9000:9000"
      - "9870:9870"
    environment:
      CLUSTER_NAME: test
      CORE_CONF_fs_defaultFS: hdfs://hadoop-namenode:9000
    volumes:
      - hadoop-namenode:/hadoop/dfs/name
    networks:
      - superset-net

  hadoop-datanode:
    image: bde2020/hadoop-datanode:latest
    container_name: hadoop-datanode
    ports:
      - "9864:9864"
    environment:
      CLUSTER_NAME: test
      CORE_CONF_fs_defaultFS: hdfs://hadoop-namenode:9000
    depends_on:
      - hadoop-namenode
    volumes:
      - hdfs-data-datanode:/hadoop/dfs/data
    networks:
      - superset-net

  spark-master:
    image: bitnami/spark:3.3.1
    container_name: spark-master
    extra_hosts:
      - "host.docker.internal:host-gateway"
    environment:
      SPARK_MODE: master
      SPARK_RPC_AUTHENTICATION_ENABLED: "no"
      SPARK_RPC_ENCRYPTION_ENABLED: "no"
      SPARK_LOCAL_STORAGE_ENCRYPTION_ENABLED: "no"
      SPARK_SSL_ENABLED: "no"
      SPARK_MASTER_HOST: spark-master
      SPARK_MASTER_PORT: "7077"
      SPARK_MASTER_WEBUI_PORT: "8080"
    ports:
      - "7077:7077"
      - "8080:8080"
    networks:
      - superset-net
    volumes:
      - ./target/scala-2.12:/app/target/scala-2.12

  spark-worker-1:
    image: bitnami/spark:3.3.1
    container_name: spark-worker-1
    depends_on:
      - spark-master
    extra_hosts:
      - "host.docker.internal:host-gateway"
    environment:
      SPARK_MODE: worker
      SPARK_MASTER_URL: spark://spark-master:7077
      SPARK_WORKER_MEMORY: 12G
      SPARK_WORKER_CORES: "4"
      SPARK_WORKER_PORT: "7078"
      SPARK_WORKER_WEBUI_PORT: "8081"
      SPARK_LOCAL_DIRS: /tmp/spark_local
    ports:
      - "8081:8081"
    networks:
      - superset-net
    volumes:
      - ./target/scala-2.12:/app/target/scala-2.12

  spark-worker-2:
    image: bitnami/spark:3.3.1
    container_name: spark-worker-2
    depends_on:
      - spark-master
    extra_hosts:
      - "host.docker.internal:host-gateway"
    environment:
      SPARK_MODE: worker
      SPARK_MASTER_URL: spark://spark-master:7077
      SPARK_WORKER_MEMORY: 12G
      SPARK_WORKER_CORES: "4"
      SPARK_WORKER_PORT: "7079"
      SPARK_WORKER_WEBUI_PORT: "8082"
      SPARK_LOCAL_DIRS: /tmp/spark_local
    ports:
      - "8082:8082"
    networks:
      - superset-net
    volumes:
      - ./target/scala-2.12:/app/target/scala-2.12

  validation-engine:
    build:
      context: ..
      dockerfile: docker/Dockerfile.engine
    container_name: validation-engine
    depends_on:
      - spark-master
      - hadoop-namenode
      - superset-db
    environment:
      INPUT_DIR: "/data/bank_accounts"
      OUTPUT_TABLE: "trigger_control"
      POLL_INTERVAL_MS: "10000"
    extra_hosts:
      - "validation-engine:host-gateway"
    networks:
      - superset-net
    volumes:
      - ../src/main/resources/files/bank_accounts:/local_bank_accounts
    command: >
      bash -c "
        # 1) Salir de safe mode antes de crear carpeta
        hdfs dfsadmin -safemode leave 2>/dev/null || true &&
        # 2) Crear ruta en HDFS y subir CSV
        hdfs dfs -mkdir -p /data/bank_accounts &&
        hdfs dfs -put -f /local_bank_accounts/*.csv /data/bank_accounts &&
        hdfs dfs -chmod -R 777 /data/bank_accounts &&
        # 3) Ejecutar motor de validaciones
        spark-submit \
          --class Main \
          --master spark://spark-master:7077 \
          --deploy-mode client \
          --conf spark.driver.host=validation-engine \
          --conf spark.hadoop.fs.defaultFS=hdfs://hadoop-namenode:9000 \
          /app/app.jar
      "

volumes:
  superset-db-data:
  hadoop-namenode:
  hdfs-data-datanode:
  docker_validation_tmp:

networks:
  superset-net:
    driver: bridge
```

* Se crea la red compartida `superset-net`.
* Volúmenes:

   * `superset-db-data`: datos de PostgreSQL (Superset).
   * `hadoop-namenode`: metadatos del NameNode.
   * `hdfs-data-datanode`: datos de DataNode.
   * `docker_validation_tmp`: se puede usar para almacenar datos temporales de Spark si se monta en `/tmp`.
* Al arrancar `validation-engine`, se sale de safe mode (`hdfs dfsadmin -safemode leave`), se crea `/data/bank_accounts`, se suben los CSV, se ajustan permisos y se lanza Spark Submit.

### 4.4 Script de reconstrucción (`scripts/rebuild_and_run.sh`)

```bash
#!/usr/bin/env bash
set -euo pipefail

# Construir el JAR con SBT
sbt clean assembly

# Crear red si no existe
docker network create superset-net || true

# Reconstruir y ejecutar solo el contenedor de validation-engine
cd docker
docker-compose build validation-engine
docker-compose up --abort-on-container-exit validation-engine
```

* Este script compila el JAR y levanta únicamente el servicio `validation-engine` (asume que el resto de contenedores ya están levantados).
* Si el cluster Hadoop/Spark/DB está parado, ejecutar `docker-compose up -d` antes.

---

## 5. Resultado (Manual de usuario)

### 5.1 Requisitos previos

* **Docker Desktop** (Linux, macOS o Windows) instalado y con **160 GB** libres en disco.
* **Docker Compose** disponible (versión ≥ 1.27).
* Clúster Docker corriendo con:

   * **HDFS** (NameNode + DataNode)
   * **Spark Master** + **Spark Workers**
   * **PostgreSQL** para Superset y para la tabla `trigger_control`
   * **Superset** (opcional)
   * **Zookeeper** + **Kafka** (opcional)
* Archivo `db.properties` configurado con credenciales de la BD.
* Variables de entorno en `docker/docker-compose.yml` o en un `.env` si se prefiere.

### 5.2 Pasos de uso

1. **Clonar el repositorio**

   ```bash
   git clone https://github.com/usuario/validation-engine.git
   cd validation-engine
   ```

2. **Configurar credenciales**

   ```bash
   cp src/main/resources/db.properties.example src/main/resources/db.properties
   # Editar usuario, contraseña y URL JDBC a PostgreSQL
   ```

3. **Levantar infraestructura completa**
   En la carpeta `docker/`:

   ```bash
   cd docker
   docker-compose up -d
   ```

   * Esto crea la red `superset-net` y levanta:
     `superset-db`, `superset`, `zookeeper`, `kafka`,
     `hadoop-namenode`, `hadoop-datanode`,
     `spark-master`, `spark-worker-1`, `spark-worker-2`.
   * Esperar a que todos los contenedores estén `healthy`.
   * Si HDFS arranca en safe mode, el servicio `validation-engine` lo dejará fuera de safe mode automáticamente al iniciarse.

4. **Reconstruir y ejecutar el motor de validaciones**
   Desde la raíz del proyecto:

   ```bash
   sbt clean assembly
   cd docker
   docker-compose build validation-engine
   docker-compose up validation-engine
   ```

   * El contenedor `validation-engine` ejecuta:

      1. `hdfs dfsadmin -safemode leave` (para salir de safe mode).
      2. `hdfs dfs -mkdir -p /data/bank_accounts`.
      3. `hdfs dfs -put -f /local_bank_accounts/*.csv /data/bank_accounts`.
      4. `spark-submit` al JAR con la clase `Main`.

5. **Consultar resultados**
   Entra en la BD PostgreSQL (por ejemplo, con `psql` o Superset) y ejecuta:

   ```sql
   SELECT * 
   FROM trigger_control 
   ORDER BY logged_at DESC 
   LIMIT 20;
   ```

   * Cada fila muestra la última validación:

      * `file_config_id`: referencia a `file_configuration`.
      * `file_name`: nombre del fichero.
      * `field_name`: campo donde falló (o NULL si no aplica).
      * `validation_flag`: código (2=OK; 30–49=fallos).
      * `error_message`: detalle del error.

6. **(Opcional) Limpiar todo y volver a cero**
   Si quieres borrar contenedores, imágenes y volúmenes no usados:

   ```bash
   # Parrilla de comandos de limpieza
   docker-compose down -v                       # Detiene y elimina contenedores + volúmenes de este Compose
   docker system prune -a --volumes -f          # Elimina imágenes, contenedores, redes y volúmenes no asociados
   ```

   Luego vuelve a levantar con `docker-compose up -d`.

### 5.3 Configuración de Superset (breve)

Superset se conecta a PostgreSQL para almacenar metadata y dashboards. Si quieres que Superset también apunte a la BD de validaciones:

* La **SQLAlchemy URI** típica para tu motor de validaciones (si se expone desde Superset) sería:

  ```
  postgresql://superset:XXXXXXXXXX@superset-db:5432/superset
  ```


## 6. Conclusiones

### 6.1 Dificultades

* **ClusterID incompatibles (NameNode vs DataNode)**

   * Al cambiar o recrear imágenes de Hadoop, puede surgir un error de “Incompatible clusterIDs” en el DataNode.
   * Solución: borrar o recrear los volúmenes de HDFS (`hadoop-namenode`, `hdfs-data-datanode`) antes de volver a levantar.

* **Safe Mode en HDFS**

   * HDFS arranca en “safe mode” si detecta problemas de espacio o metadatos.
   * Ahora se ejecuta `hdfs dfsadmin -safemode leave` en el contenedor del motor para forzar la salida de safe mode.

* **Espacio en disco de Docker**

   * Los volúmenes de Spark y Hadoop tienden a crecer.
   * Se recomienda montar `/tmp/spark_local` en un volumen dedicado (`docker_validation_tmp`) o usar `tmpfs` en Docker Desktop para `/tmp`.
   * Limpiar periódicamente con `docker system prune -a --volumes`.

* **Configuración de permisos HDFS**

   * Cada vez que se suben ficheros, se aplica `hdfs dfs -chmod -R 777 /data/bank_accounts` para asegurar lectura/escritura a todos los usuarios.

* **Parámetros Spark**

   * En contenedores Spark Workers, se configuran `SPARK_LOCAL_DIRS=/tmp/spark_local` y se monta como volumen para no colapsar el disco.
   * Ajustar `spark.executor.memory` y `spark.driver.memory` según disponibilidad de recursos.

### 6.2 Mejoras futuras

* **Migrar a Structured Streaming**

   * Pasar de polling batch a **Spark Structured Streaming** para procesamiento near‐real‐time.

* **Orquestación con Kubernetes**

   * Desplegar Hadoop + Spark + validación en un clúster Kubernetes (Helm charts, StatefulSets).

* **Monitorización con Prometheus & Grafana**

   * Exponer métricas de Spark, HDFS y PostgreSQL para visualizar latencias, contadores de errores, etc.

* **Ampliar validadores**

   * Soporte para formatos **Avro**, **Parquet** o **ORC**.
   * Validaciones basadas en **schemas Avro** o **JSON Schema**.

---

## Anexo: Glosario de Flags

|          Rango         | Significado                                      |
| :--------------------: | :----------------------------------------------- |
|           30           | Error de lectura (I/O, CSV mal formado)          |
|           32           | Delimiter mismatch (número de columnas esperado) |
|           33           | Header mismatch (encabezados distintos)          |
|           34           | Column count per row mismatch                    |
|           35           | Tipo inválido                                    |
|           36           | Nulo indebido                                    |
|           37           | Longitud excedida                                |
|           38           | Formato texto inválido                           |
|           39           | Duplicado PK (referencial)                       |
|           40           | Formato inválido (`account_number`)              |
|           41           | Fuera de rango (`credit_score`)                  |
|           42           | Fuera de rango (`risk_score`)                    |
|           43           | Menor de edad (`date_of_birth`)                  |
|           44           | Negativo en “Active” (`balance`)                 |
|           45           | Balance ≠ 0 en “Closed”                          |
|           46           | Interest ≠ 0 en “Checking”                       |
|           47           | Overdraft inválido                               |
|           48           | Pocas transacciones en joint                     |
|           49           | Avg tx ≠ 0 con 0 tx                              |
| 1.13, 1.21, 1.31, 1.41 | Flags de validación “todo OK” (sin errores)      |
|            2           | OK final (todos los validadores pasaron)         |
|           99           | Sin configuración en `file_configuration`        |

---