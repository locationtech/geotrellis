import Dependencies._

name := "geotrellis-slick"
libraryDependencies := Seq(
  slickPG,
  postgresql,
  slf4jApi,
  scalatest % "test")
