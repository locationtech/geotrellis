import Dependencies._

name := "geotrellis-gdal"
libraryDependencies ++= Seq(
  "org.gdal"         % "gdal"       % "1.11.1",
  "com.github.scopt" %% "scopt" % "3.3.0",
  scalatest % "test")

fork in test := true
javaOptions += s"-Djava.library.path=${Environment.javaGdalDir}"