# 🧪 Data Validation Engine - Entorno Local de Producción

Este entorno dockerizado permite ejecutar un **motor de validación de datos** de forma local simulando un entorno de producción. Utiliza tecnologías de análisis y procesamiento de datos como:

- **Apache Superset** para visualización
- **Apache Kafka** para ingesta en tiempo real
- **Hadoop HDFS** como sistema de almacenamiento distribuido
- **PostgreSQL** como base de datos relacional

---

## ⚙️ Tecnologías utilizadas

| Componente     | Funcionalidad                                  |
|----------------|-------------------------------------------------|
| Superset       | Dashboarding y exploración de datos             |
| PostgreSQL     | Almacén interno de Superset (metadatos)         |
| Hadoop HDFS    | Sistema de archivos distribuido                 |
| Kafka + Zookeeper | Ingesta y procesamiento de datos en tiempo real |

---

## 🚀 Puesta en marcha

### 1. Clonar el repositorio

```bash
git clone <URL_DEL_REPO>
cd <NOMBRE_DEL_PROYECTO>
```

### 2. Iniciar el entorno

```bash
docker compose up -d
```

### 3. Verificar que todos los servicios estén activos

```bash
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```

Asegúrate de que los servicios estén en estado `Up (healthy)`.

---

## 🌐 Accesos y puertos

| Servicio        | URL / Puerto         | Credenciales             |
|-----------------|----------------------|--------------------------|
| Superset        | http://localhost:8088 | Usuario: `admin` <br> Contraseña: `1234` |
| HDFS Web UI     | http://localhost:9870 | -                        |
| Kafka Broker    | `kafka:9092` (interno) | -                       |
| Zookeeper       | `zookeeper:2181` (interno) | -                  |

---

## 📁 Estructura del entorno

```text
📦 Proyecto
├── docker-compose.yml
├── superset_config.py
└── README-cluster.md (este archivo)
```

---

## 📦 Volúmenes Docker

| Volumen              | Uso                                |
|----------------------|-------------------------------------|
| superset-db-data     | Datos persistentes de PostgreSQL    |
| hadoop-namenode      | Metadatos del Namenode              |
| hadoop-datanode      | Bloques de datos en el Datanode     |

---

## 🧪 Validaciones de funcionamiento

### 🔍 Superset

1. Accede a [http://localhost:8088](http://localhost:8088)
2. Inicia sesión:  
   - Usuario: `admin`  
   - Contraseña: `1234`

### 🧱 HDFS

```bash
docker exec -it hadoop-namenode bash
hdfs dfsadmin -report
```

Ejemplo para subir archivos:
```bash
echo "prueba" > test.txt
hdfs dfs -mkdir -p /validaciones
hdfs dfs -put test.txt /validaciones
hdfs dfs -ls /validaciones
```

### 📨 Kafka

Para listar los topics:
```bash
docker run --rm -it --network=superset-net confluentinc/cp-kafka \
  kafka-topics --bootstrap-server kafka:9092 --list
```

Ejemplo para enviar datos a un topic:
```bash
echo '{"dato":"ejemplo"}' | docker run -i --rm --network=superset-net confluentinc/cp-kafka \
  kafka-console-producer --broker-list kafka:9092 --topic test-topic
```

---

## 📊 Uso típico

1. Productores de datos escriben en Kafka.
2. Datos pueden procesarse o guardarse en HDFS.
3. Superset se conecta a las fuentes para visualizar validaciones.

Ideal para:
- Probar flujos ETL en local.
- Simular ingesta de datos en tiempo real.
- Validar calidad y estructura de datasets.
- Crear dashboards de monitoreo de datos.

---

## 🧼 Apagar y limpiar el entorno

```bash
docker compose down -v
```

Esto eliminará contenedores y volúmenes persistentes.

---

## 📌 Notas de producción

- ⚠️ Este entorno corre en local y está preparado para simular producción, pero no debe usarse como tal sin medidas adicionales de seguridad.
- Para un entorno real se recomienda:
  - Autenticación externa en Superset (OAuth, LDAP, etc.)
  - Proxy inverso (como Nginx)
  - Monitoreo con Prometheus y Grafana
  - Backup y recuperación de volúmenes

---

## 🤝 Autor y soporte

Desarrollado como parte de un sistema de validación de datos en entornos analíticos.  
Para mejoras o incidencias, abre un _issue_ o contacta al autor del proyecto.
