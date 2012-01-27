import sbt._

object PluginDef extends Build {
  override def projects = Seq(root)
  lazy val root = Project("plugins", file(".")) dependsOn (ghpages, pamflet)

  lazy val ghpages = uri("git://github.com/jsuereth/xsbt-ghpages-plugin.git")
  lazy val pamflet = uri("git://github.com/n8han/pamflet-plugin#0.3.0")
}

// vim: set ts=4 sw=4 et:
