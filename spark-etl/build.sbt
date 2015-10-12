import Dependencies._

name := "geotrellis-spark-etl"
libraryDependencies ++= Seq(
  "org.rogach" %% "scallop" % "0.9.5",
  sparkCore % "provided",
  logging,
  scalatest % "test")