import Dependencies._

name := "geotrellis-vector"
libraryDependencies ++= Seq(
  jts,
  pureconfig,
  sprayJson,
  circeCore,
  circeGeneric,
  circeGenericExtras,
  circeParser,
  apacheMath,
  spire,
  scalatest  % Test,
  scalacheck % Test
)

testOptions in Test += Tests.Argument("-oDF")
