package validators

import models.FileConfigurationCaseClass
import org.apache.spark.sql.{Column, DataFrame}
import org.apache.spark.sql.functions._

/**
 * Valida reglas de negocio específicas de los datos de cuentas bancarias.
 */
object FunctionalValidator {
  /**
   * Comprueba que no haya valores que cumplan la condición inválida.
   *
   * @param df      DataFrame a validar.
   * @param invalid Expresión booleana indicando valores inválidos.
   * @return true si no se detecta ningún valor inválido.
   */
  private def noneInvalid(df: DataFrame, invalid: Column): Boolean = {
    val bad = df.select(max(when(invalid, 1).otherwise(0)).alias("bad"))
      .first().getAs[Int]("bad")
    bad == 0
  }

  /**
   * Ejecuta todas las validaciones funcionales definidas en la configuración.
   *
   * @param df DataFrame con los datos a validar.
   * @param fc Configuración del fichero (incluye formato de fecha).
   * @return Tupla con código de flag, éxito, mensaje de error y campo implicado.
   */
  def verifyFunctional(
                        df: DataFrame,
                        fc: FileConfigurationCaseClass
                      ): (String, Boolean, Option[String], Option[String]) = {
    if (!noneInvalid(df, !col("account_number").rlike("^[A-Za-z0-9]{10}$")))
      return ("40", false, Some("Formato inválido"), Some("account_number"))

    if (!noneInvalid(df, !col("credit_score").between(300, 850)))
      return ("41", false, Some("Fuera de rango"), Some("credit_score"))

    if (!noneInvalid(df, !col("risk_score").between(0, 100)))
      return ("42", false, Some("Fuera de rango"), Some("risk_score"))

    val dob = to_date(col("date_of_birth"), fc.date_format)
    if (!noneInvalid(df, months_between(current_date(), dob) < lit(18 * 12)))
      return ("43", false, Some("Menor de edad"), Some("date_of_birth"))

    if (!noneInvalid(df, col("status") === "Active" && col("balance") < 0))
      return ("44", false, Some("Negativo en Active"), Some("balance"))
    if (!noneInvalid(df, col("status") === "Closed" && col("balance") =!= 0))
      return ("45", false, Some("No cero en Closed"), Some("balance"))

    if (!noneInvalid(df, col("account_type") === "Checking" && col("interest_rate") =!= 0))
      return ("46", false, Some("Interest ≠0"), Some("interest_rate"))

    if (!noneInvalid(df,
      col("overdraft_limit") < 0 ||
        (col("overdraft_limit") > 0 && col("status") =!= "Active")
    ))
      return ("47", false, Some("Overdraft inválido"), Some("overdraft_limit"))

    if (!noneInvalid(df, col("is_joint_account") === "Yes" && col("num_transactions") < 2))
      return ("48", false, Some("Pocas tx en joint"), Some("num_transactions"))

    if (!noneInvalid(df, col("num_transactions") === 0 && col("avg_transaction_amount") =!= 0))
      return ("49", false, Some("Avg tx ≠0 con 0 tx"), Some("avg_transaction_amount"))

    ("1.41", true, None, None)
  }
}

