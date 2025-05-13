// src/main/scala/services/ExecutionManager.scala
package services

import java.util.Properties
import config.{DbConfig, SparkSessionProvider}
import models.FileConfigurationCaseClass
import org.apache.spark.sql.SparkSession
import utils.Reader
import validators.{FileSentinel, TypeValidator, ReferentialIntegrityValidator, FunctionalValidator}

object ExecutionManager {

  /** Inserta un registro en trigger_control vía JDBC */
  private def logTrigger(
                          fileConfigId: Int,
                          fileName: String,
                          fieldName: Option[String],
                          environment: String,
                          validationFlag: String,
                          errorMessage: Option[String]
                        )(implicit spark: SparkSession): Unit = {
    import spark.implicits._
    val row = (fileConfigId, fileName, fieldName.orNull, environment, validationFlag, errorMessage.orNull)
    val df  = Seq(row).toDF(
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
      .jdbc(DbConfig.getJdbcUrl, "trigger_control", props)
  }

  def executeEngine(): Unit = {
    implicit val spark = SparkSessionProvider.getSparkSession
    import spark.implicits._

    val filesToVerify   = Reader.readDf("file_configuration")
      .as[FileConfigurationCaseClass].collect()
    val semanticLayerDs = Reader.readDf("semantic_layer")
      .as[models.SemanticLayerCaseClass]

    val env = sys.env.getOrElse("ENV", "dev")

    filesToVerify.foreach { fc =>
      val filePath = fc.path + fc.file_name

      // 0️⃣ Lectura
      val dfOrError = try {
        Right(Reader.readFile(filePath, Map(
          "header"      -> fc.has_header.toString,
          "sep"         -> fc.delimiter,
          "inferSchema" -> "false"
        )).cache())
      } catch {
        case ex: Exception => Left(("30", Some(ex.getMessage), None))
      }

      dfOrError match {
        case Left((flag, errMsg, _)) =>
          logTrigger(fc.id, fc.file_name, None, env, flag, errMsg)

        case Right(df) =>
          // 1️⃣ Files
          val (fFlag, fOk, fErr, fField) = FileSentinel.verifyFiles(df, fc)
          if (!fOk) {
            logTrigger(fc.id, fc.file_name, fField, env, fFlag, fErr)
          } else {
            // 2️⃣ Typing
            val (tFlag, tOk, tErr, tField) = TypeValidator.verifyTyping(df, fc, semanticLayerDs)
            if (!tOk) {
              logTrigger(fc.id, fc.file_name, tField, env, tFlag, tErr)
            } else {
              // 3️⃣ Referential
              val (rFlag, rOk, rErr, rField) = ReferentialIntegrityValidator.verifyIntegrity(df, semanticLayerDs)
              if (!rOk) {
                logTrigger(fc.id, fc.file_name, rField, env, rFlag, rErr)
              } else {
                // 4️⃣ Functional
                val (uFlag, uOk, uErr, uField) = FunctionalValidator.verifyFunctional(df, fc)
                if (!uOk) {
                  logTrigger(fc.id, fc.file_name, uField, env, uFlag, uErr)
                } else {
                  // ✅ Todo OK
                  logTrigger(fc.id, fc.file_name, None, env, "2", None)
                }
              }
            }
          }
      }
    }
  }
}
