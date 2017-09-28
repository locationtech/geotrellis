import Dependencies._

name := "geotrellis-macros"

sourceGenerators in Compile += (sourceManaged in Compile).map(Boilerplate.genMacro).taskValue

libraryDependencies ++= Seq(
  "org.spire-math" %% "spire-macros" % Version.spire,
  "org.scala-lang" % "scala-reflect" % scalaVersion.value
)

resolvers += Resolver.sonatypeRepo("snapshots")

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
