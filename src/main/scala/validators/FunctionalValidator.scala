package validators

import models.FileConfigurationCaseClass
import org.apache.spark.sql.{Column, DataFrame}
import org.apache.spark.sql.functions._

/**
 * Objeto que agrupa las validaciones funcionales y cross-field sobre un DataFrame.
 * Utiliza la configuración de fichero para formatos de fecha y delimitadores.
 */
object FunctionalValidator {

  /**
   * Evalúa que no haya ningún registro “inválido” según la columna booleana invalidCol.
   * @param df DataFrame a evaluar
   * @param invalidCol Columna booleana que marca registros inválidos
   * @return true si NO existe ningún true en invalidCol
   */
  private def noneInvalid(df: DataFrame, invalidCol: Column): Boolean = {
    val bad = df
      .select(max(when(invalidCol, lit(1)).otherwise(lit(0))).alias("bad"))
      .first()
      .getAs[Int]("bad")
    val result = bad == 0
    println(s"🔍 noneInvalid resultado: $result (bad flag=$bad)")
    result
  }

  /**
   * Ejecuta todas las validaciones funcionales y cross-field, devolviendo true si TODO es válido.
   * Además, imprime el resultado de cada comprobación.
   * @param df DataFrame con los datos a validar
   * @param fileConf Configuración de fichero (delimitador, formatos de fecha, etc.)
   * @return true si todas las validaciones pasan
   */
  def verifyFunctional(df: DataFrame, fileConf: FileConfigurationCaseClass): Boolean = {
    println("🚀 Iniciando validaciones funcionales...")

    // 1. Formato identificadores
    val idValid = noneInvalid(df, !col("account_number").rlike("^[A-Za-z0-9]{10}$"))
    println(s"🆔 Formato identificadores: $idValid")

    // 2. Rangos numéricos
    val creditValid = noneInvalid(df, !col("credit_score").between(300, 850))
    val riskValid   = noneInvalid(df, !col("risk_score").between(0, 100))
    println(s"💳 Rango credit_score: $creditValid, ⚖️ rango risk_score: $riskValid")

    // 3. Fechas lógicas
    val dobDate     = to_date(col("date_of_birth"), fileConf.date_format)
    val createdDate = to_date(col("created_date"), fileConf.date_format)
    val dobNotFuture = noneInvalid(df, dobDate > current_date())
    val datesLogic   = noneInvalid(df, createdDate < dobDate)
    println(s"📅 date_of_birth no futura: $dobNotFuture, 🕒 created_date >= date_of_birth: $datesLogic")

    // 4. Validación de contacto
    val emailValid = noneInvalid(df, !col("email").rlike("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
    val phoneValid = noneInvalid(df, !col("phone").cast("string").rlike("^\\+?[0-9]{10,15}$"))
    println(s"📧 Email válido: $emailValid, 📱 teléfono válido: $phoneValid")

    // 5. Estados y categorías válidas
    val typeValid   = noneInvalid(df, !col("account_type").isin("Checking", "Savings", "Business", "Investment"))
    val statusValid = noneInvalid(df, !col("status").isin("Active", "Closed"))
    println(s"🏷️ account_type válido: $typeValid, 🔓 status válido: $statusValid")

    // 6. Cross-Field
    val ageValid = noneInvalid(df, months_between(dobDate, current_date()) >= lit(18 * 12))
    println(s"🔞 Edad mínima 18 años: $ageValid")

    val balanceActiveValid = noneInvalid(df, when(col("status") === "Active" && col("balance") < 0, true).otherwise(false))
    val balanceClosedValid = noneInvalid(df, when(col("status") === "Closed" && col("balance") =!= 0, true).otherwise(false))
    println(s"💰 Balance activo >=0: $balanceActiveValid, 🚫 balance cerrado =0: $balanceClosedValid")

    val interestValid = noneInvalid(df, when(col("account_type") === "Checking" && col("interest_rate") =!= 0, true).otherwise(false))
    println(s"🏦 Checking interest_rate =0: $interestValid")

    val overdraftValid = noneInvalid(df,
      when(col("overdraft_limit") < 0 || (col("overdraft_limit") > 0 && col("status") =!= "Active"), true).otherwise(false)
    )
    println(s"🔒 Overdraft válido: $overdraftValid")

    val jointValid = noneInvalid(df,
      when(col("is_joint_account") === "Yes" && col("num_transactions") < 2, true).otherwise(false)
    )
    println(s"🤝 Joint account >=2 transacciones: $jointValid")

    val avgTransValid = noneInvalid(df,
      when(col("num_transactions") === 0 && col("avg_transaction_amount") =!= 0, true).otherwise(false)
    )
    println(s"📊 avg_transaction_amount con num_transactions=0: $avgTransValid")

    // Combinamos todos los resultados
    val allValid = Seq(
      idValid, creditValid, riskValid,
      dobNotFuture, datesLogic,
      emailValid, phoneValid,
      typeValid, statusValid,
      ageValid,
      balanceActiveValid, balanceClosedValid,
      interestValid, overdraftValid,
      jointValid, avgTransValid
    ).forall(identity)

    println(s"✅ Resultado final validaciones funcionales: $allValid 🎉")
    allValid
  }
}
