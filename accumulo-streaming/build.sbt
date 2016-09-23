import Dependencies._

name := "geotrellis-accumulo-streaming"
libraryDependencies ++= Seq(
  sparkStreaming % "provided",
  sparkCore % "provided", 
  scalatest % "test"
)

fork in Test := false
parallelExecution in Test := false
