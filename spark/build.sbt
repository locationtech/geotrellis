import Dependencies._

name := "geotrellis-spark"
libraryDependencies ++= Seq(
  sparkCore % "provided",
  hadoopClient % "provided",
  "org.locationtech.sfcurve" %% "sfcurve-geowave-index" % "0.2.1-SNAPSHOT",
  "org.scalaj" %% "scalaj-http" % "2.3.0",
  avro,
  spire,
  monocleCore, monocleMacro,
  chronoscala,
  scalazStream,
  scalatest % "test"
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
  """
