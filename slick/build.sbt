import Dependencies._

name := "geotrellis-slick"
libraryDependencies := Seq(
  slickPG,
  postgresql,
  scalatest % "test")
