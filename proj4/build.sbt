import Dependencies._

name := "geotrellis-proj4"

libraryDependencies ++= Seq(
  openCSV,
  parserCombinators,
  scalatest  % Test,
  scalacheck % Test,
  scaffeine
)
