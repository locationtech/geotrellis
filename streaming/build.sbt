import Dependencies._

name := "geotrellis-streaming"
libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-streaming" % Version.spark,
  sparkCore % "provided",
  logging,
  scalatest % "test"
)

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
