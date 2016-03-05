import Dependencies._

name := "geotrellis-engine-test"
parallelExecution := true
fork in Test := true

libraryDependencies ++= Seq(
  sprayClient % "test",
  sprayRouting % "test",
  scalatest % "test")
