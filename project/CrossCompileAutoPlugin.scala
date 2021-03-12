import sbt._
import sbt.Keys._

object CrossCompileAutoPlugin extends AutoPlugin {

  override def trigger: sbt.PluginTrigger = allRequirements

  override def projectSettings: Seq[Def.Setting[_]] =
    Seq(
      libraryDependencies ++= (CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, 13)) => Seq.empty
        case Some((2, 11 | 12)) => Seq(
          compilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full),
          "org.scala-lang.modules" %% "scala-collection-compat" % "2.4.2"
        )
        case x => sys.error(s"Encountered unsupported Scala version ${x.getOrElse("undefined")}")
      }),
      Compile / scalacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, 13)) => Seq(
          "-Ymacro-annotations", // replaces paradise in 2.13
          "-Wconf:cat=deprecation&msg=Auto-application:silent" // there are many of these, silence until fixed
        )
        case Some((2, 11 | 12)) => Seq("-Ypartial-unification") // required by Cats
        case x => sys.error(s"Encountered unsupported Scala version ${x.getOrElse("undefined")}")
      })
    )
}