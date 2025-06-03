
## 2. Especificación de Requisitos

### 2.1 Requisitos funcionales

A continuación se listan los requisitos funcionales unificados, tomando en cuenta los tres componentes (frontend, backend, motor de validaciones):

1. **Autenticación de usuarios**

    * RF1.1: Registro de usuario con email y contraseña (`POST /auth/register`).
    * RF1.2: Login de usuario (`POST /auth/login`), devuelve JWT y redirige al dashboard.
    * RF1.3: Logout que elimina el token de `localStorage` y redirige a `/login`.
    * RF1.4: Cualquier petición protegida que retorne 401 Unauthorized debe borrar el token y redirigir a `/login`.

2. **Gestión de configuraciones de fichero (`file_configuration`)**

    * RF2.1: Subir CSV al backend (`POST /files/upload`) con `multipart/form-data`.
    * RF2.2: Al subir, insertar o actualizar parámetros en `file_configuration` (has\_header, delimiter, quote\_char, escape\_char, date\_format, timestamp\_format, partition\_columns).
    * RF2.3: Listar configuraciones (`GET /files/`), mostrar ID, file\_name, path, has\_header, delimiter, quote\_char, escape\_char, date\_format, timestamp\_format, partition\_columns.
    * RF2.4: Obtener detalles de una configuración (`GET /files/{id}`), editar parámetros (`PATCH /files/{id}`), eliminar configuración (`DELETE /files/{id}`).
    * RF2.5: Descargar CSV original desde backend (`GET /files/download/{file_name}`).

3. **Sincronización con HDFS**

    * RF3.1: Enviar fichero desde backend a HDFS (`POST /files/push/{file_name}`), creando directorio en HDFS (`op=MKDIRS`), ajustando permisos (`op=SETPERM`) y subiendo contenido (`op=CREATE`).
    * RF3.2: Backend debe manejar redireccionamiento 307 de NameNode a DataNode, adaptando la URL de destino.
    * RF3.3: Validar existencia local del fichero antes de empujar; si no existe, responder 404.

4. **Proceso de validación (motor Scala/Spark)**

    * RF4.1: Monitorizar HDFS en `/data/bank_accounts`; detectar nuevos ficheros en polling batch.
    * RF4.2: Por cada fichero detectado, procesarlo de forma independiente.
    * RF4.3: Validación estructural:

        * Verificar delimitador, encabezados y número de columnas según `file_configuration`.
        * Flags:

            * 32: Delimiter mismatch
            * 33: Header mismatch
            * 34: Column count per row mismatch
    * RF4.4: Validación tipológica:

        * Comprobar tipos, rangos (fechas, números), formatos de texto según `semantic_layer`.
        * Flags:

            * 35: Tipo inválido
            * 36: Nulo indebido
            * 37: Longitud excedida
            * 38: Formato texto inválido
    * RF4.5: Integridad referencial:

        * Verificar unicidad de claves primarias según metadatos.
        * Flag:

            * 39: Duplicado PK
    * RF4.6: Validación de negocio:

        * Reglas específicas del dominio bancario:

            * Formato de cuenta (`^[A-Za-z0-9]{10}$` → 40).
            * `credit_score` entre 300 y 850 → 41.
            * `risk_score` entre 0 y 100 → 42.
            * Mayor de 18 años (DOB) → 43.
            * Si `status`=“Active”, `balance`>=0; si “Closed”, `balance`=0 → 44/45.
            * Si `account_type`=“Checking”, `interest_rate`=0 → 46.
            * `overdraft_limit`>=0 y válido → 47.
            * Si `is_joint_account`=“Yes”, `num_transactions`>=2 → 48.
            * Si `num_transactions`=0, `avg_transaction_amount`=0 → 49.
    * RF4.7: Registrar resultados en `trigger_control` (timestamp, file\_config\_id, file\_name, field\_name, environment, validation\_flag, error\_message).
    * RF4.8: Tras procesar (OK o KO), borrar el fichero de HDFS para evitar reprocesamiento.

5. **Visualización de logs de validación**

    * RF5.1: Listar logs (`GET /files/logs`) filtrables por `environment`, `from_date` y `to_date`.
    * RF5.2: Formatear `logged_at` a `DD/MM/YYYY, hh:mm:ss` en zona Madrid.
    * RF5.3: Mostrar tabla con columnas: ID, file\_config\_id, file\_name, field\_name, environment, validation\_flag, error\_message, fecha.

6. **UX/UI del frontend**

    * RF6.1: Formularios de registro/login con validación de campos obligatorios.
    * RF6.2: Indicadores de carga en botones (“Subiendo…”, “Enviando…”, “Guardando…”, “Eliminando…”).
    * RF6.3: Modal de detalles que se cierra al hacer clic fuera o en botón “×”.
    * RF6.4: Alertas (`alert()`) para notificar éxito o error.
    * RF6.5: Navbar con enlaces en negrita (**Dashboard**, **Logs**) y botón **Logout** en color naranja.

---

### 2.2 Requisitos no funcionales

1. **Seguridad**

    * RNF1.1: JWT con expiración configurable; token almacenado en `localStorage`.
    * RNF1.2: Backend protege rutas con dependencia `get_current_user`; reacciona a 401 borrando token en frontend.
    * RNF1.3: HDFS: al crear directorios, aplicar `chmod -R 777` para permitir lectura/escritura a todos los usuarios.
    * RNF1.4: Contraseñas hasheadas con bcrypt.

2. **Rendimiento**

    * RNF2.1: El motor de validaciones debe procesar >1M filas por partición en < 1 minuto.
    * RNF2.2: Uso óptimo de particiones en Spark y `fetchSize` en conexiones JDBC.
    * RNF2.3: El frontend debe cargar en <2 s en producción básica.

3. **Escalabilidad**

    * RNF3.1: Motor de validaciones diseñado para aumentar nodos Spark sin cambios en el código.
    * RNF3.2: Backend asíncrono (FastAPI + SQLAlchemy Async) para operaciones I/O intensivo.
    * RNF3.3: Posibilidad de migrar a Spark Structured Streaming en el futuro.

4. **Disponibilidad**

    * RNF4.1: El motor debe mantenerse activo 24/7; al iniciar, ejecutar `hdfs dfsadmin -safemode leave`.
    * RNF4.2: Contenedores orquestados con Docker Compose deben reiniciarse automáticamente si fallan.

5. **Mantenibilidad**

    * RNF5.1: Arquitectura modular (SOLID, Clean Architecture).
    * RNF5.2: Código documentado con ScalaDoc (motor).
    * RNF5.3: Separación de capas (API, servicios, modelos, configuraciones) tanto en backend como en frontend.

6. **Compatibilidad**

    * RNF6.1: Frontend soporta navegadores modernos (Chrome, Firefox, Edge).
    * RNF6.2: No se requiere soporte para Internet Explorer.

7. **Tolerancia a fallos**

    * RNF7.1: Capturar excepciones en motor (sin detener el bucle principal) y registrar errores.
    * RNF7.2: En backend, manejar timeouts y errores de WebHDFS con lógica de reintentos básica.

---
