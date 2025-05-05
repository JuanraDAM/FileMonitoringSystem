package models

import java.util.Properties
import org.apache.spark.sql.{Dataset, SparkSession}
import org.apache.spark.sql.types.{BooleanType, CharType, StructField, StructType, StringType}

/**
 * Case class que representa la configuración de un fichero.
 */
case class FileConfigurationCaseClass(
                              id: Int,
                              file_format: String,
                              path: String,
                              file_name: String,
                              has_header: Boolean,
                              delimiter: String,
                              quote_char: String,
                              escape_char: String,
                              date_format: String,
                              timestamp_format: String,
                              partition_columns: Option[String]
                            )

object FileConfiguration {
  /**
   * Schema Spark (StructType) mapeado a la tabla Postgres file_configuration.
   */
  private val schema: StructType = StructType(Seq(
    StructField("id",           org.apache.spark.sql.types.IntegerType, nullable = false),
    StructField("file_format",  StringType,                                   nullable = false),
    StructField("path",         StringType,                                   nullable = false),
    StructField("file_name",    StringType,                                   nullable = false),
    StructField("has_header",   BooleanType,                                  nullable = false),
    StructField("delimiter",    StringType,                                   nullable = false),
    StructField("quote_char",   StringType,                                   nullable = false),
    StructField("escape_char",  StringType,                                   nullable = false),
    StructField("date_format",  StringType,                                   nullable = true),
    StructField("timestamp_format", StringType,                              nullable = true),
    StructField("partition_columns", StringType,                             nullable = true)
  ))

  /**
   * Devuelve el schema definido para file_configuration.
   */
  def getSchema(): StructType = schema

  /**
   * Lee la tabla file_configuration desde JDBC y la convierte en Dataset[FileConfiguration].
   * @param spark   SparkSession activo
   * @param jdbcUrl URL JDBC a la base de datos
   * @param props   Properties con user, password, driver, etc.
   */
  def read(spark: SparkSession, jdbcUrl: String, props: Properties): Dataset[FileConfigurationCaseClass] = {
    import spark.implicits._
    spark.read
      .schema(schema)
      .jdbc(jdbcUrl, "file_configuration", props)
      .as[FileConfigurationCaseClass]
  }
}
