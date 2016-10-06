import Dependencies._

name := "geotrellis-vectortile"

libraryDependencies ++= Seq(
  scalatest % "test",
  "org.apache.spark" %% "spark-core" % "1.6.2",
  "com.trueaccord.scalapb" %% "scalapb-runtime" % "0.5.32"
)
