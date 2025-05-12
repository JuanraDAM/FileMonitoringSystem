// src/main/scala/validators/TypeValidator.scala
package validators

import models.{FileConfigurationCaseClass, SemanticLayerCaseClass}
import org.apache.spark.sql.{DataFrame, Dataset}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._

/**
 * Objeto encargado de validar esquemas de datos según la capa semántica.
 * Todas las comprobaciones se realizan con operaciones de columna y filtros
 * directos para maximizar el rendimiento en Spark.
 */
object TypeValidator {

  /**
   * Valida tipos básicos (int, bigint, decimal, date, timestamp).
   * Imprime logs intermedios y devuelve true si no hay ningún valor inválido.
   *
   * @param df      DataFrame a validar
   * @param semList Lista de definiciones de campos de la capa semántica
   * @return        true si todos los valores cumplen su tipo
   */
  def validateBasicTypes(df: DataFrame, semList: Seq[SemanticLayerCaseClass]): Boolean = {
    println("▶️ Iniciando validación de tipos básicos…")
    val invalidCond = semList.map { sl =>
      val c = col(sl.field_name)
      // Para DECIMAL extraemos scale/precision de sl.length (e.g. "12,2")
      val casted = sl.data_type.toLowerCase match {
        case t if t.startsWith("int")     => c.cast(IntegerType)
        case t if t.startsWith("bigint")  => c.cast(LongType)
        case t if t.startsWith("decimal") =>
          val Array(p, s) = sl.length.getOrElse("38,18").split(",").map(_.toInt)
          c.cast(DecimalType(p, s))
        case "date"      => to_date(c, sl.decimal_symbol)       // 'yyyy-MM-dd'
        case "timestamp" => to_timestamp(c, sl.decimal_symbol)  // 'yyyy-MM-dd HH:mm:ss'
        case _           => c
      }
      c.isNotNull && casted.isNull
    }.reduce(_ or _)

    val found = df.filter(invalidCond).limit(1).count() > 0
    if (!found) println("✅ Tipos básicos OK") else println("❌ ¡Valores con tipo inválido detectados!")
    !found
  }

  /**
   * Valida columnas non-null según la capa semántica.
   * Imprime logs y devuelve true si no hay nulos indebidos.
   *
   * @param df      DataFrame a validar
   * @param semList Lista de definiciones de campos de la capa semántica
   * @return        true si no hay valores nulos en columnas non-null
   */
  def validateNullability(df: DataFrame, semList: Seq[SemanticLayerCaseClass]): Boolean = {
    println("▶️ Validando no nulos…")
    val cond = semList.filter(!_.nullable)
      .map(sl => col(sl.field_name).isNull)
      .reduceOption(_ or _).getOrElse(lit(false))

    val found = df.filter(cond).limit(1).count() > 0
    if (!found) println("✅ Nullability OK") else println("❌ ¡Nulos donde no debería haber!")
    !found
  }

  /**
   * Valida que los textos CHAR/VARCHAR no excedan la longitud máxima.
   * Imprime logs y devuelve true si todas las longitudes son correctas.
   *
   * @param df      DataFrame a validar
   * @param semList Lista de definiciones de campos de la capa semántica
   * @return        true si no hay textos demasiado largos
   */
  def validateLengths(df: DataFrame, semList: Seq[SemanticLayerCaseClass]): Boolean = {
    println("▶️ Validando longitudes de texto…")
    val cond = semList
      .filter(sl => {
        val t = sl.data_type.toLowerCase
        (t.startsWith("char") || t.startsWith("varchar")) &&
          sl.length.exists(_.forall(_.isDigit))
      })
      .flatMap { sl =>
        scala.util.Try(sl.length.get.toInt).toOption.map { maxLen =>
          length(col(sl.field_name)) > lit(maxLen)
        }
      }
      .reduceOption(_ or _).getOrElse(lit(false))

    val found = df.filter(cond).limit(1).count() > 0
    if (!found) println("✅ Longitudes OK") else println("❌ ¡Textos que exceden longitud máxima!")
    !found
  }

  /**
   * Valida formato de texto: que no contenga delimitador o comillas sin escape.
   * Imprime logs y devuelve true si el formato es correcto.
   *
   * @param df         DataFrame a validar
   * @param semList    Lista de definiciones de campos de la capa semántica
   * @param delimiter  Delimitador configurado
   * @param quoteChar  Carácter de cita configurado
   * @param escapeChar Carácter de escape configurado
   * @return           true si no hay formatos de texto inválidos
   */
  def validateTextFormat(
                          df: DataFrame,
                          semList: Seq[SemanticLayerCaseClass],
                          delimiter: String,
                          quoteChar: String,
                          escapeChar: String
                        ): Boolean = {
    println("▶️ Validando formato de texto…")
    val nonText = Seq("int","integer","bigint","decimal","date","timestamp")
    val cond = semList
      .filterNot(sl => nonText.exists(sl.data_type.toLowerCase.startsWith))
      .map { sl =>
        val c = col(sl.field_name)
        c.contains(delimiter) ||
          c.rlike(s"(?<!\\${escapeChar})\\${quoteChar}")
      }
      .reduceOption(_ or _).getOrElse(lit(false))

    val found = df.filter(cond).limit(1).count() > 0
    if (!found) println("✅ Formato de texto OK") else println("❌ ¡Texto con delimitador o comillas sin escape!")
    !found
  }

  /**
   * Ejecuta todas las validaciones de tipado en orden:
   *  1. Tipos básicos
   *  2. Nullability
   *  3. Longitudes
   *  4. Formato de texto
   * Imprime un resumen final y devuelve true si todas pasan.
   *
   * @param df         DataFrame a validar
   * @param fileConf   Configuración de fichero (delimitador, comillas, escape)
   * @param semDs      Dataset de la capa semántica
   * @return           true si todas las validaciones pasan
   */
  def verifyTyping(
                    df: DataFrame,
                    fileConf: FileConfigurationCaseClass,
                    semDs: Dataset[SemanticLayerCaseClass]
                  ): Boolean = {
    println("=== Iniciando verificación de tipado completa ===")
    val semList = semDs.collect().sortBy(_.field_position)

    val basicOk   = validateBasicTypes(df,     semList)
    val nullOk    = validateNullability(df,    semList)
    val lengthOk  = validateLengths(df,        semList)
    val textOk    = validateTextFormat(df,     semList,
      fileConf.delimiter,
      fileConf.quote_char,
      fileConf.escape_char)

    val allOk = Seq(basicOk, nullOk, lengthOk, textOk).forall(identity)
    println(s"=== Resultado FINAL de tipado: ${if (allOk) "✅ PASADO" else "❌ FALLÓ"} ===")
    allOk
  }
}
