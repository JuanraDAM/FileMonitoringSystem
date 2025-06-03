
## 3. Diseño (Diagramas)

> En la carpeta `docs/Img/` se incluyen todos los diagramas en PNG o PlantUML; aquí se describen y referencian los más relevantes.

### 3.1 Casos de uso

A continuación se resumen los casos de uso principales para todo el sistema:

1. **Registrar Usuario**

    * **Actor:** Usuario no autenticado.
    * **Flujo:**

        1. Accede a `/register` (frontend).
        2. Completa email y contraseña, envía `POST /auth/register` (backend).
        3. Si éxito (201), frontend redirige a `/login`; si error, muestra mensaje en formulario.

2. **Iniciar Sesión**

    * **Actor:** Usuario no autenticado.
    * **Flujo:**

        1. Accede a `/login`.
        2. Completa credenciales, envía `POST /auth/login`.
        3. Si éxito (200), backend devuelve JWT; frontend guarda en `localStorage` y redirige a `/dashboard`.
        4. Si 401, muestra mensaje “Credenciales inválidas”.

3. **Cerrar Sesión**

    * **Actor:** Usuario autenticado.
    * **Flujo:**

        1. En navbar, pulsa **Logout**.
        2. Frontend elimina token y redirige a `/login`.

4. **Subir Fichero CSV**

    * **Actor:** Usuario autenticado.
    * **Flujo:**

        1. En `/dashboard`, selecciona CSV y pulsa **Subir Fichero**.
        2. Frontend muestra “Subiendo…”, envía `POST /files/upload` con `multipart/form-data`.
        3. Backend guarda el fichero en `uploaded_files/` y crea/actualiza registro en `file_configuration`.
        4. Backend responde `{ "file_config_id": X }`; frontend muestra alerta “Fichero subido ID: X” y refresca lista.

5. **Listar Configuraciones**

    * **Actor:** Usuario autenticado.
    * **Flujo:**

        1. Al acceder a `/dashboard`, frontend hace `GET /files/`.
        2. Backend devuelve array de configuraciones; frontend muestra tabla con cada fila: ID, file\_name, path, has\_header, delimiter, etc.

6. **Editar Configuración (Modal)**

    * **Actor:** Usuario autenticado.
    * **Flujo:**

        1. En `/dashboard`, pulsa **Detalles** en una fila.
        2. Abre modal con campos prellenados (`delimiter`, `quote_char`, `escape_char`, `has_header`, `date_format`, `timestamp_format`, `partition_columns`).
        3. Usuario modifica valores y pulsa **Guardar**; frontend muestra “Guardando…”, envía `PATCH /files/{id}`.
        4. Backend actualiza `file_configuration` y responde con objeto actualizado; frontend alerta “Configuración actualizada”, cierra modal y refresca lista.

7. **Eliminar Configuración**

    * **Actor:** Usuario autenticado.
    * **Flujo:**

        1. En `/dashboard` o modal, pulsa **Eliminar**.
        2. Mostrar `window.confirm("¿Estás seguro?")`.
        3. Si confirma, frontend muestra “Eliminando…”, envía `DELETE /files/{id}`.
        4. Backend elimina registro; responde 204; frontend alerta “Configuración eliminada” y refresca lista.

8. **Descargar Fichero**

    * **Actor:** Usuario autenticado.
    * **Flujo:**

        1. En modal, pulsa **Descargar**.
        2. Frontend hace `fetch(downloadURL, { headers: { Authorization: Bearer <token> } })`.
        3. Recibe Blob, crea `<a>` con `URL.createObjectURL(blob)` y `download=file_name`, ejecuta `a.click()`.
        4. Usuario obtiene el fichero en su equipo.

9. **Enviar Fichero a HDFS (Backend → HDFS)**

    * **Actor:** Usuario autenticado.
    * **Flujo:**

        1. En `/dashboard`, pulsa **Validar** en una fila; frontend muestra “Enviando…”, envía `POST /files/push/{file_name}`.
        2. Backend:

            * Verifica que `uploaded_files/{file_name}` exista; si no, responde 404.
            * Llama a WebHDFS NameNode para crear directorio `/data/bank_accounts` y ajustar permisos (`op=MKDIRS`, `op=SETPERM`).
            * Inicia creación de fichero (`op=CREATE`), recibe redirección 307 con URL de DataNode; ajusta host/puerto y sube contenido.
            * Devuelve `{ "message": "Pushed <file_name>" }`.
        3. Frontend alerta “Enviado a validar” y refresca lista.

