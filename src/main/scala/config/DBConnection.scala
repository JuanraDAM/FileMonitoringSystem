package config

import java.sql.{Connection, DriverManager, SQLException}

object DBConnection {
  def getConnection(): Option[Connection] = {
    // ─── ¡IMPORTANTE! ────────────────────────────────────────────────────────────
    // Esto fuerza a que el driver JDBC de Postgres se registre en el DriverManager
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
