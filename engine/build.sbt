import Dependencies._

name := "geotrellis-engine"
scalacOptions in compile += "-optimize"
libraryDependencies ++= Seq(
  akkaKernel, akkaRemote, akkaActor, akkaCluster,
  spire, monocleCore, monocleMacro,
  sprayClient // for reading args from URLs,
)