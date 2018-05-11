import Dependencies._

name := "geotrellis-spark"
libraryDependencies ++= Seq(
  sparkCore % Provided,
  hadoopClient % Provided,
  "com.google.uzaygezen" % "uzaygezen-core" % "0.2",
  "org.scalaj" %% "scalaj-http" % "2.4.0",
  avro,
  spire,
  monocleCore, monocleMacro,
  chronoscala,
  catsCore,
  catsEffect,
  fs2Core,
  fs2Io,
  scalatest % Test,
  logging,
  scaffeine
)

mimaPreviousArtifacts := Set(
  "org.locationtech.geotrellis" %% "geotrellis-spark" % Version.previousVersion
)

fork in Test := false
parallelExecution in Test := false

testOptions in Test += Tests.Argument("-oDF")

initialCommands in console :=
  """
  import geotrellis.raster._
  import geotrellis.vector._
  import geotrellis.proj4._
  import geotrellis.spark._
  import geotrellis.spark.util._
  import geotrellis.spark.tiling._
  """
