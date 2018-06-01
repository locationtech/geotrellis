import Dependencies._

name := "geotrellis-hbase"
libraryDependencies ++= Seq(
  "org.apache.hbase" % "hbase-common" % Version.hbase exclude("javax.servlet", "servlet-api"),
  "org.apache.hbase" % "hbase-client" % Version.hbase exclude("javax.servlet", "servlet-api"),
  "org.apache.hbase" % "hbase-server" % Version.hbase exclude ("org.mortbay.jetty", "servlet-api-2.5"),
  "org.apache.hbase" % "hbase-hadoop-compat" % Version.hbase exclude("javax.servlet", "servlet-api"),
  "org.apache.hbase" % "hbase-hadoop2-compat" % Version.hbase exclude("javax.servlet", "servlet-api"),
  "org.apache.hbase" % "hbase-metrics" % Version.hbase exclude("javax.servlet", "servlet-api"),
  "org.apache.hbase" % "hbase-metrics-api" % Version.hbase exclude("javax.servlet", "servlet-api"),
  "org.codehaus.jackson"  % "jackson-core-asl" % "1.9.13",
  sparkCore % Provided,
  spire,
  scalatest % Test
)

fork in Test := false
parallelExecution in Test := false

initialCommands in console :=
  """
  import geotrellis.raster._
  import geotrellis.vector._
  import geotrellis.proj4._
  import geotrellis.spark._
  import geotrellis.spark.util._
  import geotrellis.spark.tiling._
  import geotrellis.spark.io.hbase._
  """
