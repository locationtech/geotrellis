import Dependencies._

name := "geotrellis-vector-test"
libraryDependencies ++= Seq(
  akkaActor   % "test",
  scalatest   % "test",
  scalacheck  % "test")
