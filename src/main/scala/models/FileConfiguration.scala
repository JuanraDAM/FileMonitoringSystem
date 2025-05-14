package models

import java.util.Properties
import org.apache.spark.sql.{Dataset, SparkSession}
import org.apache.spark.sql.types._

/**
 * Case class que representa la configuración de un fichero a validar.
 *
 * @param id                  Identificador único de la configuración.
 * @param file_format         Formato del fichero (e.g., "csv").
 * @param path                Ruta base donde reside el fichero.
 * @param file_name           Nombre del fichero.
 * @param has_header          Indica si el fichero incluye cabecera.
 * @param delimiter           Delimitador de campos en el fichero.
 * @param quote_char          Carácter utilizado como comilla.
 * @param escape_char         Carácter de escape para comillas.
 * @param date_format         Formato de fecha esperado, si aplica.
 * @param timestamp_format    Formato de timestamp esperado, si aplica.
 * @param partition_columns   Columnas de partición separadas por comas, opcional.
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

/**
 * Objeto que define el esquema y permite la lectura de la tabla `file_configuration`.
 */
object FileConfiguration {
  /**
   * Esquema Spark que mapea la estructura de la tabla `file_configuration` en la base de datos.
   */
  private val schema: StructType = StructType(Seq(
    StructField("id", IntegerType, nullable = false),
    StructField("file_format", StringType, nullable = false),
    StructField("path", StringType, nullable = false),
    StructField("file_name", StringType, nullable = false),
    StructField("has_header", BooleanType, nullable = false),
    StructField("delimiter", StringType, nullable = false),
    StructField("quote_char", StringType, nullable = false),
    StructField("escape_char", StringType, nullable = false),
    StructField("date_format", StringType, nullable = true),
    StructField("timestamp_format", StringType, nullable = true),
    StructField("partition_columns", StringType, nullable = true)
  ))

  /**
   * Obtiene el esquema definido para `file_configuration`.
   * @return StructType con la definición de columnas.
   */
  def getSchema(): StructType = schema

  /**
   * Lee la tabla `file_configuration` vía JDBC y devuelve un Dataset de FileConfigurationCaseClass.
   *
   * @param spark   SparkSession activo.
   * @param jdbcUrl URL JDBC de la base de datos.
   * @param props   Properties con credenciales y configuración de JDBC.
   * @return Dataset[FileConfigurationCaseClass] con los registros de configuración.
   */
  def read(
            spark: SparkSession,
            jdbcUrl: String,
            props: Properties
          ): Dataset[FileConfigurationCaseClass] = {
    import spark.implicits._
    spark.read
      .schema(schema)
      .jdbc(jdbcUrl, "file_configuration", props)
      .as[FileConfigurationCaseClass]
  }
}