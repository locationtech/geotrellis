import Dependencies._

name := "geotrellis-macros"
libraryDependencies <++= scalaVersion {
  case v if v.startsWith("2.10") => Seq(
    "org.scala-lang" %  "scala-reflect" % v,
    "org.scalamacros" %% "quasiquotes" % "2.0.1"
  )
  case v if v.startsWith("2.11") => Seq(
    "org.scala-lang" %  "scala-reflect" % v
  )
}

libraryDependencies += "org.spire-math" %% "spire-macros" % "0.10.1"

resolvers += Resolver.sonatypeRepo("snapshots")

addCompilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full)
