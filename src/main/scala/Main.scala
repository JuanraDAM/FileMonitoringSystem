import config.DBConnection
import org.apache.hadoop.fs.{FileStatus, FileSystem, Path}
import org.apache.hadoop.security.AccessControlException
import org.apache.spark.sql.SparkSession
import services.ExecutionManager

/**
 * Objeto principal que lanza el bucle de polling sobre HDFS y ejecuta el motor de validaciones
 * para cada fichero nuevo detectado en el directorio configurado.
 *
 * Lee URL de la base de datos de DbConfig, arranca la sesión de Spark y procesa
 * los ficheros eliminándolos tras el procesamiento.
 */
object Main {
  /**
   * Punto de entrada de la aplicación.
   *
   * @param args Parámetros de línea de comandos (no usados).
   */
  def main(args: Array[String]): Unit = {
    // 1) Conexión DB
    val connectionOption = DBConnection.getConnection()
    System.setProperty("io.netty.tryReflectionSetAccessible", "true")

    connectionOption match {
      case Some(conn) =>
        try {
          println("✅ Conexión a la base de datos establecida con éxito")

          // 2) SparkSession
          implicit val spark: SparkSession = config.SparkSessionProvider.getSparkSession

          // 3) Parámetros de polling
          val inputDir    = sys.env.getOrElse("INPUT_DIR", "/data/bank_accounts")
          val outputTable = sys.env.getOrElse("OUTPUT_TABLE", "trigger_control")
          val intervalMs  = sys.env.getOrElse("POLL_INTERVAL_MS", "10000").toLong

          val fs      = FileSystem.get(spark.sparkContext.hadoopConfiguration)
          val dirPath = new Path(inputDir)
          require(fs.exists(dirPath) && fs.isDirectory(dirPath),
            s"❌ INPUT_DIR no válido: $inputDir")

          println(s"▶️ Escuchando $inputDir cada $intervalMs ms…")
          while (true) {
            // 4) Listar y procesar
            val files: Array[FileStatus] =
              fs.listStatus(dirPath)
                .filter { status =>
                  val name = status.getPath.getName
                  status.isFile &&
                    !name.startsWith(".") &&
                    !name.endsWith("._COPYING_")
                }

            files.foreach { status =>
              val path = status.getPath.toString
              println(s"🔔 Detectado fichero: $path")
              ExecutionManager.executeFile(path, outputTable)
              try {
                if (fs.delete(status.getPath, false)) {
                  println(s"🗑️ Borrado HDFS: $path")
                } else {
                  System.err.println(s"⚠️ No se pudo borrar (sin excepción): $path")
                }
              } catch {
                case ace: AccessControlException =>
                  System.err.println(s"⚠️ Sin permiso para borrar $path: ${ace.getMessage}")
                case t: Throwable =>
                  System.err.println(s"⚠️ Error borrando $path: ${t.getMessage}")
              }
            }
            Thread.sleep(intervalMs)
          }

        } finally {
          conn.close()
        }

      case None =>
        println("❌ Error al conectar a la base de datos")
        System.exit(1)
    }
  }
}