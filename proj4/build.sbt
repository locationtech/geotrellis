import Dependencies._

name := "geotrellis-proj4"

libraryDependencies ++= Seq(
  proj4j,
  openCSV,
  parserCombinators,
  scalatest  % Test,
  scalacheck % Test,
  scaffeine
)
