import Dependencies._

name := "geotrellis-spark"
libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % Version.spark % "provided",
  "org.apache.hadoop" % "hadoop-client" % Version.hadoop % "provided",
  "de.javakaffee" % "kryo-serializers" % "0.37" exclude("com.esotericsoftware.kryo", "kryo"),
  "com.esotericsoftware.kryo" % "kryo" % "2.21",
  "com.google.uzaygezen" % "uzaygezen-core" % "0.2",
  logging,
  avro,
  spire,
  monocleCore, monocleMacro,
  nscalaTime,
  scalazStream,
  scalatest % "test")

// must use this method of import to avoid cyclic dependency errors
internalDependencyClasspath in Test <++=
  exportedProducts in Compile in LocalProject("raster-testkit")

internalDependencyClasspath in Test <++=
  exportedProducts in Compile in LocalProject("spark-testkit")

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
  import geotrellis.spark.util._
  import geotrellis.spark.tiling._
  """
