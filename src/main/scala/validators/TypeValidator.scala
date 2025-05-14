package validators

import models.{FileConfigurationCaseClass, SemanticLayerCaseClass}
import org.apache.spark.sql.{DataFrame, Dataset}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._

/**
 * Valida tipos, nullabilidad, longitudes y formato de texto según la capa semántica.
 */
object TypeValidator {
  /**
   * Ejecuta una validación completa de tipado en cuatro pasos:
   * 1. Tipos básicos
   * 2. Nullabilidad
   * 3. Longitudes
   * 4. Formato de texto
   *
   * @param df     DataFrame a validar.
   * @param fc     Configuración del fichero.
   * @param semDs  Dataset con metadatos de la capa semántica.
   * @return Tupla con código de flag, éxito, mensaje de error y campo implicado.
   */
  def verifyTyping(
                    df: DataFrame,
                    fc: FileConfigurationCaseClass,
                    semDs: Dataset[SemanticLayerCaseClass]
                  ): (String, Boolean, Option[String], Option[String]) = {
    val semList = semDs.collect().sortBy(_.field_position)

    semList.foreach { sl =>
      val c = col(sl.field_name)
      val casted = sl.data_type.toLowerCase match {
        case t if t.startsWith("int")     => c.cast(IntegerType)
        case t if t.startsWith("bigint")  => c.cast(LongType)
        case t if t.startsWith("decimal") =>
          val Array(p, s) = sl.length.get.split(",").map(_.toInt)
          c.cast(DecimalType(p, s))
        case "date"      => to_date(c, sl.decimal_symbol)
        case "timestamp" => to_timestamp(c, sl.decimal_symbol)
        case _             => c
      }
      if (df.filter(c.isNotNull && casted.isNull).limit(1).count() > 0)
        return ("35", false, Some("Tipo inválido"), Some(sl.field_name))
    }

    semList.filterNot(_.nullable).foreach { sl =>
      if (df.filter(col(sl.field_name).isNull).limit(1).count() > 0)
        return ("36", false, Some("Nulo indebido"), Some(sl.field_name))
    }

    semList.filter(sl => {
      val dt = sl.data_type.toLowerCase
      (dt.startsWith("char") || dt.startsWith("varchar")) &&
        sl.length.exists(_.forall(_.isDigit))
    }).foreach { sl =>
      val max = sl.length.get.toInt
      if (df.filter(length(col(sl.field_name)) > max).limit(1).count() > 0)
        return ("37", false, Some("Longitud excedida"), Some(sl.field_name))
    }

    semList.filterNot(sl => Seq("int", "bigint", "decimal", "date", "timestamp")
      .exists(sl.data_type.toLowerCase.startsWith)).foreach { sl =>
      val c = col(sl.field_name)
      if (df.filter(c.contains(fc.delimiter) ||
          c.rlike(s"(?<!\\${fc.escape_char})\\${fc.quote_char}"))
        .limit(1).count() > 0)
        return ("38", false, Some("Formato texto inválido"), Some(sl.field_name))
    }

    ("1.21", true, None, None)
  }
}
