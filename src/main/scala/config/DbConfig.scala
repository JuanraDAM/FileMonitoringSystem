package config

object DbConfig {
  // URL de conexión a PostgreSQL en el contenedor superset-db
  private val jdbcUrl   = "jdbc:postgresql://localhost:5432/superset"
  private val username  = "superset"
  private val password  = "superset"

  def getJdbcUrl: String  = jdbcUrl
  def getUsername: String = username
  def getPassword: String = password
}
