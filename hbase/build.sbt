import Dependencies._

name := "geotrellis-hbase"
libraryDependencies ++= Seq(
  "org.apache.hbase" % "hbase-common" % Version.hbase exclude("javax.servlet", "servlet-api"),
  "org.apache.hbase" % "hbase-client" % Version.hbase exclude("javax.servlet", "servlet-api"),
  "org.apache.hbase" % "hbase-server" % Version.hbase exclude ("org.mortbay.jetty", "servlet-api-2.5"),
  "org.apache.hbase" % "hbase-hadoop-compat" % Version.hbase exclude("javax.servlet", "servlet-api"),
  "org.codehaus.jackson"  % "jackson-core-asl" % "1.9.13",
  sparkCore % "provided",
  spire,
  scalatest % "test")

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
