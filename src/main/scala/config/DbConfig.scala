package config

import java.util.Properties

/**
 * Objeto de configuración JDBC que carga las propiedades
 * de conexión desde el archivo 'db.properties' y expone
 * getters para URL, usuario y contraseña.
 */
object DbConfig {
  private val props = new Properties()
  props.load(new java.io.FileInputStream("db.properties"))

  private val url    = props.getProperty("url")
  private val user   = props.getProperty("user")
  private val passwd = props.getProperty("password")

  /**
   * Obtiene la URL JDBC para conectarse a la base de datos.
   * @return cadena con la URL JDBC.
   */
  def getJdbcUrl: String = "jdbc:postgresql://superset-db:5432/superset"

  /**
   * Obtiene el usuario de la base de datos.
   * @return nombre de usuario.
   */
  def getUsername: String = "superset"

  /**
   * Obtiene la contraseña de la base de datos.
   * @return contraseña.
   */
  def getPassword: String = "superset"
}
