import Dependencies._

name := "geotrellis-geotools"

libraryDependencies ++= Seq(
  "org.geotools" % "gt-coverage" % Version.geotools,
  "org.geotools" % "gt-epsg-hsql" % Version.geotools,
  "org.geotools" % "gt-geotiff" % Version.geotools,
  "org.geotools" % "gt-main" % Version.geotools,
  "org.geotools" % "gt-referencing" % Version.geotools,
  "org.apache.spark" %% "spark-core" % Version.spark % "provided",
  spire,
  scalatest % "test")

fork in Test := false
parallelExecution in Test := false

initialCommands in console :=
  """
  import geotrellis.geotools._
  import geotrellis.raster._
  import geotrellis.vector._
  import org.geotools.coverage.grid._
  import org.geotools.coverage.grid.io._
  import org.geotools.gce.geotiff._

  """
