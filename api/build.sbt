
import Dependencies._

name := "api"
libraryDependencies ++= Seq(
  sparkCore % "provided",
  logging,
  avro,
  spire,
  monocleCore, monocleMacro,
  chronoscala,
  scalazStream,
  scalatest % "test"
)

// must use this method of import to avoid cyclic dependency errors
internalDependencyClasspath in Test <++=
  exportedProducts in Compile in LocalProject("raster-testkit")

internalDependencyClasspath in Test <++=
  exportedProducts in Compile in LocalProject("spark-testkit")

fork in Test := false
parallelExecution in Test := false
