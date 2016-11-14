import Dependencies._

name := "python"


libraryDependencies ++= Seq(
  typesafeConfig,
  jts,
  spire,
  monocleCore,
  monocleMacro,
  py4j)

libraryDependencies := {
  CrossVersion.partialVersion(scalaVersion.value) match {
    // if scala 2.11+ is used, add dependency on scala-xml module
    case Some((2, scalaMajor)) if scalaMajor >= 11 =>
      libraryDependencies.value ++ Seq(
        "org.scala-lang.modules" %% "scala-xml" % "1.0.5"
      )
    case _ =>
      libraryDependencies.value
  }
}
