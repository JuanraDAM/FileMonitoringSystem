// src/main/scala/validators/FileSentinel.scala
package validators

import utils.Reader
import models.FileConfigurationCaseClass
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions._

object FileSentinel {
  private val semanticLayerDf = Reader.readDf("semantic_layer")

  def verifyDelimiter(df: DataFrame)(implicit spark: SparkSession): Boolean = {
    println("Iniciando verificación de delimitador...")
    val exp = semanticLayerDf.count().toInt
    val act = df.columns.length
    val ok  = act == exp
    println(s"Esperado: $exp columnas, Encontrado: $act -> $ok")
    ok
  }

  def verifyHeader(df: DataFrame)(implicit spark: SparkSession): Boolean = {
    println("Iniciando verificación de encabezados...")
    import org.apache.spark.sql.Encoders
    val exp = semanticLayerDf.orderBy("field_position")
      .select("field_name").as[String](Encoders.STRING).collect()
    val ok  = df.columns.sameElements(exp)
    println(s"Esperado: ${exp.mkString(", ")}, Real: ${df.columns.mkString(", ")} -> $ok")
    ok
  }

  def verifyColumnCount(df: DataFrame): Boolean = {
    println("Iniciando verificación de conteo de columnas por fila...")
    val cnt = size(array(df.columns.map(col): _*)).alias("c")
    val distinct = df.select(cnt).distinct().collect().map(_.getInt(0))
    val ok = distinct.length == 1
    println(s"Conteos distintos: ${distinct.mkString(", ")} -> $ok")
    ok
  }

  def verifyFiles(
                   df: DataFrame,
                   fc: FileConfigurationCaseClass
                 )(implicit spark: SparkSession): (String, Boolean, Option[String], Option[String]) = {
    println("=== Orquestación verificaciones de archivos ===")

    if (!verifyDelimiter(df))
      return ("32", false, Some("Delimiter mismatch"), None)

    if (!verifyHeader(df))
      return ("33", false, Some("Header mismatch"), None)

    if (!verifyColumnCount(df))
      return ("34", false, Some("Column count per row mismatch"), None)

    ("1.13", true, None, None)
  }
}
