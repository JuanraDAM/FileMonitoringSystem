package validators

import models.SemanticLayerCaseClass
import org.apache.spark.sql.{DataFrame, Dataset}
import org.apache.spark.sql.functions._

object ReferentialIntegrityValidator {
  /**
   * Comprueba que las combinaciones de valores PK son únicas en el DataFrame.
   * @param df DataFrame con los datos a validar
   * @param pkFields Lista de nombres de columnas marcadas como PK
   * @return true si no existen duplicados en la concatenación de PKs
   */
  def verifyPrimaryKeyUnique(df: DataFrame, pkFields: Seq[String]): Boolean = {
    // Si no hay campos PK, se considera válido
    if (pkFields.isEmpty) true
    else {
      // Concatenamos valores de PK con un separador único
      val pkConcat = concat_ws("§", pkFields.map(col): _*)
      // Contamos grupos repetidos
      val duplicateCount = df
        .select(pkConcat.alias("pk_concat"))
        .groupBy("pk_concat")
        .count()
        .filter(col("count") > lit(1))
        .limit(1)
        .count()
      duplicateCount == 0
    }
  }

  /**
   * Validación completa de integridad referencial usando semantic layer para extraer campos PK.
   * @param df    DataFrame con los datos a validar
   * @param semDs Dataset de SemanticLayerCaseClass con la definición de campos
   * @return true si la clave primaria es única según la semántica
   */
  def verifyIntegrity(df: DataFrame, semDs: Dataset[SemanticLayerCaseClass]): Boolean = {
    // Traemos a driver la semántica (es pequeña)
    val semList = semDs.collect()
    // Extraemos los nombres de campos PK ordenados
    val pkFields = semList
      .filter(_.pk)
      .sortBy(_.field_position)
      .map(_.field_name)
    // Aplicamos la verificación de unicidad de PK
    verifyPrimaryKeyUnique(df, pkFields)
  }
}
