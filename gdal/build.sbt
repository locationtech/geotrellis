import Dependencies._

name := "geotrellis-gdal"
libraryDependencies ++= Seq(
  "org.apache.hadoop" % "hadoop-client" % Version.hadoop % "provided",
  "org.apache.spark" %% "spark-core" % Version.spark % "provided",
  "org.gdal"         % "gdal"       % "1.11.1",
  "com.github.scopt" %% "scopt" % "3.3.0",
  scalatest % "test")

fork in test := false
parallelExecution in Test := false

javaOptions += s"-Djava.library.path=${Environment.javaGdalDir}"
