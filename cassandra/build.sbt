import Dependencies._

name := "geotrellis-cassandra"
libraryDependencies ++= Seq(
  "com.datastax.cassandra" % "cassandra-driver-core" % "3.0.0"
    excludeAll (ExclusionRule("org.jboss.netty"), ExclusionRule("io.netty"), ExclusionRule("org.slf4j"))
    exclude("org.apache.hadoop", "hadoop-client"),
  "org.apache.spark" %% "spark-core" % Version.spark % "provided",
  //hadoopClient,
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
  import geotrellis.spark.io.cassandra._
  """
