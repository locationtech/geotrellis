import Dependencies._

name := "geotrellis-slick"
libraryDependencies := Seq(
  slick exclude("org.slf4j", "slf4j-api"),
  slickPG,
  postgresql,
  slf4jApi,
  scalatest % "test")
