import Dependencies._

name := "geotrellis-vectortile-spark"

libraryDependencies ++= Seq(
  sparkCore % "provided",
  avro,
  scalatest % "test"
)
