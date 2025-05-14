package config

import org.apache.spark.sql.SparkSession

/**
 * Proveedor de instancia única de SparkSession configurada
 * con parámetros de cluster, HDFS y serialización.
 */
object SparkSessionProvider {
  /** URL del maestro de Spark, configurable vía SPARK_MASTER_URL */
  private val masterUrl: String =
    sys.env.getOrElse("SPARK_MASTER_URL", "spark://spark-master:7077")

  /** URL de HDFS, configurable vía HDFS_URL */
  private val hdfsUrl: String =
    sys.env.getOrElse("HDFS_URL", "hdfs://hadoop-namenode:9000")

  /** SparkSession singleton configurada para el clúster. */
  lazy val sparkSession: SparkSession = SparkSession.builder()
    .appName("Proyecto Fin de grado")
    .master(masterUrl)
    .config("spark.ui.enabled", "false")                             // Desactiva Spark UI para evitar MetricsSystem errors
    .config("spark.driver.host", sys.env.getOrElse("DRIVER_HOST", "validation-engine"))
    .config("spark.driver.bindAddress", "0.0.0.0")
    .config("spark.hadoop.fs.defaultFS", hdfsUrl)
    .config("spark.sql.shuffle.partitions", "200")
    .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
    .config("spark.kryo.registrationRequired", "false")
    .config("spark.kryo.registrator", "serialization.MyKryoRegistrator")
    .config("spark.default.parallelism", "12")
    .config("spark.hadoop.io.nativeio.enabled", "false")
    .getOrCreate()

  // Nivel de log reducido
  sparkSession.sparkContext.setLogLevel("ERROR")

  /** Devuelve la SparkSession compartida */
  def getSparkSession: SparkSession = sparkSession
}