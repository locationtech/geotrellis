import Dependencies._

name := "geotrellis-streaming"
libraryDependencies ++= Seq(
  sparkStreaming % "provided",
  sparkCore % "provided",
  scalatest % "test"
)

fork in Test := false
parallelExecution in Test := false
