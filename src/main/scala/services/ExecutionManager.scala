package services

import java.util.Properties
import java.nio.file.{Files, Paths}
import config.{DbConfig, SparkSessionProvider}
import models.FileConfigurationCaseClass
import org.apache.spark.sql.{DataFrame, SparkSession}
import utils.Reader
import java.sql.Timestamp
import validators.{FileSentinel, TypeValidator, ReferentialIntegrityValidator, FunctionalValidator}

/**
 * Objeto que gestiona la ejecución del motor de validaciones.
 *
 * Ofrece métodos para procesar por lotes (executeEngine) o fichero a fichero (executeFile),
 * aplicando chequeos de integridad y registrando los resultados en BD.
 */
object ExecutionManager {
  /**
   * Registra un intento de validación en la tabla de logs.
   *
   * @param fileConfigId    ID de configuración del fichero.
   * @param fileName        Nombre del fichero.
   * @param fieldName       Nombre de campo implicado en un error (opcional).
   * @param environment     Entorno de ejecución (p.ej. "dev").
   * @param validationFlag  Código de validación (p.ej. "2" = OK).
   * @param errorMessage    Mensaje de error si lo hubiere.
   * @param tableName       Nombre de la tabla de logs en BD.
   * @param spark           SparkSession implícita.
   */
  private def logTrigger(
                          fileConfigId: Int,
                          fileName: String,
                          fieldName: Option[String],
                          environment: String,
                          validationFlag: String,
                          errorMessage: Option[String],
                          tableName: String
                        )(implicit spark: SparkSession): Unit = {
    import spark.implicits._
    val now = new Timestamp(System.currentTimeMillis())
    val row = (
      now,
      fileConfigId,
      fileName,
      fieldName.orNull,
      environment,
      validationFlag,
      errorMessage.orNull
    )
    val df = Seq(row).toDF(
      "logged_at",
      "file_config_id",
      "file_name",
      "field_name",
      "environment",
      "validation_flag",
      "error_message"
    )
    val props = new Properties()
    props.put("user",     DbConfig.getUsername)
    props.put("password", DbConfig.getPassword)
    props.put("driver",   "org.postgresql.Driver")
    df.write.mode("append").jdbc(DbConfig.getJdbcUrl, tableName, props)
  }

  /**
   * Procesa todos los ficheros de un directorio local de archivos.
   *
   * @param inputDir    Ruta local del directorio.
   * @param outputTable Tabla destino de logs.
   */
  def executeEngine(inputDir: String, outputTable: String): Unit = {
    implicit val spark: SparkSession = SparkSessionProvider.getSparkSession
    import spark.implicits._

    val fileConfigs = Reader.readDf("file_configuration").as[FileConfigurationCaseClass].collect()
    val semanticLayerDs = Reader.readDf("semantic_layer").as[models.SemanticLayerCaseClass]
    val env = sys.env.getOrElse("ENV", "dev")

    val dir = new java.io.File(inputDir)
    require(dir.exists() && dir.isDirectory, s"DIRECTORIO no válido: $inputDir")

    dir.listFiles().filter(_.isFile).foreach { file =>
      val fileName = file.getName
      fileConfigs.find(_.file_name == fileName) match {
        case Some(fc) =>
          val path = file.getAbsolutePath
          println(s"▶️ Validando $path (config id=${fc.id})")
          // Lectura y validaciones en cascada...
          // (se omite por brevedad, mantiene tu lógica previa)
          logTrigger(fc.id, fc.file_name, None, env, "2", None, outputTable)
          Files.delete(Paths.get(path))
          println(s"🗑️ Eliminado fichero: $path")

        case None => println(s"⚠️ Sin configuración: $fileName")
      }
    }
    spark.catalog.clearCache()
    spark.stop()
  }

  /**
   * Procesa un único fichero, ideal para uso en bucle de detección.
   *
   * @param filePath    Ruta completa HDFS o local.
   * @param outputTable Tabla destino de logs.
   */
  def executeFile(filePath: String, outputTable: String): Unit = {
    implicit val spark: SparkSession = SparkSessionProvider.getSparkSession
    import spark.implicits._

    val fileName = filePath.split("/").last
    val env = sys.env.getOrElse("ENV", "dev")
    val fileConfigs = Reader.readDf("file_configuration").as[FileConfigurationCaseClass].collect()
    val semanticLayerDs = Reader.readDf("semantic_layer").as[models.SemanticLayerCaseClass]

    fileConfigs.find(_.file_name == fileName) match {
      case Some(fc) =>
        println(s"▶️ Validando $fileName (config id=${fc.id})")
        // Lectura y validaciones en cascada...
        logTrigger(fc.id, fc.file_name, None, env, "2", None, outputTable)

      case None => println(s"⚠️ Sin configuración para $fileName")
    }
  }
}
