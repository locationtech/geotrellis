import Dependencies._

name := "geotrellis-spark-testkit"

libraryDependencies ++= Seq(
  sparkCore % Provided ,
  hadoopClient % Provided,
  scalatest,
  chronoscala
)
