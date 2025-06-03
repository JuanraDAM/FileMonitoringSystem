
## 1. Introducción

### 1.1 Resumen del proyecto

El **File Monitoring System** es una solución integral para la ingesta, validación y visualización de ficheros bancarios en un entorno Big Data. Consta de tres componentes principales:

1. Un **motor de validaciones** implementado en **Scala** y ejecutado sobre **Apache Spark**, que procesa ficheros almacenados en **HDFS**, aplica validaciones estructurales, tipológicas, referenciales y de negocio, y almacena los resultados en **PostgreSQL**.
2. Un **backend** desarrollado con **FastAPI** (Python 3.12) que expone una API REST para gestionar usuarios, configuraciones de ficheros, subir/descargar archivos y sincronizarlos con HDFS a través de **WebHDFS**.
3. Un **frontend** construido con **React 18+** y **Vite**, que permite al usuario autenticarse, subir CSV, editar metadatos de parseo, enviar ficheros a HDFS para validación y visualizar los logs resultantes.

La arquitectura está contenida en varios contenedores Docker orquestados mediante **Docker Compose**, incluyendo clúster Hadoop (NameNode/DataNode), Spark (Master/Workers), PostgreSQL, Kafka, Superset (opcional) y el propio motor de validaciones. El objetivo es ofrecer una solución escalable, modular y mantenible para garantizar la calidad e integridad de los datos bancarios.

### 1.2 Explicación de la aplicación

El flujo de trabajo global se describe a continuación:

1. **Usuario (frontend)**

    * Se registra o inicia sesión mediante JWT (FastAPI).
    * Accede a un dashboard donde puede subir un fichero CSV y gestionar metadatos de parseo (delimiter, quote\_char, date\_format, etc.).
    * Al pulsar “Validar”, el frontend envía el CSV al backend y éste lo almacena localmente y registra la configuración en la tabla `file_configuration` de PostgreSQL.

2. **Backend (FastAPI)**

    * Recibe el CSV (`POST /files/upload`) y guarda el archivo en la carpeta local `uploaded_files/`.
    * Crea o actualiza un registro en la tabla `file_configuration` con parámetros de parseo.
    * Cuando el usuario solicita “Enviar a HDFS” (`POST /files/push/{file_name}`), el backend:

        1. Verifica que el archivo existe en `uploaded_files/`.
        2. Llama a WebHDFS (NameNode) para crear el directorio `/data/bank_accounts` y ajustar permisos (`op=MKDIRS`, `op=SETPERM`).
        3. Solicita la creación del fichero en HDFS (`op=CREATE`), recuerda el redireccionamiento 307 a DataNode, ajusta la URL y sube el contenido.
        4. Responde al frontend con un mensaje de éxito.

3. **Motor de validaciones (Scala + Spark)**

    * Se ejecuta en un contenedor Docker con Spark y SBT. En el `ENTRYPOINT`, al arrancar:

        1. Ejecuta `hdfs dfsadmin -safemode leave` para salir de safe mode.
        2. Crea la carpeta `/data/bank_accounts` en HDFS (si no existe).
        3. Copia los CSV desde un volumen local (`/local_bank_accounts`) a HDFS.
        4. Inicia `spark-submit` que corre la clase `Main.scala`.
    * **Main.scala**:

        1. Configura un bucle de polling que observa el directorio HDFS (`/data/bank_accounts`).
        2. Al detectar un fichero, llama a `ExecutionManager.executeFile(path, outputTable)`.
        3. Dentro de `ExecutionManager`:

            * Lee el CSV con `Reader.readFile(...)` en un DataFrame particionado.
            * Aplica validadores en cascada:

                1. **FileSentinel**: validación estructural (delimitador, headers, número de columnas).
                2. **TypeValidator**: validación tipológica (tipos, rangos, formato de fecha, nullability).
                3. **ReferentialIntegrityValidator**: integridad referencial (unicidad de claves primarias según metadatos).
                4. **FunctionalValidator**: reglas de negocio (formato cuenta, rangos de credit\_score, balance según estado, etc.).
            * Si falla alguna validación, registra el flag correspondiente en la tabla `trigger_control` en PostgreSQL y detiene el procesamiento.
            * Si pasa todo, registra flag “2” (OK).
            * Borra el fichero de HDFS para evitar reprocesamientos.

4. **Base de datos (PostgreSQL)**

    * Contiene tablas:
        * `users`: para gestionar cuentas de usuario (backend y frontend).
        * `file_configuration`: configura parámetros de parseo de cada CSV.
        * `semantic_layer`: metadatos de cada campo (tipo de dato, longitud, nullable, PK, formato) utilizados en las validaciones tipológicas y referenciales.
        * `trigger_control`: almacena logs de validación (timestamp, file_config_id, field_name, environment, validation_flag, error_message).
        * `negative_flag_logs` (opcional): detalles adicionales de validaciones negativas.

5. **Visualización de resultados**

    * El usuario, desde el frontend, accede a `/logs` y obtiene los registros de validación (`GET /files/logs`).
    * El backend formatea la fecha (`logged_at`) al formato `DD/MM/YYYY, hh:mm:ss` en zona Madrid.
    * El frontend muestra una tabla con campos: ID, file\_config\_id, file\_name, field\_name, environment, validation\_flag, error\_message, fecha.

---

### 1.3 Resumen de tecnologías utilizadas

* **Scala 2.12**
* **Apache Spark 3.x** (Spark Streaming para procesamiento batch/near real‐time)
* **HDFS (Hadoop 3.x)**
* **SBT** (gestor de proyectos Scala)
* **Kryo** (serializador personalizado en Spark)
* **Docker & Docker Compose**

    * Contenedores: Spark Master/Workers (bitnami/spark), Hadoop NameNode/DataNode (bde2020), PostgreSQL (postgres\:latest), Kafka/Zookeeper (wurstmeister), Superset (dockerfile custom).
* **PostgreSQL 13** (logs y metadatos)
* **FastAPI** (Python 3.12)

    * **Uvicorn** (servidor ASGI)
    * **SQLAlchemy Async + AsyncPG**
    * **Pydantic Settings**
    * **python-jose**, **passlib\[bcrypt]** (JWT y hashing de contraseñas)
    * **requests** (llamadas a WebHDFS)
* **React 18+** (frontend) con **Vite**

    * **React Router v6**
    * **Axios** (con interceptores de token y 401)
    * **Context API** (AuthContext)
    * **CSS puro**
* **Superset** (opcional, para dashboards de logs)
* **Git / GitHub** (control de versiones)
* **PlantUML** (diagramas UML en bruto para generar ER, clases y secuencias)

---