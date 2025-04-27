import config.DBConnection
import org.apache.spark.sql.SparkSession

object Main {
  def main(args: Array[String]): Unit = {

    val connectionOption = DBConnection.getConnection()
    System.setProperty("io.netty.tryReflectionSetAccessible", "true")

    connectionOption match {
      case Some(connection) =>
        try {
          println("✅ Conexión a la base de datos establecida con éxito")
        } finally {
          connection.close()
        }
      case None =>
        println("❌ Error al conectar a la base de datos")
    }


    val masterUrl = "spark://localhost:7077"
    val hdfsUrl = "hdfs://hadoop-namenode:9000"
    // echo "127.0.0.1 hadoop-namenode" | sudo tee -a /etc/hosts

    val spark = SparkSession.builder()
      .appName("Spark + HDFS Test")
      .master(masterUrl)
      .config("spark.driver.host", "host.docker.internal")
      .config("spark.driver.bindAddress", "0.0.0.0")
      .config("spark.hadoop.fs.defaultFS", hdfsUrl)
      .getOrCreate()

    spark.sparkContext.setLogLevel("ERROR")

    import spark.implicits._

    println(s"✅ Spark arrancado en $masterUrl")

    val dfLocal = Seq(
      ("Juan",    "Pérez",     23),
      ("María",   "García",    34),
      ("Luis",    "López",     45),
      ("Ana",     "Sánchez",   28),
      ("Carlos",  "Rodríguez", 39)
    ).toDF("nombre","apellido","edad")

    println("---- DF local de prueba ----")
    dfLocal.show()

    try {
      println(s"🔎 Leyendo /data/ejemplo.csv desde HDFS ($hdfsUrl)…")
      val dfHdfs = spark.read
        .option("header","true")
        .csv(s"$hdfsUrl/data/ejemplo.csv")
      dfHdfs.show(false)
    } catch {
      case e: Throwable =>
        println(s"❌ Error leyendo HDFS: ${e.getMessage}")
    } finally {
      spark.stop()
    }


  }
}
