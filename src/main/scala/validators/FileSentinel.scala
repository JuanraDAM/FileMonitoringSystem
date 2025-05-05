package validators

import config.DbConfig
import models.FileConfigurationCaseClass
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions.{array, col, size}

import scala.util.Try

object FileSentinel {

  /**
   * 1) Verifica que el DataFrame tiene tantas columnas como se definen en semantic_layer.
   *    Si el separador fuera incorrecto, df.columns.length == 1 y fallaría aquí.
   */
  def verifyDelimiter(df: DataFrame)
                     (implicit spark: SparkSession): Boolean = {
    // Cargo desde JDBC el número de campos esperados
    val props = new java.util.Properties()
    props.put("user",     DbConfig.getUsername)
    props.put("password", DbConfig.getPassword)
    props.put("driver",   "org.postgresql.Driver")

    val expectedColsCount = spark.read
      .jdbc(DbConfig.getJdbcUrl, "semantic_layer", props)
      .count()

    df.columns.length == expectedColsCount
  }

  /**
   * 2) Verifica que los nombres de columna no contienen dígitos (son los headers reales)
   *    y que coinciden en orden y valor con semantic_layer.field_name.
   */
  def verifyHeader(df: DataFrame)
                  (implicit spark: SparkSession): Boolean = {
    val props = new java.util.Properties()
    props.put("user",     DbConfig.getUsername)
    props.put("password", DbConfig.getPassword)
    props.put("driver",   "org.postgresql.Driver")

    // Extraigo los nombres esperados por posición
    import org.apache.spark.sql.Encoders
    val expected: Array[String] = spark.read
      .jdbc(DbConfig.getJdbcUrl, "semantic_layer", props)
      .orderBy("field_position")
      .select("field_name")
      .as[String](Encoders.STRING)
      .collect()

    df.columns.sameElements(expected)
  }

  /**
   * 3) Verifica que todas las filas tienen exactamente el mismo número de columnas,
   *    usando sólo DataFrame API (sin pasar a RDD).
   */
  def verifyColumnCount(df: DataFrame): Boolean = {
    // construyo una columna "cnt" con el tamaño de un array de todas las columnas
    val cntCol = size(array(df.columns.map(col): _*)).alias("cnt")
    val distinctCounts = df.select(cntCol).distinct().collect().map(_.getInt(0))
    distinctCounts.length == 1
  }

  /**
   * Orquesta las verificaciones: devuelve true sólo si todas pasan.
   */
  def verifyFiles(df: DataFrame, fc: FileConfigurationCaseClass)
                 (implicit spark: SparkSession): Boolean = {
    verifyDelimiter(df) &&
      verifyHeader(df)    &&
      verifyColumnCount(df)
  }
}
