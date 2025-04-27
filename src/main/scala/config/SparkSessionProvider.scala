package config

import org.apache.spark.sql.SparkSession

object SparkSessionProvider {
  val masterUrl = "spark://localhost:7077"
  val hdfsUrl = "hdfs://hadoop-namenode:9000"
  lazy val sparkSession: SparkSession = SparkSession.builder()
    .appName("Proyecto Fin de grado")
    .master(masterUrl)
    .config("spark.driver.host", "host.docker.internal")
    .config("spark.driver.bindAddress", "0.0.0.0")
    .config("spark.hadoop.fs.defaultFS", hdfsUrl)
    .config("spark.sql.shuffle.partitions", "200")
    .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
    .config("spark.kryo.registrationRequired", "true")
    // Es importante especificar tu registrator personalizado
    .config("spark.kryo.registrator", "serialization.MyKryoRegistrator")
    .config("spark.default.parallelism", "12")
    .config("spark.hadoop.io.nativeio.enabled", "false")
    .getOrCreate()


  def getSparkSession: SparkSession = sparkSession
}