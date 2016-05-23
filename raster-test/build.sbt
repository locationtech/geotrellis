import Dependencies._

name := "geotrellis-raster-test"
libraryDependencies ++= Seq(
  scalatest % "test",
  scalacheck  % "test",
  spire % "test")
addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)

parallelExecution := false
