import Dependencies._

name := "geotrellis-raster"


libraryDependencies ++= Seq(
  typesafeConfig,
  jts,
  spire,
  logging,
  monocleCore,
  monocleMacro,
  openCSV)

libraryDependencies := {
  CrossVersion.partialVersion(scalaVersion.value) match {
    // if scala 2.11+ is used, add dependency on scala-xml module
    case Some((2, scalaMajor)) if scalaMajor >= 11 =>
      libraryDependencies.value ++ Seq(
        "org.scala-lang.modules" %% "scala-xml" % "1.0.6"
      )
    case _ =>
      libraryDependencies.value
  }
}

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
scalacOptions ++= Seq("-optimize", "-language:experimental.macros")
sourceGenerators in Compile += (sourceManaged in Compile).map(Boilerplate.genRaster).taskValue

initialCommands in console :=
  """
  import geotrellis.raster._
  import geotrellis.raster.resample._
  import geotrellis.vector._
  """
