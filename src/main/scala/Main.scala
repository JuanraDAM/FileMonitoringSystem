import config.DBConnection
import org.apache.spark.sql.SparkSession
import services.ExecutionManager.executeEngine

object Main {
  def main(args: Array[String]): Unit = {

    val connectionOption = DBConnection.getConnection()
    System.setProperty("io.netty.tryReflectionSetAccessible", "true")

    connectionOption match {
      case Some(connection) =>
        try {
          println("✅ Conexión a la base de datos establecida con éxito")

          executeEngine()

        } finally {
          connection.close()
        }
      case None =>
        println("❌ Error al conectar a la base de datos")
    }

  }
}
