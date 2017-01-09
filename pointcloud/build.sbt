import Dependencies._

name := "geotrellis-pointcloud"

val circeVersion = "0.6.1"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-generic-extras" % circeVersion,
  "io.circe" %% "circe-literal" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  sparkCore % "provided",
  pdal,
  scalatest % "test")

resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots")
)

fork in Test := true
parallelExecution in Test := false

javaOptions += s"-Djava.library.path=${Environment.ldLibraryPath}"
