import Dependencies._

name := "geotrellis-s3"
libraryDependencies ++= Seq(
  "org.apache.accumulo" % "accumulo-core" % Version.accumulo
    exclude("org.jboss.netty", "netty")
    exclude("org.apache.hadoop", "hadoop-client"),
  "org.apache.spark" %% "spark-core" % Version.spark % "provided",
  "org.apache.hadoop" % "hadoop-client" % Version.hadoop % "provided",
  "de.javakaffee" % "kryo-serializers" % "0.27" exclude("com.esotericsoftware.kryo", "kryo"),
  "com.esotericsoftware.kryo" % "kryo" % "2.21",
  "com.google.uzaygezen" % "uzaygezen-core" % "0.2",
  logging, awsSdkS3, avro,
  spire,
  monocleCore, monocleMacro,
  nscalaTime,
  scalazStream)

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

assemblyMergeStrategy in assembly := {
  case "reference.conf" => MergeStrategy.concat
  case "application.conf" => MergeStrategy.concat
  case "META-INF/MANIFEST.MF" => MergeStrategy.discard
  case "META-INF\\MANIFEST.MF" => MergeStrategy.discard
  case _ => MergeStrategy.first
}
