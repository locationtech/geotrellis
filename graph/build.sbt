import Dependencies._

name := "geotrellis-graph"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-graphx" % Version.spark % "provided",
  scalatest % "test")

fork := true
parallelExecution in Test := false
javaOptions ++= List(
  "-Xmx8G",
  s"-Djava.library.path=${Environment.javaGdalDir}",
  "-Dsun.io.serialization.extendedDebugInfo=true"
)