
## 5. Resultado (Manual de usuario)

### 5.1 Requisitos previos

Requisitos comunes:

* **Docker Desktop** (Linux, macOS o Windows) con **160 GB** libres en disco.
* **Docker Compose** (versión ≥ 1.27).
* **Node.js v16+** y **npm** (para frontend).
* **Python 3.12**, **pip** (para backend).
* Clonar el repositorio:

  ```bash
  git clone https://github.com/JuanraDAM/FileMonitoringSystem.git
  cd FileMonitoringSystem
  ```

#### 5.1.1 Motor de validaciones

* Espacio en disco para contenedores Hadoop/Spark y datos HDFS.
* Puertos:

    * **9000** (NameNode)
    * **9870** (NameNode UI)
    * **9864** (DataNode)
    * **7077** (Spark Master)
    * **8080** (Spark Master UI)
    * **8081, 8082** (Spark Workers UI)
* **SBT** instalado localmente si se desea compilar sin Docker.

#### 5.1.2 Backend (FastAPI)

* Crear entorno virtual Python 3.12:

  ```bash
  python3.12 -m venv backend/.venv
  source backend/.venv/bin/activate
  pip install --upgrade pip
  pip install -r backend/requirements.txt 
  ```
* Archivo `.env` en `backend/` con credenciales y configuración de HDFS.

#### 5.1.3 Frontend (React)

* Desde `frontend/`:

  ```bash
  cd frontend
  npm install
  ```
* No hay variables de entorno adicionales, asume que el backend corre en `http://localhost:8000`.

---

### 5.2 Pasos de uso

A continuación se describen los pasos para levantar cada componente y usar el sistema completo.

#### 5.2.1 Levantar infraestructura con Docker Compose

1. **Ir a la carpeta del motor**:

   ```bash
   cd engine/docker
   ```

2. **Crear la red Docker** (si no existe):

   ```bash
   docker network create superset-net || true
   ```

3. **Levantar todos los servicios**:

   ```bash
   docker-compose up -d
   ```

   Esto arrancará:

    * PostgreSQL (para Superset y tablas de validación).
    * Superset (opcional, UI de dashboards).
    * Zookeeper + Kafka (opcional).
    * Hadoop NameNode + DataNode.
    * Spark Master + Spark Workers.
    * Contenedor `validation-engine` (motor de validaciones).

4. **Verificar estado**:

   ```bash
   docker-compose ps
   ```

    * Esperar a que contenedores se vuelvan `healthy`.
    * Si HDFS arranca en safe mode, `validation-engine` ejecutará automáticamente `hdfs dfsadmin -safemode leave`.

#### 5.2.2 Inicializar el backend (FastAPI)

1. Entrar a la carpeta `backend/`:

   ```bash
   cd ../../backend
   source .venv/bin/activate
   ```
2. Ejecutar migraciones o crear tablas manualmente (no incluidas en el proyecto, usar tu propia estrategia).
3. Levantar el servidor local:

   ```bash
   uvicorn main:app --reload --host 0.0.0.0 --port 8000
   ```

    * La API estará disponible en `http://localhost:8000`.
    * Endpoints principales:

        * `/auth/register`
        * `/auth/login`
        * `/files/upload`
        * `/files/push/{file_name}`
        * `/files/download/{file_name}`
        * `/files/` (GET, POST, PATCH, DELETE)
        * `/files/logs`

#### 5.2.3 Inicializar el frontend (React)

1. En otra terminal, entrar a `frontend/`:

   ```bash
   cd ../frontend
   ```
2. Instalar dependencias (si no se hizo antes):

   ```bash
   npm install
   ```
3. Levantar el servidor de desarrollo:

   ```bash
   npm run dev
   ```
4. Abrir el navegador en `http://localhost:5173`.

#### 5.2.4 Flujo de usuario

1. **Registro/Login**

    * Acceder a `http://localhost:5173/register` para crear cuenta.
    * Luego `http://localhost:5173/login` para iniciar sesión.

2. **Dashboard**

    * Subir CSV: seleccionar un archivo y pulsar **Subir Fichero**.
    * Aparecerá alerta con el ID de configuración.
    * La tabla mostrará todas las configuraciones; cada fila ofrece botones:

        * **Validar**: envía el CSV a HDFS y lanza validación en Spark.
        * **Detalles**: abre modal para editar metadatos, validar, descargar o eliminar.
        * **Eliminar**: borra configuración de la base de datos.

3. **Visualizar logs**

    * Acceder a `http://localhost:5173/logs` para ver resultados de validación.
    * La tabla mostrará los últimos logs, con fecha formateada a `DD/MM/YYYY`.

4. **Superset (opcional)**

    * Superset corre en `http://localhost:8088`.
    * Iniciar sesión con credenciales creadas en `docker-compose.yml` (admin / 1234).
    * Configurar una conexión a PostgreSQL (`superset-db:5432`) para leer tablas `trigger_control` y crear dashboards de calidad de datos.

---

### 5.3 Configuración adicional

1. **Variables de entorno (backend)**

    * En `backend/.env`:

      ```
      # PostgreSQL
      POSTGRES_USER=superset
      POSTGRES_PASSWORD=superset
      POSTGRES_HOST=postgres
      POSTGRES_PORT=5432
      POSTGRES_DB=superset
 
      # HDFS
      HDFS_HOST=hadoop-namenode
      HDFS_PORT=9870
      HDFS_DIR=/data/bank_accounts
      HDFS_USER=hdfs
      HDFS_DATANODE_HOST=hadoop-datanode
      HDFS_DATANODE_PORT=9864
 
      # Carpeta local de uploads
      UPLOAD_DIR=uploaded_files
 
      # JWT
      JWT_SECRET_KEY=TuSecretoUltraSeguro123!
      JWT_ALGORITHM=HS256
      ACCESS_TOKEN_EXPIRE_MINUTES=60
      ```

2. **Permisos en HDFS**

    * El contenedor `validation-engine` se encarga de ajustar permisos con `hdfs dfs -chmod -R 777 /data/bank_accounts`.
    * Si se prefiere, desde cualquier contenedor con cliente Hadoop:

      ```bash
      hdfs dfs -chmod -R 777 /data/bank_accounts
      ```

3. **Descargas y backups**

    * Para exportar la base de datos PostgreSQL:

      ```bash
      docker exec -t superset-db pg_dumpall -c -U superset > dump_$(date +%F).sql
      ```
    * Para restaurar:

      ```bash
      docker exec -i superset-db psql -U superset < dump_YYYY-MM-DD.sql
      ```

4. **Personalizar Spark**

    * Variables de entorno para Spark Master/Workers (en `docker-compose.yml`):

        * `SPARK_WORKER_MEMORY`, `SPARK_WORKER_CORES`, `SPARK_LOCAL_DIRS`.
    * Ajustar `spark.executor.memory` y `spark.driver.memory` en `spark-submit` si se procesan CSV muy grandes.

---
