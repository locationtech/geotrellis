import Dependencies._

name := "geotrellis-vectortile"

libraryDependencies ++= Seq(
  scalatest % "test",
  "org.apache.spark" %% "spark-core" % Version.spark % "provided",
  "com.trueaccord.scalapb" %% "scalapb-runtime" % "0.5.32"
)
