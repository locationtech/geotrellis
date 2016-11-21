import Dependencies._

name := "geotrellis-pdal"
libraryDependencies ++= Seq(
  sparkCore % "provided",
  pdal,
  scalatest % "test")

resolvers += Resolver.bintrayRepo("daunnc", "maven")

fork in Test := false
parallelExecution in Test := false

javaOptions += s"-Djava.library.path=${Environment.javaPdalDir}"
