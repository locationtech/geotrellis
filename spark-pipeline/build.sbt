import Dependencies._

name := "geotrellis-spark-pipeline"
libraryDependencies ++= Seq(
  "com.chuusai" %% "shapeless" % "2.3.2",
  "org.clapper" %% "classutil" % "1.1.2",
  "org.clapper" %% "grizzled-scala" % "4.2.0",
  "org.ow2.asm" % "asm" % "5.1",
  "org.ow2.asm" % "asm-commons" % "5.1",
  "org.ow2.asm" % "asm-util" % "5.1",
  "io.circe" %% "circe-core"           % "0.8.0",
  "io.circe" %% "circe-generic"        % "0.8.0",
  "io.circe" %% "circe-generic-extras" % "0.8.0",
  "io.circe" %% "circe-parser"         % "0.8.0",
  "io.circe" %% "circe-optics"         % "0.8.0",
  "org.typelevel" %% "cats" % "0.9.0",
  sparkCore % "provided",
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