10. **Proceso de validación en Spark (motor)**

    * **Actor:** Sistema (motor de validaciones).
    * **Flujo:**

        1. Motor arranca en contenedor Docker; sale de safe mode (`hdfs dfsadmin -safemode leave`), crea carpeta en HDFS y copia CSV.
        2. `Main.scala` ejecuta bucle de polling sobre `/data/bank_accounts`.
        3. Al detectar fichero, llama a `ExecutionManager.executeFile(path, outputTable)`.

            * `Reader.readFile(...)` carga CSV como DataFrame.
            * `FileSentinel.verifyFiles(...)` → si falla, `logTrigger(flag)` en `trigger_control` (flags 32,33,34).
            * `TypeValidator.verifyTyping(...)` → si falla, `logTrigger(...)` (flags 35-38).
            * `ReferentialIntegrityValidator.verifyIntegrity(...)` → si falla, `logTrigger(39)`.
            * `FunctionalValidator.verifyFunctional(...)` → si falla, `logTrigger(flags 40–49)`.
            * Si todo OK, `logTrigger(2)`.
        4. Borra el fichero de HDFS.

11. **Consultar Logs de Validación**

    * **Actor:** Usuario autenticado.
    * **Flujo:**

        1. Accede a `/logs`; frontend ejecuta `GET /files/logs?environment=&from_date=&to_date=`.
        2. Backend filtra registros en `trigger_control` según parámetros y devuelve array de logs.
        3. Frontend formatea `logged_at` con `toLocaleDateString('es-ES')` y muestra tabla con columnas: ID, file\_config\_id, file\_name, field\_name, environment, validation\_flag, error\_message, fecha.

*(Diagrams separados en docs/Img/Engine, docs/Img/Backend y docs/Img/Frontend respectivamente)*


---

### 3.2 Diagrama entidad-relación

**Tabla principal:** `trigger_control`

* `id (serial PK)`
* `logged_at (timestamp)`
* `file_config_id (int FK → file_configuration.id)`
* `file_name (varchar)`
* `field_name (varchar, null)`
* `environment (varchar)`
* `validation_flag (varchar)`
* `error_message (varchar, null)`

**Otras tablas relacionadas:**

* `file_configuration`

    * `id (serial PK)`
    * `file_format (varchar)`
    * `path (varchar)`
    * `file_name (varchar)`
    * `has_header (boolean)`
    * `delimiter (varchar)`
    * `quote_char (varchar)`
    * `escape_char (varchar)`
    * `date_format (varchar)`
    * `timestamp_format (varchar)`
    * `partition_columns (varchar, null)`
    * `created_by (int FK → users.id)`
    * `created_at (timestamp)`

* `semantic_layer`

    * `id (serial PK)`
    * `file_config_id (int FK → file_configuration.id)`
    * `field_name (varchar)`
    * `data_type (varchar)`
    * `length (int)`
    * `nullable (boolean)`
    * `is_pk (boolean)`
    * `format (varchar)`

* `users` (backend/front-end)

    * `id (serial PK)`
    * `email (varchar, unique)`
    * `hashed_password (varchar)`
    * `is_active (boolean, default true)`
    * `created_at (timestamp)`
    * `updated_at (timestamp)`

* `negative_flag_logs` (opcional)

    * `id (serial PK)`
    * `trigger_id (int FK → trigger_control.id)`
    * (otros campos de detalle)

**Relaciones:**

* `users` 1—\* `file_configuration` (un usuario puede tener varias configuraciones).
* `file_configuration` 1—\* `semantic_layer` (cada configuración define metadatos para múltiples campos).
* `file_configuration` 1—\* `trigger_control` (cada configuración genera múltiples logs de validación).
* `semantic_layer` (opcional) 1—\* `trigger_control` (para identificar qué campo en `trigger_control.field_name` proviene de qué metadato).
* `trigger_control` 1—\* `negative_flag_logs` (detalle de validaciones negativas).

![Diagrama ER](Img/ERD.png)

