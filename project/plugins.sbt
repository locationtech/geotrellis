resolvers ++= Seq(
  Classpaths.sbtPluginReleases,
  Opts.resolver.sonatypeReleases
)

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.8.2")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.4")

addSbtPlugin("com.eed3si9n" % "sbt-unidoc" % "0.4.0")

addSbtPlugin("com.eed3si9n" % "sbt-dirty-money" % "0.1.0")

addSbtPlugin("me.lessis" % "ls-sbt" % "0.1.3")

addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.3.0")

addSbtPlugin("de.heikoseeberger" % "sbt-header" % "1.7.0")

addSbtPlugin("pl.project13.scala" % "sbt-jmh" % "0.2.27")
