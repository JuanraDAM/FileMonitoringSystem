// src/main/scala/validators/ReferentialIntegrityValidator.scala
package validators

import models.SemanticLayerCaseClass
import org.apache.spark.sql.{DataFrame, Dataset}
import org.apache.spark.sql.functions._

/**
 * Objeto encargado de validar la integridad referencial de un DataFrame,
 * comprobando la unicidad de las claves primarias definidas en la capa semántica.
 */
object ReferentialIntegrityValidator {

  /**
   * Comprueba que las combinaciones de valores de las columnas PK sean únicas.
   * Imprime logs intermedios y devuelve true si no se encuentran duplicados.
   *
   * @param df       DataFrame con los datos a validar.
   * @param pkFields Secuencia de nombres de columnas marcadas como PK.
   * @return         true si no hay duplicados en la concatenación de PKs.
   */
  def verifyPrimaryKeyUnique(df: DataFrame, pkFields: Seq[String]): Boolean = {
    println("▶️ Verificando unicidad de clave primaria...")
    if (pkFields.isEmpty) {
      println("ℹ️ No hay campos PK definidos; se considera válida la integridad referencial.")
      true
    } else {
      println(s"ℹ️ Campos PK a comprobar: ${pkFields.mkString(", ")}")
      // Concatenamos valores de PK con un separador inusual para evitar colisiones
      val pkConcat = concat_ws("§", pkFields.map(col): _*).alias("pk_concat")
      // Agrupamos por esa concatenación y contamos las repeticiones
      val duplicateCount = df
        .select(pkConcat)
        .groupBy("pk_concat")
        .count()
        .filter(col("count") > lit(1))
        .limit(1)
        .count()

      if (duplicateCount == 0) {
        println("✅ Todas las claves primarias son únicas.")
      } else {
        println(s"❌ Duplicado encontrado en clave primaria (al menos $duplicateCount veces).")
      }
      duplicateCount == 0
    }
  }

  /**
   * Valida la integridad referencial completa usando la capa semántica para extraer campos PK.
   * Imprime logs de inicio y fin, así como el resultado de la comprobación PK.
   *
   * @param df    DataFrame a validar.
   * @param semDs Dataset de SemanticLayerCaseClass con la definición de campos.
   * @return      true si la clave primaria es única según la semántica.
   */
  def verifyIntegrity(df: DataFrame, semDs: Dataset[SemanticLayerCaseClass]): Boolean = {
    println("=== Iniciando validación de integridad referencial ===")
    // Traemos la semántica a driver (pequeña)
    val semList = semDs.collect().sortBy(_.field_position)
    // Extraemos nombres de campos PK
    val pkFields = semList.filter(_.pk).map(_.field_name)

    val result = verifyPrimaryKeyUnique(df, pkFields)
    println(s"=== Resultado FINAL de integridad referencial: ${if (result) "✅ PASADO" else "❌ FALLÓ"} ===")
    result
  }
}
