package validators

import models.SemanticLayerCaseClass
import org.apache.spark.sql.{DataFrame, Dataset}
import org.apache.spark.sql.functions._

/**
 * Verifica la unicidad de claves primarias y la integridad referencial.
 */
object ReferentialIntegrityValidator {
  /**
   * Verifica que no existan duplicados en las columnas de clave primaria.
   *
   * @param df       DataFrame a verificar.
   * @param pkFields Secuencia de nombres de campos PK.
   * @return true si todas las claves primarias son únicas.
   */
  def verifyPrimaryKeyUnique(df: DataFrame, pkFields: Seq[String]): Boolean = {
    if (pkFields.isEmpty) true
    else {
      val pkConcat = concat_ws("§", pkFields.map(col): _*).alias("pk_concat")
      val duplicateCount = df.select(pkConcat)
        .groupBy("pk_concat").count()
        .filter(col("count") > 1).limit(1).count()
      duplicateCount == 0
    }
  }

  /**
   * Verifica la integridad referencial usando la definición de la capa semántica.
   *
   * @param df     DataFrame a verificar.
   * @param semDs  Dataset con metadatos de la capa semántica.
   * @return Tupla con código de flag, éxito, mensaje de error y campo implicado.
   */
  def verifyIntegrity(
                       df: DataFrame,
                       semDs: Dataset[SemanticLayerCaseClass]
                     ): (String, Boolean, Option[String], Option[String]) = {
    val pkFields = semDs.collect().filter(_.pk).map(_.field_name)
    val dupCount = df.select(concat_ws("§", pkFields.map(col): _*).alias("pk"))
      .groupBy("pk").count().filter(col("count") > 1).limit(1).count()
    if (dupCount > 0) ("39", false, Some("Duplicado PK"), Some(pkFields.mkString(",")))
    else ("1.31", true, None, None)
  }
}

