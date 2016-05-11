import Dependencies._

name := "geotrellis-macros"
libraryDependencies <++= scalaVersion {
  case v if v.startsWith("2.10") => Seq(
    "org.scala-lang" %  "scala-reflect" % v,
    "org.scalamacros" %% "quasiquotes" % "2.0.1" // matches spire version of quasiquotes (spire 0.11)
  )
  case v if v.startsWith("2.11") => Seq(
    "org.scala-lang" %  "scala-reflect" % v
  )
}

sourceGenerators in Compile <+= (sourceManaged in Compile).map(Boilerplate.genMacro)

libraryDependencies += "org.spire-math" %% "spire-macros" % "0.11.0"

resolvers += Resolver.sonatypeRepo("snapshots")

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
