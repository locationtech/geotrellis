import Dependencies._

name := "geotrellis-raster"
libraryDependencies ++= Seq(
  typesafeConfig,
  jts,
  spire,
  monocleCore,
  monocleMacro,
  openCSV)
addCompilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full)
scalacOptions ++= Seq("-optimize", "-language:experimental.macros")
javaOptions in run += "-Xmx2G"
parallelExecution := false
fork in test := false
initialCommands in console :=
  """
  import geotrellis.raster._
  import geotrellis.raster.resample._
  import geotrellis.vector._
  """
