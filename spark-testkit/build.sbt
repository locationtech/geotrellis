import Dependencies._

name := "geotrellis-spark-testkit"

libraryDependencies ++= Seq(
  sparkCore % Provided,
  sparkSQL % Provided,
  hadoopClient % Provided,
  scalatest,
  chronoscala
)
