import Dependencies._

name := "geotrellis-proj4"
libraryDependencies ++= Seq(
  openCSV,
  "org.parboiled" %% "parboiled" % "2.0.0" % "test",
  scalatest   % "test",
  scalacheck  % "test")