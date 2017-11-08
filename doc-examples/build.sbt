import Dependencies._

name := "geotrellis-doc-examples"
libraryDependencies ++= Seq(
  sparkCore,
  logging,
  scalatest % Test
)
