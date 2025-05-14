package config

import org.apache.spark.sql.SparkSession

/**
 * Proveedor de instancia única de SparkSession configurada
 * con parámetros de cluster, HDFS y serialización.
 */
object SparkSessionProvider {
  /**
   * Obtiene la SparkSession configurada.
   * @return instancia de SparkSession.
   */
  def getSparkSession: SparkSession = sparkSession

  private val masterUrl = "spark://localhost:7077"
  private val hdfsUrl   = "hdfs://hadoop-namenode:9000"

  private lazy val sparkSession: SparkSession = SparkSession.builder()
    .appName("Proyecto Fin de grado")
    .master(masterUrl)
    .config("spark.driver.host", "host.docker.internal")
    .config("spark.driver.bindAddress", "0.0.0.0")
    .config("spark.hadoop.fs.defaultFS", hdfsUrl)
    .config("spark.sql.shuffle.partitions", "200")
    .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
    .config("spark.kryo.registrationRequired", "false")
    .config("spark.kryo.registrator", "serialization.MyKryoRegistrator")
    .config("spark.default.parallelism", "12")
    .config("spark.hadoop.io.nativeio.enabled", "false")
    .getOrCreate()

  sparkSession.sparkContext.setLogLevel("ERROR")
}