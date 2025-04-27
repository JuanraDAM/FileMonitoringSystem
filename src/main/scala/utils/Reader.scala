package utils
import config.{DbConfig, SparkSessionProvider}
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.functions.{col, column, lit, row_number, when}

import java.io.File

object Reader {
  private val spark = SparkSessionProvider.getSparkSession
  spark.conf.set("spark.sql.json.inferLong", "false")

  def readDf(tableLink: String): DataFrame = {
    val query = s"(SELECT * FROM $tableLink LIMIT 4000000) as subquery"
    val df = spark.read
      .format("jdbc")
      .option("url", DbConfig.getJdbcUrl)
      .option("dbtable", query)
      .option("user", DbConfig.getUsername)
      .option("password", DbConfig.getPassword)
      .load()
    df
  }

  def readTableDf(tableLink: String, checkpoint: Boolean = false): DataFrame = {
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
        .option("url", DbConfig.getJdbcUrl)
        .option("dbtable", query)
        .option("user", DbConfig.getUsername)
        .option("password", DbConfig.getPassword)
        .option("fetchsize", fetchSize)
        .load()
    }
  }




  def readerPartitioned (tableLink: String, partitions: Int): DataFrame = {
    val query = s"(SELECT CAST(row_y AS BIGINT) AS row_y, * FROM $tableLink LIMIT 4000000) as subquery"
    val df = spark.read
      .format("jdbc")
      .option("url", DbConfig.getJdbcUrl)
      .option("dbtable", query)
      .option("user", DbConfig.getUsername)
      .option("password", DbConfig.getPassword)
      // Opciones de particionamiento:
      .option("partitionColumn", "row_y")
      .option("lowerBound", "1")
      .option("upperBound", "40000000000")
      .option("numPartitions", partitions)
      .load()
    df
  }
}