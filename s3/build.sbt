import Dependencies._

name := "geotrellis-s3"
libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % Version.spark % "provided",
  awsSdkS3,
  spire,
  scalatest % "test")

fork in Test := false
parallelExecution in Test := false

javaOptions ++= List(
  "-Xmx2G",
  "-XX:MaxPermSize=384m",
  s"-Djava.library.path=${Environment.javaGdalDir}",
  "-Dsun.io.serialization.extendedDebugInfo=true")

initialCommands in console :=
  """
  import geotrellis.raster._
  import geotrellis.vector._
  import geotrellis.proj4._
  import geotrellis.spark._
  import geotrellis.spark.utils._
  import geotrellis.spark.tiling._
  """
