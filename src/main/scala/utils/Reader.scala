package utils
import config.{DbConfig, SparkSessionProvider}
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.functions.{col, column, lit, row_number, when}

import java.io.File

object Reader {
  private val spark = SparkSessionProvider.getSparkSession
  spark.conf.set("spark.sql.json.inferLong", "false")

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


      val columToPartition = columnNames(5)

      val connectionProperties = new java.util.Properties()
      connectionProperties.put("user", DbConfig.getUsername)
      connectionProperties.put("password", DbConfig.getPassword)
      connectionProperties.put("fetchsize", fetchSize.toString)
      connectionProperties.put("driver", "org.postgresql.Driver")

      val regex = "'^[0-9]+(\\.[0-9]+)?$'"
      val predicates = (0 until numPartitions).map { i =>
        s"""MOD(ABS(CAST(SPLIT_PART($columToPartition, '.', 1) AS BIGINT)), $numPartitions) = $i"""
      }.toArray

      val df = spark.read
        .jdbc(
          url = DbConfig.getJdbcUrl,
          table = query2,
          predicates = predicates,
          connectionProperties = connectionProperties
        )
      df

    } else {
      val query = s"(SELECT * FROM $tableLink) AS subquery"
      spark.read
        .format("jdbc")
        .option("driver", "org.postgresql.Driver")
        .option("url", DbConfig.getJdbcUrl)
        .option("dbtable", query)
        .option("user", DbConfig.getUsername)
        .option("password", DbConfig.getPassword)
        //.option("fetchsize", fetchSize)
        .load()
    }
  }

  /**
   * Lee cualquier fichero soportado:
   *  - csv  ⇒ Header=true, sep="," por defecto
   *  - json ⇒ inferSchema=true
   *  - parquet, avro, etc. ⇒ sin opciones por defecto
   *
   * @param path    ruta (HDFS o local) al fichero
   * @param opts    opciones extra / overrides
   * @return        DataFrame
   */
  def readFile(path: String, opts: Map[String, String] = Map.empty): DataFrame = {
    // 1) inferir formato por extensión
    val ext = path.reverse.takeWhile(_!='.').reverse.toLowerCase
    val format = ext match {
      case "csv"     => "csv"
      case "json"    => "json"
      case "parquet" => "parquet"
      case other     => other
    }

    // 2) opciones por defecto según formato
    val defaults: Map[String, String] = format match {
      case "csv" =>
        Map(
          "header"      -> "true",
          "sep"         -> ",",
          "inferSchema" -> "true"
        )
      case "json" =>
        Map("inferSchema" -> "true")
      case _ =>
        Map.empty
    }

    // 3) construir reader
    val reader = spark.read.format(format)
    (defaults ++ opts).foreach { case (k, v) => reader.option(k, v) }

    // 4) cargar
    reader.load(path)
  }

}