import Dependencies._

name := "geotrellis-cassandra"
libraryDependencies ++= Seq(
  "org.apache.hbase" % "hbase-common" % "1.2.1",
  "org.apache.hbase" % "hbase-client" % "1.2.1",
  "org.apache.hbase" % "hbase-server" % "1.2.1" excludeAll ExclusionRule(organization = "org.mortbay.jetty"),
  "org.apache.hbase" % "hbase-prefix-tree" % "1.2.1" exclude("javax.servlet", "servlet-api"),
  "org.apache.hbase" % "hbase-hadoop-compat" % "1.2.1" exclude("javax.servlet", "servlet-api"),
  "org.apache.spark" %% "spark-core" % Version.spark % "provided",
  "org.apache.hadoop" % "hadoop-client" % Version.hadoop % "provided",
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
