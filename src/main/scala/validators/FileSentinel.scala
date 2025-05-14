package validators

import utils.Reader
import models.FileConfigurationCaseClass
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions._

/**
 * Valida la estructura y consistencia básica de un DataFrame según su configuración.
 */
object FileSentinel {
  private val semanticLayerDf = Reader.readDf("semantic_layer")

  /**
   * Verifica que el número de columnas coincida con la capa semántica.
   *
   * @param df DataFrame a verificar.
   * @return true si el conteo coincide.
   */
  def verifyDelimiter(df: DataFrame)(implicit spark: SparkSession): Boolean = {
    val expected = semanticLayerDf.count().toInt
    val actual = df.columns.length
    expected == actual
  }

  /**
   * Verifica que las cabeceras coincidan en orden y contenido.
   *
   * @param df DataFrame a verificar.
   * @return true si los encabezados coinciden.
   */
  def verifyHeader(df: DataFrame)(implicit spark: SparkSession): Boolean = {
    import spark.implicits._
    val expected = semanticLayerDf.orderBy("field_position")
      .select("field_name").as[String].collect()
    df.columns.sameElements(expected)
  }

  /**
   * Verifica que todas las filas tengan el mismo número de columnas.
   *
   * @param df DataFrame a verificar.
   * @return true si todas las filas tienen igual conteo.
   */
  def verifyColumnCount(df: DataFrame): Boolean = {
    val countCol = size(array(df.columns.map(col): _*)).alias("c")
    val distinctCounts = df.select(countCol).distinct().collect().map(_.getInt(0))
    distinctCounts.length == 1
  }

  /**
   * Orquesta las verificaciones de FileSentinel en orden.
   *
   * @param df DataFrame a validar.
   * @param fc Configuración de fichero asociada.
   * @return Tuple con código de flag, éxito, mensaje de error y campo.
   */
  def verifyFiles(
                   df: DataFrame,
                   fc: FileConfigurationCaseClass
                 )(
                   implicit spark: SparkSession
                 ): (String, Boolean, Option[String], Option[String]) = {
    if (!verifyDelimiter(df)) return ("32", false, Some("Delimiter mismatch"), None)
    if (!verifyHeader(df))   return ("33", false, Some("Header mismatch"), None)
    if (!verifyColumnCount(df)) return ("34", false, Some("Column count per row mismatch"), None)
    ("1.13", true, None, None)
  }
}
