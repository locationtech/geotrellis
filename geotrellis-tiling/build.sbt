import Dependencies._

name := "geotrellis-tiling"
libraryDependencies ++= Seq(
  spire,
  chronoscala,
  monocleCore,
  monocleMacro,
  scalatest % Test
)

mimaPreviousArtifacts := Set(
  "org.locationtech.geotrellis" %% "geotrellis-tiling" % Version.previousVersion
)

fork in Test := false
parallelExecution in Test := false

initialCommands in console :=
  """
  import geotrellis.raster._
  import geotrellis.vector._
  import geotrellis.proj4._
  import geotrellis.tiling._
  """
