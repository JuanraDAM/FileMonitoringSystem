package config

import java.sql.{Connection, DriverManager, SQLException}

/**
 * Objeto proveedor de conexiones JDBC a Postgres.
 * Registra el driver y devuelve una Option[Connection].
 */
object DBConnection {
  /**
   * Obtiene una conexión a la base de datos PostgreSQL.
   *
   * Registra el driver y utiliza DbConfig para URL, usuario y contraseña.
   * @return Some(Connection) si la conexión es exitosa, None en caso contrario.
   */
  def getConnection(): Option[Connection] = {
    Class.forName("org.postgresql.Driver")

    val url      = DbConfig.getJdbcUrl
    val user     = DbConfig.getUsername
    val password = DbConfig.getPassword

    try {
      val conn = DriverManager.getConnection(url, user, password)
      Some(conn)
    } catch {
      case e: SQLException =>
        println(s"❌ Error al conectar a la base de datos: ${e.getMessage}")
        None
    }
  }
}
