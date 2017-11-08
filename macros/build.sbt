import Dependencies._

name := "geotrellis-macros"

sourceGenerators in Compile += (sourceManaged in Compile).map(Boilerplate.genMacro).taskValue

libraryDependencies ++= Seq(
  spireMacro,
  "org.scala-lang" % "scala-reflect" % scalaVersion.value
)

resolvers += Resolver.sonatypeRepo("snapshots")

