package services

import org.apache.spark.sql.functions._
import utils.Reader

object TriggerIdManager {
  var initialMaxTriggerId = 0

  // Obtener el valor inicial del contador desde la base de datos
  private var currentMaxIdTrigger: Int = {
    val maxId = Reader.readDf("trigger_control")
      .agg(max(col("id_trigger")).as("max_id"))
      .first()
      .getAs[Int]("max_id")

    if(initialMaxTriggerId == 0){
      initialMaxTriggerId = maxId
    }

    maxId
  }

  // Método sincronizado para asegurar unicidad
  def nextId(): Int = this.synchronized {
    currentMaxIdTrigger += 1
    currentMaxIdTrigger
  }
}