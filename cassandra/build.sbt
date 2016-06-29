import Dependencies._

name := "geotrellis-cassandra"
libraryDependencies ++= Seq(
  // 2.1.10.2 version used to provide guava compatibility with hadoop stack
  "com.datastax.cassandra" % "cassandra-driver-core" % "2.1.10.2"
    excludeAll (ExclusionRule("org.jboss.netty"), ExclusionRule("io.netty"), ExclusionRule("org.slf4j"), ExclusionRule("io.spray"), ExclusionRule("com.typesafe.akka"))
    exclude("org.apache.hadoop", "hadoop-client"),
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
  import geotrellis.spark.io.cassandra._
  """