*(Ver `docs/Img/ERD.png` para la imagen completa.)*

---

### 3.3 Diagrama de clases del modelo

Se incluyen las clases más relevantes en los tres componentes.

#### 3.3.1 Motor de validaciones (Scala)

* **config**

    * `DbConfig.scala`
    * `DBConnection.scala`
    * `SparkSessionProvider.scala`

* **models**

    * `FileConfigurationCaseClass.scala`
    * `SemanticLayerCaseClass.scala`

* **services**

    * `ExecutionManager.scala`
    * `TriggerIdManager.scala`

* **utils**

    * `Reader.scala`
    * `Writer.scala`
    * `FileManager.scala`

* **validators**

    * `FileSentinel.scala`
    * `TypeValidator.scala`
    * `ReferentialIntegrityValidator.scala`
    * `FunctionalValidator.scala`

* **Main.scala**: configura el bucle de polling e invoca a `ExecutionManager`.

Ejemplo de case class:

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

*(Ver `docs/Img/Engine/` para la imagen completa.)*

---

#### 3.3.2 Frontend (React)

* **AuthContext.jsx**

    * Propiedades: `user`, `token`
    * Métodos: `login()`, `register()`, `logout()`
* **FileConfig (modelo JS)**

    * Propiedades: `id`, `fileName`, `path`, `hasHeader`, `delimiter`, `quoteChar`, `escapeChar`, `dateFormat`, `timestampFormat`, `partitionColumns`
    * Métodos: `fetchAll()`, `create()`, `update()`, `delete()`, `pushToHDFS()`, `download()`
* **ValidationLog (modelo JS)**

    * Propiedades: `id`, `fileConfigId`, `fileName`, `fieldName`, `environment`, `validationFlag`, `errorMessage`, `loggedAt`
    * Métodos: `fetchAll()`
* **Componentes principales**

    * `AppRouter` (gestiona rutas con `RequireAuth`)
    * `MainLayout` (Navbar + `<Outlet />`)
    * `Dashboard` (muestra lista de configuraciones y acciones)
    * `FileDetailModal` (modal para editar/validar/eliminar/descargar)
    * `LogsPage` (muestra logs formateando `logged_at`)

*(Ver `docs/Img/Frontend/` para la imagen completa.)*

---

#### 3.3.3 Backend (FastAPI)

* **models ORM (SQLAlchemy Async)**

    * `User`

        * `id: Integer PK`
        * `email: String(255) UNIQUE NOT NULL`
        * `hashed_password: String(255) NOT NULL`
        * `is_active: Boolean NOT NULL DEFAULT True`
        * `created_at: DateTime(timezone=True) DEFAULT now()`
        * `updated_at: DateTime(timezone=True)`
    * `FileConfiguration`

        * `id: Integer PK`
        * `file_format: String NOT NULL`
        * `path: String NOT NULL`
        * `file_name: String NOT NULL`
        * `has_header: Boolean NOT NULL`
        * `delimiter: String(1) NOT NULL`
        * `quote_char: String(1) NOT NULL`
        * `escape_char: String(1) NOT NULL`
        * `date_format: String NOT NULL`
        * `timestamp_format: String NOT NULL`
        * `partition_columns: String NULL`
    * `TriggerControl`

        * `id: Integer PK`
        * `file_config_id: Integer FK → FileConfiguration.id`
        * `file_name: String NOT NULL`
        * `field_name: String NOT NULL`
        * `environment: String NOT NULL`
        * `validation_flag: String NOT NULL`
        * `error_message: Text NULL`
        * `logged_at: DateTime(timezone=True) DEFAULT now()`
    * `NegativeFlagLog`

        * `id: Integer PK`
        * `trigger_id: Integer FK → TriggerControl.id`
        * (otros campos específicos)

* **Clases de servicio**

    * `file_service.py`

        * `save_and_register_file(file: UploadFile, db: AsyncSession) → int`
    * `hdfs_sync.py`

        * `push_file_to_hdfs(file_name: str)`

* **Controladores (routers)**

    * `auth.py`: `/auth/register`, `/auth/login`
    * `files.py`: `/files/upload`, `/files/push/{file_name}`, `/files/download/{file_name}`, `/files/`, `/files/{id}`, `/files/{id}` (GET, PATCH, DELETE), `/files/logs`
    * `health.py`: `/health` (healthcheck)

