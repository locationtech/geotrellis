import Dependencies._

name := "geotrellis-raster-test"     
libraryDependencies ++= Seq(
  scalatest % "test",
  scalacheck  % "test",
  spire % "test",
  sprayClient % "test",
  sprayRouting % "test")
addCompilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full)

parallelExecution := true
fork in test := true

// TODO: Make the "Boot" go away. Caused by the Jpg tests.
javaOptions in test += "-J-Djava.awt.headless=true"
