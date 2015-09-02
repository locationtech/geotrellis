import Dependencies._

name := "geotrellis-engine-test"
parallelExecution := true
fork in test := true
javaOptions in run += "-Xmx4G"
scalacOptions in compile += "-optimize"
libraryDependencies ++= Seq(
  sprayClient % "test",
  sprayRouting % "test",
  scalatest % "test")