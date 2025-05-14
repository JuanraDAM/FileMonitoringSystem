package services

import org.apache.spark.sql.functions._
import utils.Reader

/**
 * Gestor de generación de identificadores únicos para triggers.
 *
 * Lee el máximo actual en BD y ofrece un método sincronizado para incrementar.
 */
object TriggerIdManager {
  /** Valor inicial del ID obtenido de trigger_control. */
  var initialMaxTriggerId = 0

  private var currentMaxIdTrigger: Int = {
    val maxId = Reader.readDf("trigger_control")
      .agg(max(col("id_trigger")).as("max_id"))
      .first().getAs[Int]("max_id")
    if (initialMaxTriggerId == 0) initialMaxTriggerId = maxId
    maxId
  }

  /**
   * Devuelve el siguiente ID de trigger, de forma atómica.
   * @return nuevo identificador.
   */
  def nextId(): Int = this.synchronized {
    currentMaxIdTrigger += 1
    currentMaxIdTrigger
  }
}
