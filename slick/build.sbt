import Dependencies._

name := "geotrellis-slick"
libraryDependencies := Seq(
  slick,
  slickPG,
  postgresql,
  slf4jApi,
  scalatest % "test")
