import Dependencies._

name := "geotrellis-proj4"

libraryDependencies ++= Seq(
  apacheSIS,
  apacheSISEPSG,
  openCSV,
  scalatest   % "test",
  scalacheck  % "test")
