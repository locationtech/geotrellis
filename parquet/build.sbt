import Dependencies._

name := "geotrellis-parquet"
libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-sql" % Version.spark  % "provided",
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
  import geotrellis.spark.io.parquet._
  """
