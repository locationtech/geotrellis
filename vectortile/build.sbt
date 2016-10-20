import Dependencies._

name := "geotrellis-vectortile"

libraryDependencies ++= Seq(
  scalatest % "test",
  // We need to use HBase protobuf version due to compatibility issues
  "com.trueaccord.scalapb" %% "scalapb-runtime" % "0.5.32" exclude ("com.google.protobuf", "protobuf-java"),
  "com.google.protobuf" % "protobuf-java" % "2.5.0"
)
