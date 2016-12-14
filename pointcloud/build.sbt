import Dependencies._

name := "geotrellis-pointcloud"
libraryDependencies ++= Seq(
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
