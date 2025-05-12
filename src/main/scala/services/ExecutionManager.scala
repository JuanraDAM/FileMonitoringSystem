package services

import validators.FileSentinel.verifyFiles
import utils.Reader
import models.{FileConfigurationCaseClass, SemanticLayerCaseClass}
import config.{DbConfig, SparkSessionProvider}
import validators.{FunctionalValidator, ReferentialIntegrityValidator, TypeValidator}

object ExecutionManager {


  def executeEngine(): Unit = {
    println("Ejecutando validaciones")

    // Obtener SparkSession e importar implicits para usar .as
    val spark = SparkSessionProvider.getSparkSession
    import spark.implicits._

    // Leer configuración de archivos como Dataset[FileConfiguration]
    val filesToVerify = Reader.readDf("file_configuration").as[FileConfigurationCaseClass]
    val semanticLayerDs = Reader.readDf("semantic_layer").as[SemanticLayerCaseClass]

    val result: Boolean = filesToVerify.collect().forall(fc => {
      val props = Map(
        "header"      -> fc.has_header.toString,
        "sep"         -> fc.delimiter,
        "inferSchema" -> "false"    // <- antes era "true"
      )
      val path = fc.path + fc.file_name
      val dataDf = Reader.readFile(path, props)
      dataDf.cache()
      val rowCount = dataDf.count()

      val filesValidate = verifyFiles(dataDf, fc)(spark)
      val typeValidate = TypeValidator.verifyTyping(dataDf, fc, semanticLayerDs)
      val referentialIntegrity = ReferentialIntegrityValidator.verifyIntegrity(dataDf, semanticLayerDs)
      val funcionalValidate = FunctionalValidator.verifyFunctional(dataDf, fc)
      filesValidate && typeValidate && referentialIntegrity && funcionalValidate
    })

    if (result) println("validaciones pasadas para todos")

  }
}
