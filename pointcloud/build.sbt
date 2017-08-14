import Dependencies._

name := "geotrellis-pointcloud"

libraryDependencies ++= Seq(
  pdalScala,
  sparkCore % "provided",
  scalatest % "test"
)

resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots")
)

fork in Test := true
parallelExecution in Test := false
connectInput in Test := true

javaOptions += s"-Djava.library.path=${Environment.ldLibraryPath}"
