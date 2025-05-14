package utils

import config.{DbConfig, DBConnection, SparkSessionProvider}
import org.apache.spark.sql.{DataFrame, SaveMode}

import java.util.Properties

/**
 * Utilidades para escritura de datos en JDBC y CSV.
 */
object Writer {
  /**
   * Realiza un upsert en la tabla "trigger_control" a partir de un staging intermedio.
   *
   * @param tableName Nombre de la tabla destino.
   * @param df        DataFrame con los datos a insertar o actualizar.
   */
  def upsertTrigger(tableName: String, df: DataFrame): Unit = {
    val spark = SparkSessionProvider.getSparkSession
    val connectionProperties = new Properties()
    connectionProperties.setProperty("user", DbConfig.getUsername)
    connectionProperties.setProperty("password", DbConfig.getPassword)

    df.write.mode(SaveMode.Overwrite)
      .jdbc(DbConfig.getJdbcUrl, "staging_table", connectionProperties)

    val upsertQuery = """
      INSERT INTO trigger_control (
        id_trigger,
        id_type_file,
        file_name,
        environment,
        source,
        date_load,
        tst_trigger_control,
        flag,
        timestamp_load,
        row_count
      )
      SELECT
        id_trigger,
        id_type_file,
        file_name,
        environment,
        source,
        date_load,
        tst_trigger_control,
        flag,
        timestamp_load,
        row_count
      FROM staging_table
      ON CONFLICT (id_trigger)
      DO UPDATE SET
        id_type_file = EXCLUDED.id_type_file,
        file_name = EXCLUDED.file_name,
        environment = EXCLUDED.environment,
        source = EXCLUDED.source,
        date_load = EXCLUDED.date_load,
        tst_trigger_control = EXCLUDED.tst_trigger_control,
        flag = EXCLUDED.flag,
        timestamp_load = EXCLUDED.timestamp_load,
        row_count = EXCLUDED.row_count
    """

    DBConnection.getConnection() match {
      case Some(connection) =>
        try {
          val statement = connection.createStatement()
          statement.executeUpdate(upsertQuery)
          statement.close()
        } finally {
          connection.close()
        }
      case None => println("Error en la escritura de upsert")
    }
  }

  /**
   * Escribe un DataFrame en una tabla JDBC en modo Append.
   *
   * @param tableName Tabla destino.
   * @param df        DataFrame a escribir.
   */
  def writeDf(tableName: String, df: DataFrame): Unit = {
    val spark = SparkSessionProvider.getSparkSession
    val connectionProperties = new Properties()
    connectionProperties.setProperty("user", DbConfig.getUsername)
    connectionProperties.setProperty("password", DbConfig.getPassword)

    df.write.mode(SaveMode.Append)
      .jdbc(DbConfig.getJdbcUrl, tableName, connectionProperties)
  }

  /**
   * Exporta un DataFrame a CSV en la ruta especificada.
   *
   * @param df        DataFrame a exportar.
   * @param path      Ruta destino.
   * @param delimiter Delimitador de columnas.
   * @param header    Indica si incluir cabecera.
   */
  def writeDfToCsv(df: DataFrame, path: String, delimiter: String = ",", header: Boolean = true): Unit = {
    df.write
      .option("header", header.toString)
      .option("delimiter", delimiter)
      .mode("append")
      .csv(path)
  }
}