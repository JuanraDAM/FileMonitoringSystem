// src/main/scala/validators/TypeValidator.scala
package validators

import models.SemanticLayerCaseClass
import org.apache.spark.sql.{Column, DataFrame, Dataset}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._

object TypeValidator {

  /** Marca si hay algún casteo inválido */
  private def basicTypeInvalidCol(df: DataFrame, semList: Seq[SemanticLayerCaseClass]): Column = {
    val checks = semList.map { sl =>
      val c = col(sl.field_name)
      val casted = sl.data_type.toLowerCase match {
        case t if Seq("int","integer").exists(t.startsWith) => c.cast(IntegerType)
        case t if t.startsWith("bigint")                    => c.cast(LongType)
        case t if t.startsWith("decimal")                   => c.cast(DecimalType.SYSTEM_DEFAULT)
        case "date"                                         => to_date(c, sl.decimal_symbol)
        case "timestamp"                                    => to_timestamp(c, sl.decimal_symbol)
        case _                                              => c
      }
      c.isNotNull && casted.isNull
    }
    checks.reduceOption(_ or _).getOrElse(lit(false))
  }

  /** Marca si hay nulos donde no debería */
  private def nullInvalidCol(semList: Seq[SemanticLayerCaseClass]): Column = {
    val checks = semList.filter(!_.nullable).map(sl => col(sl.field_name).isNull)
    checks.reduceOption(_ or _).getOrElse(lit(false))
  }

  /** Marca si alguna longitud de texto excede la permitida */
  private def lengthInvalidCol(semList: Seq[SemanticLayerCaseClass]): Column = {
    // solo filas con tipo CHAR/VARCHAR y con length definido numérico
    val checks: Seq[Column] = semList
      .filter(sl => {
        val t = sl.data_type.toLowerCase
        (t.startsWith("char") || t.startsWith("varchar")) && sl.length.exists(s => s.forall(_.isDigit))
      })
      .flatMap { sl =>
        // extraemos el número máximo
        sl.length.flatMap(s => scala.util.Try(s.toInt).toOption).map { maxLen =>
          length(col(sl.field_name)) > lit(maxLen)
        }
      }

    checks.reduceOption(_ or _).getOrElse(lit(false))
  }

  /** Marca si algún texto contiene delimitador o comilla sin escape */
  private def textFormatInvalidCol(
                                    semList: Seq[SemanticLayerCaseClass],
                                    delimiter: String,
                                    quoteChar: String,
                                    escapeChar: String
                                  ): Column = {
    val nonText = Seq("int","integer","bigint","decimal","date","timestamp")
    val checks = semList
      .filterNot(sl => nonText.exists(sl.data_type.toLowerCase.startsWith))
      .map { sl =>
        val c = col(sl.field_name)
        c.contains(delimiter) ||
          c.rlike(s"(?<!\\${escapeChar})\\${quoteChar}")
      }
    checks.reduceOption(_ or _).getOrElse(lit(false))
  }

  /** Devuelve true si ninguna fila ha fallado la columna inválida */
  private def noneInvalid(df: DataFrame, invalidCol: Column): Boolean = {
    val bad = df
      .select(max(when(invalidCol, lit(1)).otherwise(lit(0))).alias("bad"))
      .collect()(0)
      .getAs[Int]("bad")
    bad == 0
  }

  /** Ejecuta todas las validaciones de tipado usando Column API */
  def verifyTyping(
                    df: DataFrame,
                    fileConf: models.FileConfigurationCaseClass,
                    semDs: Dataset[SemanticLayerCaseClass]
                  ): Boolean = {
    // traemos semántica a driver
    val semList = semDs.collect().sortBy(_.field_position)

    val basicInvalid  = basicTypeInvalidCol(df, semList)
    val nullInvalid   = nullInvalidCol(semList)
    val lengthInvalid = lengthInvalidCol(semList)
    val textInvalid   = textFormatInvalidCol(
      semList,
      fileConf.delimiter,
      fileConf.quote_char,
      fileConf.escape_char
    )

    Seq(basicInvalid, nullInvalid, lengthInvalid, textInvalid)
      .map(col => noneInvalid(df, col))
      .forall(identity)
  }
}
