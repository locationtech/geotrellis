import Dependencies._

name := "geotrellis-macros"

libraryDependencies <+= scalaVersion { v => "org.scala-lang" %  "scala-reflect" % v }

sourceGenerators in Compile <+= (sourceManaged in Compile).map(Boilerplate.genMacro)

libraryDependencies += "org.spire-math" %% "spire-macros" % "0.11.0"

resolvers += Resolver.sonatypeRepo("snapshots")

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
