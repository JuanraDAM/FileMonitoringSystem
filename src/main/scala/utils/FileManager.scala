package utils

import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions.col
import services.TriggerIdManager.initialMaxTriggerId

object FileManager {
  def getFilePath(fileName: String, fileFormat: String, route: String): String = {
    route + fileName + "." + fileFormat
  }
  def getFilesToVerify(flag: Int): DataFrame = {
    val triggerControlDf = Reader
      .readDf("trigger_control")
      .select("file_name", "flag")
      .filter(col("flag") === flag && col("id_trigger") > initialMaxTriggerId)
      .select("file_name")
      .distinct()

    val fileConfigurationDf = Reader.readDf("file_configuration")
    val dfJoined = fileConfigurationDf
      .join(
        triggerControlDf,
        fileConfigurationDf.col("type_file_name") === triggerControlDf.col("file_name"),
        "left_semi")
    dfJoined
  }
}