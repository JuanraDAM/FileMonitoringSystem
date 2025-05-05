package config

import java.io.IOException
import java.util.Properties

object DbConfig {
  val props = new java.util.Properties()
  props.load(new java.io.FileInputStream("db.properties"))
  private val url      = props.getProperty("url")
  private val user     = props.getProperty("user")
  private val passwd   = props.getProperty("password")


  // Getters
  def getJdbcUrl: String   = url
  def getUsername: String  = user
  def getPassword: String  = passwd
}
