# Motor de Validaciones con Scala y Apache Spark

Este proyecto implementa un motor de validaciones distribuido para procesar ficheros bancarios en HDFS, utilizando **Scala**, **Apache Spark** y **PostgreSQL** dentro de un entorno **Docker Compose**.

## Índice

1. [Introducción](#introducción)
2. [Especificación de Requisitos](#especificación-de-requisitos)
3. [Diseño (Diagramas)](#diseño-diagramas)
4. [Implementación (Git & Docker)](#implementación-git--docker)
5. [Resultado (Manual de usuario)](#resultado-manual-de-usuario)
6. [Conclusiones](#conclusiones)

---

## 1. Introducción

### 1.1 Resumen del proyecto

Este proyecto de Fin de Grado nace de la necesidad de garantizar la **calidad** y la **integridad** de los datos bancarios que llegan periódicamente en forma de ficheros al clúster HDFS. Mediante un motor de validaciones implementado en **Scala** y sobre la plataforma **Apache Spark**, se procesan grandes volúmenes de datos de manera distribuida, aplicando una serie de comprobaciones en varias capas:

* **Estructural**: formato y número de columnas.
* **Tipológica**: tipos de datos y rangos válidos.
* **Referencial**: unicidad y coherencia con metadatos.
* **Funcional**: reglas de negocio específicas del dominio bancario.

Cada validación se registra con detalle en una tabla de logs en **PostgreSQL**, permitiendo auditoría, estadísticas de calidad y alertas automatizadas.

### 1.2 Explicación de la Aplicación

La aplicación se compone de los siguientes módulos:

1. **Main.scala**: corazón del sistema, implementa un **bucle de polling** que observa un directorio en HDFS. Al detectar nuevos ficheros, ejecuta el motor de validaciones y, tras finalizar, borra o archiva el fichero.
2. **DbConfig & DBConnection**: encapsulan la lectura de credenciales desde `db.properties` y gestionan la creación de conexiones JDBC, con tratamiento de excepciones y retry.
3. **SparkSessionProvider**: configura la SparkSession con parámetros de master, serialización (Kryo), particionamiento y acceso a HDFS, de forma centralizada.
4. **ExecutionManager**: orquesta las fases de validación. Puede procesar directorios completos (`executeEngine`) o ficheros sueltos (`executeFile`), reutilizando la misma lógica con ligeras variaciones.
5. **Validators**:

    * **FileSentinel**: verifica delimitadores, encabezados y consistencia de conteo de columnas.
    * **TypeValidator**: comprueba tipos de datos (números, fechas, timestamps), nullabilidad, longitud de texto y formato.
    * **ReferentialIntegrityValidator**: asegura unicidad de claves primarias y consistencia con la capa semántica.
    * **FunctionalValidator**: aplica reglas específicas (formato de cuenta, rangos de credit\_score, cálculos de edad mínima, estados de cuenta y límites de sobregiro).
6. **Utils**:

    * **Reader**: carga tablas JDBC con particionamiento y ficheros de distintos formatos (CSV, JSON, Parquet).
    * **Writer**: mecanismos de escritura a JDBC (append/upsert) y exportación a CSV.
    * **FileManager**: construye rutas y filtra ficheros pendientes basándose en triggers.
7. **TriggerIdManager**: gestiona la generación de identificadores de trigger únicos basados en el máximo histórico en BD, evitando colisiones en entornos distribuidos.

Esta arquitectura modular permite extender o sustituir validadores, cambiar backend de almacenamiento o integrar nuevos flujos sin modificar el núcleo del motor.

### 1.3 Resumen de tecnologías utilizadas

* **Scala 2.12**: lenguaje funcional y orientado a objetos para Spark.
* **Apache Spark 3.x**: procesamiento distribuido in-memory.
* **HDFS (Hadoop 3.x)**: almacenamiento distribuido de ficheros.
* **PostgreSQL 13**: base de datos relacional para logs.
* **Kryo**: serializador ligero, con registrator personalizado para `TimestampType` y `ByteBuffer`.
* **SBT**: gestor de proyectos, compilación y ensamblado.
* **Docker & Docker-Compose**: contenedorización de Spark, Hadoop, PostgreSQL.
* **TypeSafe Config**: gestión de configuración mediante `application.conf`.
* **ScalaTest**: framework para pruebas unitarias.

---

## 2. Especificación de Requisitos

### 2.1 Requisitos funcionales

1. **Monitorización de HDFS**: detectar automáticamente nuevos ficheros en un directorio configurado.
2. **Procesamiento por fichero**: cada archivo debe ser procesado de forma independiente.
3. **Validación estructural**: verificar número exacto y orden de columnas conforme a `file_configuration`.
4. **Validación tipológica**: asegurar tipos, rangos, formato de fecha y timestamp.
5. **Integridad referencial**: comprobar unicidad de claves primarias y consistencia con la capa semántica (`semantic_layer`).
6. **Validación de negocio**: aplicar reglas como formato de número de cuenta, rangos de credit\_score y balance según estado.
7. **Registro de resultados**: insertar logs detallados en `trigger_control` con campos:

    * `logged_at` (timestamp)
    * `file_config_id`, `file_name`, `field_name`, `environment`, `validation_flag`, `error_message`
8. **Limpieza de ficheros**: una vez procesados, los ficheros deben eliminarse o moverse para evitar reprocesamiento.
9. **Reintentos controlados**: en caso de fallo temporal (BD o HDFS), reintentar según política configurable.

### 2.2 Requisitos no funcionales

* **Rendimiento**: procesar >1M filas/partición en <2 min, uso óptimo de particiones Spark.
* **Escalabilidad**: aumentar nodos executor sin cambios en el código.
* **Disponibilidad**: capaz de funcionar 24/7 con reinicio automático.
* **Configurabilidad**: parámetros (rutas, credenciales, intervalos de polling) configurables sin recompilar.
* **Tolerancia a Fallos**: capturar y loguear excepciones, no detener el bucle principal.
* **Seguridad**: gestión de permisos HDFS y acceso restringido a BD.
* **Mantenibilidad**: código documentado con ScalaDoc, tests unitarios y estructura modular.

---

## 3. Diseño (Diagramas)

> A continuación se describen los diagramas que acompañarán esta sección; se incluirán en la versión PDF final.

### 3.1 Casos de uso

Se identifican tres actores principales:

* **Operador**: deposita ficheros en HDFS.
* **Sistema de validación**: detecta y procesa.
* **Analista**: consulta resultados en BD.

Casos de uso:

1. **Subir fichero**: Operador → HDFS.
2. **Procesar fichero**: Sistema → HDFS & Spark → BD.
3. **Consultar logs**: Analista → BD.

### 3.2 Diagrama Entidad-Relación

Entidad principal **trigger\_control** con relaciones a **file\_configuration** y **semantic\_layer**. Campos clave y cardinalidades.

### 3.3 Esquema para BD no relacional

Propuesta de colección **logs** en MongoDB:

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

### 3.4 Diagrama de Clases del modelo

Clases y objetos en paquetes:

* **config**: `DbConfig`, `DBConnection`, `SparkSessionProvider`
* **models**: `FileConfigurationCaseClass`, `SemanticLayerCaseClass`
* **services**: `ExecutionManager`, `TriggerIdManager`
* **utils**: `Reader`, `Writer`, `FileManager`
* **validators**: cuatro validadores.

### 3.5 Diagramas de Secuencia

1. **Detección y validación**:

    * Main inicia bucle → HDFS.listStatus → ejecución `executeFile`.
2. **Registro en BD**:

    * ExecutionManager → logTrigger → JDBC `INSERT`.

---

## 4. Implementación (Git & Docker)

### 4.1 Estructura del repositorio

```plaintext
Fin_de_Grado/
├── docker/
│   ├── Dockerfile.engine
│   └── docker-compose.yml
├── src/
│   ├── main/scala/
│   ├── main/resources/
│   └── test/scala/
├── build.sbt
├── db.properties
└── scripts/
    └── rebuild_and_run.sh
```

### 4.2 Multi-Stage Dockerfile (`docker/Dockerfile.engine`)

```dockerfile
# Stage 1: Compilar con openjdk y sbt
FROM openjdk:11-slim AS builder
WORKDIR /app

# Instalar sbt
RUN apt-get update && apt-get install -y curl gnupg && \
    echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" > /etc/apt/sources.list.d/sbt.list && \
    curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x99E82A75642AC823" | apt-key add - && \
    apt-get update && apt-get install -y sbt && rm -rf /var/lib/apt/lists/*

# Copiar proyecto
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

# JAR y configuración
COPY --from=builder /app/target/scala-2.12/Fin_de_Grado-assembly-0.1.0-SNAPSHOT.jar app.jar
COPY ../db.properties    db.properties
COPY ../src/main/resources/application.conf application.conf

# Variables de entorno
ENV INPUT_DIR=/data/bank_accounts \
    OUTPUT_TABLE=trigger_control \
    POLL_INTERVAL_MS=10000

# Arranque
ENTRYPOINT spark-submit \
  --class Main \
  --master spark://spark-master:7077 \
  --deploy-mode client \
  --conf spark.driver.host=validation-engine \
  --conf spark.hadoop.fs.defaultFS=hdfs://hadoop-namenode:9000 \
  /app/app.jar
```

### 4.3 Docker Compose (`docker/docker-compose.yml`)

Servicios:

* `hadoop-namenode`
* `hadoop-datanode`
* `spark-master`
* `spark-worker-1`, `spark-worker-2`
* `superset-db`, `superset`
* `zookeeper`, `kafka`
* `validation-engine`

Red compartida: `superset-net` (external o creada previamente).

### 4.4 Script de reconstrucción (`scripts/rebuild_and_run.sh`)

```bash
#!/usr/bin/env bash
set -euo pipefail
# Compilar JAR
sbt clean assembly
# Crear red si no existe
docker network create superset-net || true
# Reconstruir y ejecutar motor
cd docker
docker-compose build validation-engine
docker-compose up --abort-on-container-exit validation-engine
```

---

## 5. Resultado (Manual de usuario)

### 5.1 Requisitos previos

* Docker y Docker Compose instalados.
* Clúster Docker corriendo: Hadoop, Spark, PostgreSQL, Kafka, Superset.
* Fichero `db.properties` con credenciales.

### 5.2 Pasos de uso

1. **Clonar repositorio**:

   ```bash
   ```

git clone [https://github.com/usuario/validation-engine.git](https://github.com/usuario/validation-engine.git)
cd validation-engine

````
2. **Configurar credenciales**:
```bash
cp db.properties.example db.properties
# Editar valores en db.properties
````

3. **Levantar infraestructura**:

   ```bash
   ```

cd docker
docker-compose up -d&#x20;
zookeeper kafka superset-db superset&#x20;
hadoop-namenode hadoop-datanode&#x20;
spark-master spark-worker-1 spark-worker-2

````
4. **Reconstruir y ejecutar motor**:
```bash
cd ../scripts
./rebuild_and_run.sh
````

5. **Consultar resultados**:

   ```sql
   ```

SELECT \* FROM trigger\_control ORDER BY logged\_at DESC LIMIT 20;

```

---

## 6. Conclusiones

### 6.1 Dificultades

* Configurar conexión Docker→Spark Master (`spark.driver.host`).
* Evitar errores de MetricsSystem desactivando Spark UI.
* Permisos HDFS para eliminar ficheros.

### 6.2 Mejoras futuras

* Migrar a **Structured Streaming**.
* Añadir **Prometheus/Grafana** para métricas.
* CI/CD completo con GitHub Actions.

---

*Fin de la documentación extensa.*

```
