// src/main/scala/models/SemanticLayerCaseClass.scala
package models

import java.util.Properties
import org.apache.spark.sql.{Dataset, SparkSession}
import org.apache.spark.sql.types._
import org.apache.spark.sql.functions._

/**
 * Case class que representa la configuración de un campo en la capa semántica.
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

object SemanticLayer {
  /** Esquema Spark asociado a la tabla `semantic_layer`. */
  private val schema: StructType = StructType(Seq(
    StructField("id",               IntegerType, nullable = false),
    StructField("field_position",   IntegerType, nullable = false),
    StructField("field_name",       StringType,  nullable = false),
    StructField("field_description",StringType,  nullable = true),
    StructField("pk",               BooleanType, nullable = false),
    StructField("data_type",        StringType,  nullable = false),
    StructField("length",           StringType,  nullable = true),
    StructField("nullable",         BooleanType, nullable = false),
    StructField("decimal_symbol",   StringType,  nullable = false)
  ))

  /** Devuelve el esquema definido para `semantic_layer`. */
  def getSchema: StructType = schema

  /**
   * Lee la tabla `semantic_layer` vía JDBC y la convierte en Dataset[SemanticLayerCaseClass].
   *
   * @param spark   SparkSession activo
   * @param jdbcUrl URL JDBC a la base de datos
   * @param props   Properties con user, password, driver, etc.
   */
  def read(spark: SparkSession, jdbcUrl: String, props: Properties): Dataset[SemanticLayerCaseClass] = {
    import spark.implicits._
    spark.read
      .schema(schema)
      .jdbc(jdbcUrl, "semantic_layer", props)
      .as[SemanticLayerCaseClass]
  }
}
