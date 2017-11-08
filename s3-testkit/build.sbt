import Dependencies._

name := "geotrellis-s3-testkit"
libraryDependencies ++= Seq(
  sparkCore % Provided,
  awsSdkS3,
  spire,
  scalatest
)
