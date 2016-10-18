import Dependencies._

name := "geotrellis-vectortile"

libraryDependencies ++= Seq(
  scalatest % "test",
  sparkCore % "provided",
  "com.trueaccord.scalapb" %% "scalapb-runtime" % "0.5.32"
)
