package services

import java.util.Properties
import java.nio.file.{Files, Paths}
import config.{DbConfig, SparkSessionProvider}
import models.FileConfigurationCaseClass
import org.apache.spark.sql.{DataFrame, SparkSession}
import utils.Reader
import java.time.Instant
import validators.{FileSentinel, TypeValidator, ReferentialIntegrityValidator, FunctionalValidator}

object ExecutionManager {
  import java.sql.Timestamp

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

    // Timestamp tipo SQL para que Spark lo reconozca como TimestampType
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
      "logged_at",        // coincide con la columna de tu tabla
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

    df.write
      .mode("append")
      .jdbc(DbConfig.getJdbcUrl, tableName, props)
  }


  /**
   * Escanea un directorio, procesa cada fichero que tenga configuración en la tabla
   */
  def executeEngine(inputDir: String, outputTable: String): Unit = {
    implicit val spark: SparkSession = SparkSessionProvider.getSparkSession
    import spark.implicits._

    // Carga configuración de ficheros a validar
    val fileConfigs = Reader.readDf("file_configuration")
      .as[FileConfigurationCaseClass]
      .collect()

    // Carga capa semántica
    val semanticLayerDs = Reader.readDf("semantic_layer")
      .as[models.SemanticLayerCaseClass]

    val env = sys.env.getOrElse("ENV", "dev")

    // Procesa batch de ficheros en directorio
    val dir = new java.io.File(inputDir)
    if (dir.exists() && dir.isDirectory) {
      dir.listFiles().filter(_.isFile).foreach { file =>
        val fileName = file.getName
        // Busca la configuración para este fichero
        fileConfigs.find(_.file_name == fileName) match {
          case Some(fc) =>
            val filePath = file.getAbsolutePath
            println(s"▶️ Validando fichero ${filePath} con config id=${fc.id}")

            // 0️⃣ Lectura
            val dfOrError: Either[(String, Option[String]), DataFrame] = try {
              Right(
                Reader.readFile(filePath, Map(
                  "header"      -> fc.has_header.toString,
                  "sep"         -> fc.delimiter,
                  "inferSchema" -> "false"
                )).cache()
              )
            } catch {
              case ex: Exception => Left(("30", Some(ex.getMessage)))
            }

            dfOrError match {
              case Left((flag, errMsg)) =>
                logTrigger(fc.id, fc.file_name, None, env, flag, errMsg, outputTable)

              case Right(df) =>
                // 1️⃣ FileSentinel
                val (fFlag, fOk, fErr, fField) = FileSentinel.verifyFiles(df, fc)
                if (!fOk) {
                  logTrigger(fc.id, fc.file_name, fField, env, fFlag, fErr, outputTable)
                } else {
                  // 2️⃣ Typing
                  val (tFlag, tOk, tErr, tField) = TypeValidator.verifyTyping(df, fc, semanticLayerDs)
                  if (!tOk) {
                    logTrigger(fc.id, fc.file_name, tField, env, tFlag, tErr, outputTable)
                  } else {
                    // 3️⃣ Referential
                    val (rFlag, rOk, rErr, rField) = ReferentialIntegrityValidator.verifyIntegrity(df, semanticLayerDs)
                    if (!rOk) {
                      logTrigger(fc.id, fc.file_name, rField, env, rFlag, rErr, outputTable)
                    } else {
                      // 4️⃣ Functional
                      val (uFlag, uOk, uErr, uField) = FunctionalValidator.verifyFunctional(df, fc)
                      if (!uOk) {
                        logTrigger(fc.id, fc.file_name, uField, env, uFlag, uErr, outputTable)
                      } else {
                        // ✅ Todo OK
                        logTrigger(fc.id, fc.file_name, None, env, "2", None, outputTable)
                      }
                    }
                  }
                }
            }

            // Tras procesar, elimina fichero
            Files.delete(Paths.get(file.getAbsolutePath))
            println(s"🗑️ Eliminado fichero: ${filePath}")

          case None =>
            println(s"⚠️ No hay configuración para el fichero: $fileName, se omite.")
        }
      }
    } else {
      System.err.println(s"❌ Directorio no encontrado o no es directorio: $inputDir")
      System.exit(1)
    }

    // Limpieza de cache y parada
    spark.catalog.clearCache()
    spark.stop()
  }

  /**
   * Procesa un único fichero HDFS o local, aplicando todas las validaciones.
   */
  def executeFile(filePath: String, outputTable: String): Unit = {
    implicit val spark: SparkSession = SparkSessionProvider.getSparkSession
    import spark.implicits._

    val fileName = filePath.split("/").last
    val env      = sys.env.getOrElse("ENV", "dev")

    // Cargo configuraciones
    val fileConfigs = Reader.readDf("file_configuration")
      .as[FileConfigurationCaseClass].collect()
    val semanticLayerDs = Reader.readDf("semantic_layer")
      .as[models.SemanticLayerCaseClass]

    fileConfigs.find(_.file_name == fileName) match {
      case Some(fc) =>
        println(s"▶️ Validando $fileName (config id=${fc.id})")

        // 0️⃣ Lectura
        val dfOrError: Either[(String, Option[String]), DataFrame] =
          try {
            Right(
              Reader.readFile(filePath, Map(
                "header"      -> fc.has_header.toString,
                "sep"         -> fc.delimiter,
                "inferSchema" -> "false"
              )).cache()
            )
          } catch {
            case ex: Exception => Left(("30", Some(ex.getMessage)))
          }

        dfOrError match {
          case Left((flag, errMsgOpt)) =>
            logTrigger(fc.id, fc.file_name, None, env, flag, errMsgOpt, outputTable)

          case Right(df) =>
            // 1️⃣ FileSentinel
            val (fFlag, fOk, fErr, fField) =
              FileSentinel.verifyFiles(df, fc)
            if (!fOk) {
              logTrigger(fc.id, fc.file_name, fField, env, fFlag, fErr, outputTable)
            } else {
              // 2️⃣ TypeValidator
              val (tFlag, tOk, tErr, tField) =
                TypeValidator.verifyTyping(df, fc, semanticLayerDs)
              if (!tOk) {
                logTrigger(fc.id, fc.file_name, tField, env, tFlag, tErr, outputTable)
              } else {
                // 3️⃣ ReferentialIntegrityValidator
                val (rFlag, rOk, rErr, rField) =
                  ReferentialIntegrityValidator.verifyIntegrity(df, semanticLayerDs)
                if (!rOk) {
                  logTrigger(fc.id, fc.file_name, rField, env, rFlag, rErr, outputTable)
                } else {
                  // 4️⃣ FunctionalValidator
                  val (uFlag, uOk, uErr, uField) =
                    FunctionalValidator.verifyFunctional(df, fc)
                  if (!uOk) {
                    logTrigger(fc.id, fc.file_name, uField, env, uFlag, uErr, outputTable)
                  } else {
                    // ✅ Todo OK
                    logTrigger(fc.id, fc.file_name, None, env, "2", None, outputTable)
                  }
                }
              }
            }
        }

      case None =>
        println(s"⚠️ Sin configuración para $fileName, se omite.")
    }
  }


}
