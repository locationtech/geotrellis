import Dependencies._

name := "geotrellis-slick"
libraryDependencies := Seq(
  slick,
  postgresql,
  slf4jApi,
  scalatest % "test")