package models

import java.util.Properties
import org.apache.spark.sql.{Dataset, SparkSession}
import org.apache.spark.sql.types._

/**
 * Case class que representa la configuración de campos de la capa semántica.
 *
 * @param id                Identificador único del campo.
 * @param field_position    Posición del campo en el fichero.
 * @param field_name        Nombre del campo.
 * @param field_description Descripción del campo, opcional.
 * @param pk                Indicador de clave primaria.
 * @param data_type         Tipo de datos (e.g., "string", "int").
 * @param length            Longitud máxima o formato, opcional.
 * @param nullable          Indica si el campo admite valores nulos.
 * @param decimal_symbol    Símbolo decimal usado en valores numéricos.
 */
case class SemanticLayerCaseClass(
                                   id: Int,
                                   field_position: Int,
                                   field_name: String,
                                   field_description: Option[String],
                                   pk: Boolean,
                                   data_type: String,
                                   length: Option[String],
                                   nullable: Boolean,
                                   decimal_symbol: String
                                 )

/**
 * Objeto que define el esquema y permite la lectura de la tabla `semantic_layer`.
 */
object SemanticLayer {
  /**
   * Esquema Spark que refleja la estructura de la tabla `semantic_layer`.
   */
  private val schema: StructType = StructType(Seq(
    StructField("id", IntegerType, nullable = false),
    StructField("field_position", IntegerType, nullable = false),
    StructField("field_name", StringType, nullable = false),
    StructField("field_description", StringType, nullable = true),
    StructField("pk", BooleanType, nullable = false),
    StructField("data_type", StringType, nullable = false),
    StructField("length", StringType, nullable = true),
    StructField("nullable", BooleanType, nullable = false),
    StructField("decimal_symbol", StringType, nullable = false)
  ))

  /**
   * Obtiene el esquema definido para `semantic_layer`.
   * @return StructType con la definición de columnas.
   */
  def getSchema: StructType = schema

  /**
   * Lee la tabla `semantic_layer` vía JDBC y devuelve un Dataset de SemanticLayerCaseClass.
   *
   * @param spark   SparkSession activo.
   * @param jdbcUrl URL JDBC de la base de datos.
   * @param props   Properties con credenciales y configuración de JDBC.
   * @return Dataset[SemanticLayerCaseClass] con los registros de la capa semántica.
   */
  def read(
            spark: SparkSession,
            jdbcUrl: String,
            props: Properties
          ): Dataset[SemanticLayerCaseClass] = {
    import spark.implicits._
    spark.read
      .schema(schema)
      .jdbc(jdbcUrl, "semantic_layer", props)
      .as[SemanticLayerCaseClass]
  }
}
