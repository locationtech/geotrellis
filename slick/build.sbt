import Dependencies._

name := "geotrellis-slick"
libraryDependencies := Seq(
  slickPG,
  slf4jApi,
  scalatest % "test")
