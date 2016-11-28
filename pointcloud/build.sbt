import Dependencies._

name := "geotrellis-pointcloud"
libraryDependencies ++= Seq(
  sparkCore % "provided",
  pdal,
  scalatest % "test")

resolvers += Resolver.bintrayRepo("daunnc", "maven")

fork in Test := true
parallelExecution in Test := false

javaOptions += s"-Djava.library.path=${Environment.ldLibraryPath}"
