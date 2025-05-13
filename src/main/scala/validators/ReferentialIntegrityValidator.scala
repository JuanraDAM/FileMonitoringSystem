// src/main/scala/validators/ReferentialIntegrityValidator.scala
package validators

import models.SemanticLayerCaseClass
import org.apache.spark.sql.{DataFrame, Dataset}
import org.apache.spark.sql.functions._

object ReferentialIntegrityValidator {

  def verifyPrimaryKeyUnique(df: DataFrame, pkFields: Seq[String]): Boolean = {
    println("▶️ Verificando unicidad de clave primaria...")
    if (pkFields.isEmpty) {
      println("ℹ️ No hay campos PK definidos; integridad referencial considerada OK.")
      true
    } else {
      println(s"ℹ️ Campos PK a comprobar: ${pkFields.mkString(", ")}")
      val pkConcat = concat_ws("§", pkFields.map(col): _*).alias("pk_concat")
      val duplicateCount = df
        .select(pkConcat)
        .groupBy("pk_concat")
        .count()
        .filter(col("count") > 1)
        .limit(1)
        .count()
      if (duplicateCount == 0) println("✅ Todas las claves primarias son únicas.")
      else println(s"❌ Duplicado encontrado en PK (al menos $duplicateCount veces).")
      duplicateCount == 0
    }
  }

  def verifyIntegrity(
                       df: DataFrame,
                       semDs: Dataset[SemanticLayerCaseClass]
                     ): (String, Boolean, Option[String], Option[String]) = {
    println("=== Integridad referencial ===")
    val pkFields = semDs.collect().filter(_.pk).map(_.field_name)
    // detect duplicate
    val dupCount = df
      .select(concat_ws("§", pkFields.map(col): _*).alias("pk"))
      .groupBy("pk").count().filter(col("count") > 1).limit(1).count()
    if (dupCount > 0)
      ("39", false, Some("Duplicado PK"), Some(pkFields.mkString(",")))
    else {
      println("Integridad referencial OK")
      ("1.31", true, None, None)
    }
  }
}
