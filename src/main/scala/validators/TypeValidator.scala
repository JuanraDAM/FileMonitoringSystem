package validators

import models.{FileConfigurationCaseClass, SemanticLayerCaseClass}
import org.apache.spark.sql.{Column, DataFrame, Dataset}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._

/**
 * Valida tipos, nullabilidad, longitudes y formato de texto según la capa semántica,
 * pero ahora aprovechando los formatos de fecha y timestamp provenientes de FileConfigurationCaseClass.
 */
object TypeValidator {
  /**
   * Ejecuta una validación completa de tipado en cuatro pasos:
   *  1. Tipos básicos
   *  2. Nullabilidad
   *  3. Longitudes
   *  4. Formato de texto
   *
   * @param df     DataFrame a validar.
   * @param fc     Configuración del fichero (incluye date_format, timestamp_format, delimiter, escape_char, quote_char, etc.).
   * @param semDs  Dataset con metadatos de la capa semántica.
   * @return Tupla con código de flag, éxito, mensaje de error y campo implicado.
   */
  def verifyTyping(
                    df: DataFrame,
                    fc: FileConfigurationCaseClass,
                    semDs: Dataset[SemanticLayerCaseClass]
                  ): (String, Boolean, Option[String], Option[String]) = {
    // Primero, recogemos la lista de metadatos ordenada por posición
    val semList = semDs.collect().sortBy(_.field_position)

    // 1. Validación de tipos básicos (INT, BIGINT, DECIMAL, DATE, TIMESTAMP)
    semList.foreach { sl =>
      val c: Column = col(sl.field_name)
      val casted: Column = sl.data_type.toLowerCase match {
        case t if t.startsWith("int") =>
          c.cast(IntegerType)
        case t if t.startsWith("bigint") =>
          c.cast(LongType)
        case t if t.startsWith("decimal") =>
          // El formato de longitud viene en "precision,scale", p.ej. "8,2"
          val Array(p, s) = sl.length.get.split(",").map(_.toInt)
          c.cast(DecimalType(p, s))
        case "date" =>
          // Usamos el date_format definido en FileConfigurationCaseClass
          to_date(c, fc.date_format)
        case "timestamp" =>
          // Usamos el timestamp_format definido en FileConfigurationCaseClass
          to_timestamp(c, fc.timestamp_format)
        case _ =>
          // Tipos que no requieran conversión: char, varchar, boolean, etc.
          c
      }

      // Si existe alguna fila donde el valor original no es null, pero al castear es null → tipo inválido
      val invalidTypeFound =
        df
          .filter(c.isNotNull && casted.isNull)
          .limit(1)
          .count() > 0

      if (invalidTypeFound) {
        return ("35", false, Some("Tipo inválido"), Some(sl.field_name))
      }
    }

    // 2. Validación de nullabilidad: si el campo no es nullable, no debe haber nulls
    semList.filterNot(_.nullable).foreach { sl =>
      val hasUnexpectedNull =
        df
          .filter(col(sl.field_name).isNull)
          .limit(1)
          .count() > 0

      if (hasUnexpectedNull) {
        return ("36", false, Some("Nulo indebido"), Some(sl.field_name))
      }
    }

    // 3. Validación de longitudes en campos CHAR/VARCHAR que tengan longitud definida
    semList
      .filter { sl =>
        val dt = sl.data_type.toLowerCase
        (dt.startsWith("char") || dt.startsWith("varchar")) &&
          sl.length.exists(_.forall(_.isDigit))
      }
      .foreach { sl =>
        val maxLen = sl.length.get.toInt
        val tooLongFound =
          df
            .filter(length(col(sl.field_name)) > maxLen)
            .limit(1)
            .count() > 0

        if (tooLongFound) {
          return ("37", false, Some("Longitud excedida"), Some(sl.field_name))
        }
      }

    // 4. Validación de formato de texto: que los strings no contengan delimitador ni comillas sin escapar
    semList
      .filterNot { sl =>
        Seq("int", "bigint", "decimal", "date", "timestamp")
          .exists(prefix => sl.data_type.toLowerCase.startsWith(prefix))
      }
      .foreach { sl =>
        val c: Column = col(sl.field_name)
        val containsDelimiter = c.contains(fc.delimiter)
        // Regex para detectar ocurrencia de quote_char no escapada:
        //   (?<!\)\"   ← si quote_char es '"', y fc.escape_char es '\', se busca: (?<!\\)\"
        val badUnescapedQuote = c.rlike(s"""(?<!\\${fc.escape_char})\\${fc.quote_char}""")

        val invalidTextFound =
          df
            .filter(containsDelimiter || badUnescapedQuote)
            .limit(1)
            .count() > 0

        if (invalidTextFound) {
          return ("38", false, Some("Formato texto inválido"), Some(sl.field_name))
        }
      }

    // Si pasa todos los chequeos, devolvemos éxito
    ("1.21", true, None, None)
  }
}
