import Dependencies._

name := "geotrellis-s3-testkit"
libraryDependencies ++= Seq(
  sparkCore % "provided",
  awsSdkS3,
  spire,
  logging,
  scalatest
)
