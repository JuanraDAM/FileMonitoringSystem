import sbtassembly.AssemblyPlugin.autoImport._
import sbtassembly.{MergeStrategy, PathList}

lazy val root = (project in file("."))
  .enablePlugins(sbtassembly.AssemblyPlugin)
  .settings(
    name := "Fin_de_Grado",
    scalaVersion := "2.12.18",
    version := "0.1.0-SNAPSHOT",

    libraryDependencies ++= Seq(
      "org.apache.spark" %% "spark-core" % "3.3.1",
      "org.apache.spark" %% "spark-sql"  % "3.3.1",
      "org.postgresql"    % "postgresql"  % "42.5.1"
    ),

    Compile / mainClass := Some("Main"),
    Compile / fork      := true,

    // log4j2 + Java 11 opens
    Compile / javaOptions ++= Seq(
      "-Dlog4j2.configurationFile=log4j2.properties",
      "--add-opens=java.base/java.lang=ALL-UNNAMED",
      "--add-opens=java.base/java.nio=ALL-UNNAMED",
      "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED"
    ),

    // fusionar los META-INF/services correctamente (para el driver JDBC)
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", "services", xs @ _*) => MergeStrategy.concat
      case PathList("META-INF", xs @ _*)            => MergeStrategy.discard
      case _                                        => MergeStrategy.first
    }
  )
