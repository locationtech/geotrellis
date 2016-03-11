import Dependencies._

name := "geotrellis-proj4"

libraryDependencies ++= Seq(
  openCSV,
  scalatest   % "test",
  scalacheck  % "test")
