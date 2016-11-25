import Dependencies._

name := "geotrellis-s3-test"
libraryDependencies ++= Seq(
  sparkCore % "provided",
  awsSdkS3,
  spire,
  scalatest % "test")

fork in Test := false
parallelExecution in Test := false
