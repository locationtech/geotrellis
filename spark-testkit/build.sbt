import Dependencies._

name := "geotrellis-spark-testkit"

libraryDependencies ++= Seq(
  sparkCore % "provided",
  hadoopClient % "provided",
  scalatest, chronoscala)