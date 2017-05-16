import Dependencies._

name := "geotrellis-spark-pipeline"
libraryDependencies ++= Seq(
  "com.github.fge" % "json-schema-validator" % "2.2.6",
  sparkCore % "provided",
  scalatest % "test")

test in assembly := {}

assemblyMergeStrategy in assembly := {
  case "reference.conf" => MergeStrategy.concat
  case "application.conf" => MergeStrategy.concat
  case "META-INF/MANIFEST.MF" => MergeStrategy.discard
  case "META-INF\\MANIFEST.MF" => MergeStrategy.discard
  case "META-INF/ECLIPSEF.RSA" => MergeStrategy.discard
  case "META-INF/ECLIPSEF.SF" => MergeStrategy.discard
  case _ => MergeStrategy.first
}
