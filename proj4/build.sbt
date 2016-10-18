import Dependencies._

name := "geotrellis-proj4"

libraryDependencies ++= Seq(
  "com.azavea" % "proj4j" % "1.0",
  openCSV,
  scalatest   % "test",
  scalacheck  % "test")
