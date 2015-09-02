import Dependencies._

name := "geotrellis-macros"
libraryDependencies <++= scalaVersion {
  case "2.10.4" => Seq(
    "org.scala-lang" %  "scala-reflect" % "2.10.4",
    "org.scalamacros" %% "quasiquotes" % "2.0.1",
    "org.spire-math" %% "spire-macros" % "0.10.1"
  )
  case "2.11.5" => Seq(
    "org.scala-lang" %  "scala-reflect" % "2.11.5",
    "org.spire-math" %% "spire-macros" % "0.10.1"
  )
}
resolvers += Resolver.sonatypeRepo("snapshots")
addCompilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full)
