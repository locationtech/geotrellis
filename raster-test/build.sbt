import Dependencies._

name := "geotrellis-raster-test"     
libraryDependencies ++= Seq(
  scalatest % "test",
  scalacheck  % "test",
  spire % "test",
  sprayClient % "test",
  sprayRouting % "test")
addCompilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full)
parallelExecution := false
fork in test := false
javaOptions in run += "-Xmx2G"
scalacOptions in compile += "-optimize"