package validators

import config.DbConfig
import utils.Reader
import models.FileConfigurationCaseClass
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions.{array, col, size}

/**
 * Objeto encargado de verificar la estructura de los archivos cargados contra la definición
 * de la capa semántica (semantic_layer).
 * Utiliza Reader para cargar desde la capa semántica y SparkSession implícito para operaciones.
 */
object FileSentinel {

  // DataFrame con la definición de la capa semántica (nombre y posición de campos esperados)
  private val semanticLayerDf = Reader.readDf("semantic_layer")

  /**
   * 1) Verifica que el DataFrame tiene tantas columnas como se definen en la capa semántica.
   *    Si el separador fuera incorrecto, df.columns.length == 1 y fallaría aquí.
   *
   * @param df DataFrame a verificar
   * @param spark SparkSession implícito para operaciones JDBC
   * @return true si el número de columnas coincide con la definición semántica
   */
  def verifyDelimiter(df: DataFrame)(implicit spark: SparkSession): Boolean = {
    println("Iniciando verificación de delimitador...")
    val expectedColsCount = semanticLayerDf.count().toInt
    val actualColsCount = df.columns.length
    val result = actualColsCount == expectedColsCount
    println(s"Esperado: $expectedColsCount columnas, Encontrado: $actualColsCount -> Resultado: $result")
    result
  }

  /**
   * 2) Verifica que los nombres de columna no contienen dígitos y coinciden,
   *    en orden y valor, con semantic_layer.field_name.
   *
   * @param df DataFrame a verificar
   * @param spark SparkSession implícito para operaciones JDBC y codificación
   * @return true si los encabezados coinciden exactamente con la definición semántica
   */
  def verifyHeader(df: DataFrame)(implicit spark: SparkSession): Boolean = {
    println("Iniciando verificación de encabezados...")
    import org.apache.spark.sql.Encoders
    val expected: Array[String] = semanticLayerDf
      .orderBy("field_position")
      .select("field_name")
      .as[String](Encoders.STRING)
      .collect()
    val actual: Array[String] = df.columns
    val result = actual.sameElements(expected)
    println(s"Encabezados esperados: ${expected.mkString(", ")}")
    println(s"Encabezados reales:   ${actual.mkString(", ")}")
    println(s"Resultado verificación de encabezados: $result")
    result
  }

  /**
   * 3) Verifica que todas las filas tienen exactamente el mismo número de columnas,
   *    usando sólo DataFrame API (sin pasar a RDD).
   *
   * @param df DataFrame a verificar
   * @return true si todas las filas tienen el mismo recuento de columnas
   */
  def verifyColumnCount(df: DataFrame): Boolean = {
    println("Iniciando verificación de conteo de columnas por fila...")
    val cntCol = size(array(df.columns.map(col): _*)).alias("cnt")
    val distinctCounts = df.select(cntCol).distinct().collect().map(_.getInt(0))
    val result = distinctCounts.length == 1
    println(s"Conteos distintos encontrados: ${distinctCounts.mkString(", ")} -> Resultado: $result")
    result
  }

  /**
   * Orquesta las verificaciones de delimitador, encabezado y conteo de columnas.
   *
   * @param df DataFrame a validar
   * @param fc Configuración de fichero (no utilizada en esta etapa)
   * @param spark SparkSession implícito para operaciones
   * @return true si todas las verificaciones pasan
   */
  def verifyFiles(df: DataFrame, fc: FileConfigurationCaseClass)(implicit spark: SparkSession): Boolean = {
    println("=== Iniciando orquestación de verificaciones de archivos ===")
    val delimiterOk = verifyDelimiter(df)
    val headerOk    = verifyHeader(df)
    val columnOk    = verifyColumnCount(df)
    val allValid    = Seq(delimiterOk, headerOk, columnOk).forall(identity)
    println(s"Resumen verificaciones -> Delimitador: $delimiterOk, Encabezados: $headerOk, Columnas: $columnOk")
    println(s"Resultado final de verifyFiles: $allValid")
    allValid
  }
}
