import sbt._
class MyProjectPlugins(info:ProjectInfo) extends PluginDefinition(info) {
  val scctRepo = "scct-repo" at "http://mtkopone.github.com/scct/maven-repo/"
  lazy val scctPlugin = "reaktor" % "sbt-scct-for-2.8" % "0.1-SNAPSHOT"
  val sbtIdeaRepo = "sbt-idea-repo" at "http://mpeltonen.github.com/maven/"
  val sbtIdea = "com.github.mpeltonen" % "sbt-idea-plugin" % "0.1-SNAPSHOT"

  val repo = "Christoph's Maven Repo" at "http://maven.henkelmann.eu/"
  val junitXml = "eu.henkelmann" % "junit_xml_listener" % "0.2"
  val proguard = "org.scala-tools.sbt" % "sbt-proguard-plugin" % "0.0.5"
}
