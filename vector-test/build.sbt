import Dependencies._

name := "geotrellis-vector-test"
libraryDependencies ++= Seq(
  akkaActor   % "test",
  sprayHttpx  % "test",
  scalatest   % "test",
  scalacheck  % "test")