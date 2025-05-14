package utils

import config.{DbConfig, SparkSessionProvider}
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.functions.{col, column, lit, row_number, when}

/**
 * Utilidad para lectura de datos desde JDBC y ficheros locales o HDFS.
 */
object Reader {
  private val spark = SparkSessionProvider.getSparkSession
  spark.conf.set("spark.sql.json.inferLong", "false")

  /**
   * Carga una tabla o consulta JDBC en un DataFrame.
   *
   * @param tableLink    Nombre de la tabla o subconsulta, debe incluir paréntesis y alias.
   * @param checkpoint   Si es true, utiliza particionado y fetchSize para optimizar la lectura.
   * @return DataFrame con los datos obtenidos por JDBC.
   */
  def readDf(tableLink: String, checkpoint: Boolean = false): DataFrame = {
    val numPartitions = 12
    val fetchSize = 10000

    if (checkpoint) {
      val query1 = s"(SELECT * FROM (SELECT /*+ PARALLEL(8) */ * FROM $tableLink WHERE 1=0) AS inner_sub WHERE 1=0) AS subquery"
      val query2 = s"(SELECT * FROM $tableLink LIMIT 3000000) AS subquery"

      val columnNames = spark.read
        .format("jdbc")
        .option("url", DbConfig.getJdbcUrl)
        .option("dbtable", query1)
        .option("user", DbConfig.getUsername)
        .option("password", DbConfig.getPassword)
        .load()
        .columns

      val columnToPartition = columnNames(5)
      val connectionProperties = new java.util.Properties()
      connectionProperties.put("user", DbConfig.getUsername)
      connectionProperties.put("password", DbConfig.getPassword)
      connectionProperties.put("fetchsize", fetchSize.toString)
      connectionProperties.put("driver", "org.postgresql.Driver")

      val predicates = (0 until numPartitions).map { i =>
        s"MOD(ABS(CAST(SPLIT_PART($columnToPartition, '.', 1) AS BIGINT)), $numPartitions) = $i"
      }.toArray

      spark.read
        .jdbc(
          url = DbConfig.getJdbcUrl,
          table = query2,
          predicates = predicates,
          connectionProperties = connectionProperties
        )
    } else {
      val query = s"(SELECT * FROM $tableLink) AS subquery"
      spark.read
        .format("jdbc")
        .option("driver", "org.postgresql.Driver")
        .option("url", DbConfig.getJdbcUrl)
        .option("dbtable", query)
        .option("user", DbConfig.getUsername)
        .option("password", DbConfig.getPassword)
        .load()
    }
  }

  /**
   * Lee un fichero soportado por Spark (csv, json, parquet, avro, etc.).
   *
   * @param path Ruta (HDFS o local) al fichero.
   * @param opts Opciones de lectura específicas.
   * @return DataFrame resultante.
   */
  def readFile(path: String, opts: Map[String, String] = Map.empty): DataFrame = {
    val ext = path.reverse.takeWhile(_ != '.').reverse.toLowerCase
    val format = ext match {
      case "csv"     => "csv"
      case "json"    => "json"
      case "parquet" => "parquet"
      case other      => other
    }

    val defaults: Map[String, String] = format match {
      case "csv" => Map(
        "header" -> "true",
        "sep" -> ",",
        "inferSchema" -> "true"
      )
      case "json" => Map("inferSchema" -> "true")
      case _       => Map.empty
    }

    val reader = spark.read.format(format)
    (defaults ++ opts).foreach { case (k, v) => reader.option(k, v) }
    reader.load(path)
  }
}
