import Dependencies._

name := "geotrellis-spark-etl"
libraryDependencies ++= Seq(
  "com.google.inject" % "guice" % "3.0",
  "com.google.inject.extensions" % "guice-multibindings" % "3.0",
  "org.rogach" %% "scallop" % "0.9.5",
  logging,
  sparkCore % "provided")