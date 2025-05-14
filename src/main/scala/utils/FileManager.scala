package utils

import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions.col
import services.TriggerIdManager.initialMaxTriggerId
import utils.Reader

/**
 * Utilidades para construcción de rutas y filtrado de ficheros a verificar.
 */
object FileManager {
  /**
   * Construye la ruta completa combinando ruta base, nombre y extensión.
   *
   * @param fileName   Nombre de fichero sin extensión.
   * @param fileFormat Extensión (p.ej. "csv").
   * @param route      Ruta base.
   * @return ruta completa con formato.
   */
  def getFilePath(fileName: String, fileFormat: String, route: String): String =
    s"$route$fileName.$fileFormat"

  /**
   * Devuelve DataFrame de configuraciones de ficheros pendientes según flag.
   *
   * @param flag código de flag de validación.
   * @return DataFrame con registros de file_configuration.
   */
  def getFilesToVerify(flag: Int): DataFrame = {
    val triggerControlDf = Reader.readDf("trigger_control")
      .select("file_name", "flag")
      .filter(col("flag") === flag && col("id_trigger") > initialMaxTriggerId)
      .select("file_name").distinct()

    val fileConfigDf = Reader.readDf("file_configuration")
    fileConfigDf.join(triggerControlDf,
      fileConfigDf.col("type_file_name") === triggerControlDf.col("file_name"),
      "left_semi")
  }
}
