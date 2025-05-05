// src/main/scala/validators/FunctionalValidator.scala
package validators

import models.FileConfigurationCaseClass
import org.apache.spark.sql.{Column, DataFrame}
import org.apache.spark.sql.functions._

object FunctionalValidator {

  /**
   * Evalúa que no haya ningún registro “inválido” según la columna booleana invalidCol.
   * Devuelve true si NO existe ningún true en invalidCol.
   */
  private def noneInvalid(df: DataFrame, invalidCol: Column): Boolean = {
    val bad = df
      .select(max(when(invalidCol, lit(1)).otherwise(lit(0))).alias("bad"))
      .first()
      .getAs[Int]("bad")
    bad == 0
  }

  /**
   * Ejecuta todas las validaciones funcionales y cross-field, devolviendo true si TODO es válido.
   */
  def verifyFunctional(df: DataFrame, fileConf: FileConfigurationCaseClass): Boolean = {
    // 1. Formato identificadores
    val idValid = noneInvalid(df, ! col("account_number").rlike("^[A-Za-z0-9]{10}$"))

    // 2. Rangos numéricos
    val creditValid = noneInvalid(df, ! col("credit_score").between(300, 850))
    val riskValid   = noneInvalid(df, ! col("risk_score").between(0, 100))

    // 3. Fechas lógicas
    // Convertimos ambos campos a DateType usando el formato de fileConf
    val dobDate      = to_date(col("date_of_birth"), fileConf.date_format)
    val createdDate  = to_date(col("created_date"),     fileConf.date_format)
    val dobNotFuture = noneInvalid(df, dobDate > current_date())
    val datesLogic   = noneInvalid(df, createdDate < dobDate)

    // 4. Validación de contacto
    val emailValid = noneInvalid(df, ! col("email").rlike("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
    val phoneValid = noneInvalid(df,
      ! col("phone").cast("string").rlike("^\\+?[0-9]{10,15}$")
    )

    // 5. Estados y categorías válidas
    val typeValid   = noneInvalid(df,
      ! col("account_type").isin("Checking", "Savings", "Business", "Investment")
    )
    val statusValid = noneInvalid(df,
      ! col("status").isin("Active", "Closed")
    )

    // 6. Cross-Field
    val ageValid = noneInvalid(df,
      months_between(current_date(), dobDate) < lit(18 * 12)
    )

    val balanceActiveValid = noneInvalid(df,
      (col("status") === "Active") && col("balance") < 0
    )
    val balanceClosedValid = noneInvalid(df,
      (col("status") === "Closed") && col("balance") =!= 0
    )

    val interestValid = noneInvalid(df,
      (col("account_type") === "Checking") && col("interest_rate") =!= 0
    )

    val overdraftValid = noneInvalid(df,
      (col("overdraft_limit") < 0) ||
        ((col("overdraft_limit") > 0) && col("status") =!= "Active")
    )

    val jointValid = noneInvalid(df,
      (col("is_joint_account") === "Yes") && col("num_transactions") < 2
    )

    val avgTransValid = noneInvalid(df,
      (col("num_transactions") === 0) && col("avg_transaction_amount") =!= 0
    )

    // Combinamos todos los resultados: debe ser todo true
    Seq(
      idValid,
      creditValid, riskValid,
      dobNotFuture, datesLogic,
      emailValid, phoneValid,
      typeValid, statusValid,
      ageValid,
      balanceActiveValid, balanceClosedValid,
      interestValid, overdraftValid,
      jointValid, avgTransValid
    ).forall(_ == true)
  }
}
