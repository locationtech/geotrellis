import Dependencies._

name := "geotrellis-core"

libraryDependencies ++= Seq(
  cats,
  "com.github.mpilquist" %% "simulacrum" % "0.11.0",
  "org.typelevel"        %% "spire"      % "0.14.1"
)
