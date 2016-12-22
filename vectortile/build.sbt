import Dependencies._

name := "geotrellis-vectortile"

libraryDependencies ++= Seq(
  scalatest % "test",
  "com.trueaccord.scalapb" %% "scalapb-runtime" % "0.5.46"
)
