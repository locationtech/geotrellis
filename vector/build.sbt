import Dependencies._

name := "geotrellis-vector"
libraryDependencies ++= Seq(
  jts,
  pureconfig,
  sprayJson,
  apacheMath,
  spire,
  scalatest  % Test,
  scalacheck % Test
)

