import Dependencies._

name := "geotrellis-jetty"
libraryDependencies ++= Seq(
  jettyWebapp,
  jerseyBundle,
  slf4jApi,
  asm)