import Dependencies._

name := "geotrellis-spark-testkit"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % Version.spark % "provided",
  "org.apache.hadoop" % "hadoop-client" % Version.hadoop % "provided",
  scalatest, nscalaTime)