import Dependencies._

name := "geotrellis-hbase"
libraryDependencies ++= Seq(
  "org.apache.hbase" % "hbase-common" % Version.hbase,
  "org.apache.hbase" % "hbase-client" % Version.hbase,
  "org.apache.hbase" % "hbase-server" % Version.hbase excludeAll ExclusionRule(organization = "org.mortbay.jetty"),
  "org.apache.hbase" % "hbase-prefix-tree" % Version.hbase exclude("javax.servlet", "servlet-api"),
  "org.apache.hbase" % "hbase-hadoop-compat" % Version.hbase exclude("javax.servlet", "servlet-api"),
  "org.apache.spark" %% "spark-core" % Version.spark % "provided",
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