*(Ver `docs/Img/Backend/` para la imagen completa.)*

---

### 3.4 Diagramas de secuencia

Se incluyen los diagramas de secuencia más relevantes para cada componente. A continuación se describen de forma textual; ver `docs/` para los PNG o PlantUML.

#### 3.4.1 Secuencia: Subida de fichero (Frontend → Backend)

1. **Usuario → Dashboard (Frontend):** selecciona CSV y pulsa **Subir Fichero**.
2. **Dashboard → Axios (`POST /files/upload`):** envía `multipart/form-data` al backend.
3. **Backend (`files.py`) → `file_service.save_and_register_file()`:**

    * Guarda CSV en `uploaded_files/`.
    * Inserta/actualiza registro en `file_configuration`.
4. **Backend → Dashboard:** responde con `{ "file_config_id": X }` (201).
5. **Dashboard:** muestra alerta “Fichero subido ID: X” y llama `fetchConfigs()`.

#### 3.4.2 Secuencia: Enviar fichero a HDFS (Frontend → Backend → HDFS)

1. **Usuario → Dashboard:** pulsa **Validar**.
2. **Dashboard → Axios (`POST /files/push/{file_name}`):** solicita al backend empuje a HDFS.
3. **Backend (`files.py`) → `hdfs_sync.push_file_to_hdfs(file_name)`:**

    * Verifica existencia local en `uploaded_files/`.
    * Llama a WebHDFS NameNode (`MKDIRS`, `SETPERM`).
    * Llama `CREATE`, recibe 307 con header `Location` apuntando al DataNode.
    * Ajusta URL para apuntar a `hdfs_datanode_host:hdfs_datanode_port`.
    * Abre CSV y hace `PUT` a DataNode.
4. **HDFS DataNode → Backend:** responde 201 Created si éxito.
5. **Backend → Dashboard:** responde `{ "message": "Pushed <file_name>" }`.
6. **Dashboard:** muestra alerta “Enviado a validar” y llama `fetchConfigs()`.

#### 3.4.3 Secuencia: Proceso de validación en Spark (Motor)

1. **Contenedor `validation-engine` → HDFS:**

    * Ejecuta `hdfs dfsadmin -safemode leave`.
    * `hdfs dfs -mkdir -p /data/bank_accounts`.
    * `hdfs dfs -put -f /local_bank_accounts/*.csv /data/bank_accounts`.
    * Ajusta permisos `hdfs dfs -chmod -R 777 /data/bank_accounts`.
2. **Contenedor Spark (`spark-submit Main`) → HDFS.listStatus():** detecta CSV.
3. **Main → ExecutionManager.executeFile(path, outputTable):**

    * Llama `Reader.readFile(...)` → devuelve DataFrame particionado.
    * Llama `FileSentinel.verifyFiles(...)` → si falla, `logTrigger(flag)` y salir.
    * Llama `TypeValidator.verifyTyping(...)` → si falla, `logTrigger(flag)` y salir.
    * Llama `ReferentialIntegrityValidator.verifyIntegrity(...)` → si falla, `logTrigger(39)` y salir.
    * Llama `FunctionalValidator.verifyFunctional(...)` → si falla, `logTrigger(flags 40–49)` y salir.
    * Si todo OK, `logTrigger(2)`.
    * Escribe logs en PostgreSQL (`df.write.mode("append").jdbc(...)`).
    * `HDFS.delete(path)`.

#### 3.4.4 Secuencia: Listar Logs de Validación (Frontend → Backend → BD)

1. **Usuario → LogsPage (Frontend):** al montar, llama `getLogs()`.
2. **LogsPage → Axios (`GET /files/logs?environment=&from_date=&to_date=`):** solicita logs.
3. \*\*Backend (`files.py`) → consulta `trigger_control` filtrando por parámetros.
4. **Backend → LogsPage:** devuelve array de objetos JSON con campos de log.
5. **LogsPage:** recorre cada registro y formatea `logged_at` con `toLocaleDateString('es-ES')`.
6. **LogsPage:** muestra tabla con columnas: ID, file\_config\_id, file\_name, field\_name, environment, validation\_flag, error\_message, fecha.

---
