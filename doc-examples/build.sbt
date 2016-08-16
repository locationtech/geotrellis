import Dependencies._

name := "geotrellis-doc-examples"
libraryDependencies ++= Seq(
  sparkCore % "provided",
  logging,
  scalatest % "test")
