import Dependencies._

name := "geotrellis-s3"
libraryDependencies ++= Seq(
  sparkCore % "provided",
  awsSdkS3,
  spire,
  scaffeine,
  scalatest % "test"
)

fork in Test := false
parallelExecution in Test := false

testOptions in Test += Tests.Argument("-oDF")

mimaPreviousArtifacts := Set(
  "org.locationtech.geotrellis" %% "geotrellis-s3" % Version.previousVersion
)

initialCommands in console :=
  """
  import geotrellis.raster._
  import geotrellis.vector._
  import geotrellis.proj4._
  import geotrellis.spark._
  import geotrellis.spark.util._
  import geotrellis.spark.tiling._
  """

test in assembly := {}

assemblyShadeRules in assembly := {
  val shadePackage = "com.azavea.shaded.demo"
  Seq(
    ShadeRule.rename("com.google.common.**" -> s"$shadePackage.google.common.@1")
      .inLibrary("com.azavea.geotrellis" %% "geotrellis-cassandra" % Version.geotrellis).inAll,
    ShadeRule.rename("io.netty.**" -> s"$shadePackage.io.netty.@1")
      .inLibrary("com.azavea.geotrellis" %% "geotrellis-hbase" % Version.geotrellis).inAll,
    ShadeRule.rename("com.fasterxml.jackson.**" -> s"$shadePackage.com.fasterxml.jackson.@1")
      .inLibrary("com.networknt" % "json-schema-validator" % "0.1.7").inAll,
    ShadeRule.rename("org.apache.avro.**" -> s"$shadePackage.org.apache.avro.@1")
      .inLibrary("com.azavea.geotrellis" %% "geotrellis-spark" % Version.geotrellis).inAll
  )
}

assemblyMergeStrategy in assembly := {
  case s if s.startsWith("META-INF/services") => MergeStrategy.concat
  case "reference.conf" | "application.conf"  => MergeStrategy.concat
  case "META-INF/MANIFEST.MF" | "META-INF\\MANIFEST.MF" => MergeStrategy.discard
  case "META-INF/ECLIPSEF.RSA" | "META-INF/ECLIPSEF.SF" => MergeStrategy.discard
  case "META-INF/ECLIPSE_.RSA" | "META-INF/ECLIPSE_.SF" => MergeStrategy.discard
  case s if s.startsWith("META-INF/") && s.endsWith("SF") => MergeStrategy.discard
  case s if s.startsWith("META-INF/") && s.endsWith("RSA") => MergeStrategy.discard
  case s if s.startsWith("META-INF/") && s.endsWith("DSA") => MergeStrategy.discard
  case _ => MergeStrategy.first
}
