import Dependencies._

name := "geotrellis-raster"

libraryDependencies ++= Seq(
  typesafeConfig,
  jts,
  spire,
  monocleCore,
  monocleMacro,
  scalatest % Test,
  scalacheck % Test
)

mimaPreviousArtifacts := Set(
  "org.locationtech.geotrellis" %% "geotrellis-raster" % Version.previousVersion
)

sourceGenerators in Compile += (sourceManaged in Compile).map(Boilerplate.genRaster).taskValue

initialCommands in console :=
  """
  import geotrellis.raster._
  import geotrellis.raster.resample._
  import geotrellis.vector._
  """
