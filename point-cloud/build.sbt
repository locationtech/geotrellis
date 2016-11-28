import Dependencies._

name := "geotrellis-points"
libraryDependencies ++= Seq(
  sparkCore % "provided",
  pdal,
  scalatest % "test")

resolvers += Resolver.bintrayRepo("daunnc", "maven")

fork in Test := true
parallelExecution in Test := false

javaOptions += s"-Djava.library.path=${Environment.javaPdalDir}"
