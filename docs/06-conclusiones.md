
## 6. Conclusiones

### 6.1 Dificultades

1. **ClusterID incompatibles (HDFS)**

    * Al cambiar o recrear imágenes de Hadoop, DataNode arroja error de “Incompatible clusterIDs”.
    * Solución: borrar volúmenes `hadoop-namenode` y `hdfs-data-datanode` antes de levantar.

2. **Safe Mode en HDFS**

    * HDFS arranca en “safe mode” si detecta inconsistencias o falta de bloques.
    * Se incorporó `hdfs dfsadmin -safemode leave` en el contenedor `validation-engine` para forzar la salida.

3. **Manejo de redirección en WebHDFS**

    * WebHDFS devuelve un redirect 307 con URL del DataNode; ajustar host y puerto es crítico.
    * Se debió detectar y procesar manualmente el header `Location` antes de subir el contenido.

4. **Tamaño de ficheros grandes**

    * Al probar con CSV de varios GB, los DataNode se quedaban sin espacio y se excluían.
    * Se recomendó montar `/tmp` de Spark Workers en volúmenes dedicados (`docker_validation_tmp`) o usar `tmpfs`.

5. **Coordinación entre frontend y backend (token y rutas)**

    * La gestión de 401 Unauthorized requirió interceptores en Axios para borrar token y redirigir a login.
    * Asegurarse de que todas las peticiones protegidas incluyeran el header `Authorization`.

6. **Configuración de permisos HDFS desde FastAPI**

    * Ejecutar comandos Hadoop desde contenedor Python (shell) generaba a veces problemas de path.
    * Se optó por usar HTTP WebHDFS en lugar de comandos nativos en Python.

7. **SQLAlchemy Async y migraciones**

    * Cambios frecuentes en el modelo de datos (por ejemplo, `TIMESTAMP` a `TIMESTAMPTZ`) implicaron migraciones manuales.
    * No se incluyó un sistema de migraciones (Alembic), por lo que hubo que recrear tablas en producción.

8. **Paginación y filtros en logs**

    * Al crecer el número de logs, la consulta simple (`SELECT * FROM trigger_control`) se volvía lenta.
    * Se implementaron filtros por fecha y entorno, pero falta paginación y límites por defecto.

9. **Interfaz de usuario básica**

    * Al usar CSS puro, costó diseñar un estilo consistente y responsivo.
    * Se recomendó integrar un framework CSS (Tailwind, Material UI) para mejorar UX/UI.

---

### 6.2 Mejoras

1. **Migrar a Spark Structured Streaming**

    * Pasar de polling batch a procesamiento near‐real‐time con Structured Streaming y checkpoints.

2. **Orquestación con Kubernetes**

    * Desplegar Hadoop, Spark y el motor de validaciones en un clúster Kubernetes usando Helm charts y StatefulSets.
    * Facilitar elasticidad y escalado automático de nodos.

3. **Monitorización con Prometheus & Grafana**

    * Exponer métricas de Spark, HDFS y PostgreSQL.
    * Crear dashboards de latencia, tasas de error y uso de recursos.

4. **Alertas automáticas**

    * Enviar notificaciones por correo o webhook cuando ocurran validaciones negativas (flags críticos).

5. **Paginación y filtros avanzados en frontend**

    * Añadir paginación en listas de configuraciones y logs.
    * Filtros por fecha, entorno, flag de validación, nombre de fichero.

6. **Soporte para formatos adicionales**

    * Añadir validación y lectura de CSV comprimidos, Parquet, Avro u ORC.
    * Implementar validaciones basadas en schemas Avro o JSON Schema.

7. **Carga fragmentada (chunked upload)**

    * Para CSV muy grandes, usar multipart upload o chunking en frontend/backend para evitar timeouts.

8. **Integración de notificaciones en tiempo real**

    * Usar WebSockets o WebPubSub para notificar al frontend sobre el estado de la validación en tiempo real.

9. **Internacionalización (i18n)**

    * Permitir cambiar idioma en frontend (ES, EN).
    * Formateo de fechas y mensajes localizados.

10. **Documentación de API con Swagger/Redoc**

    * FastAPI ya genera documentación automática; extenderla con ejemplos de petición/respuesta y código de errores.

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
|           39           | Duplicado PK (integridad referencial)            |
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

