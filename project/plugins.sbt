resolvers += sbt.Resolver.bintrayIvyRepo("typesafe", "sbt-plugins")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.9.2")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.10")
addSbtPlugin("com.eed3si9n" % "sbt-unidoc" % "0.4.2")
addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.4.0")
addSbtPlugin("de.heikoseeberger" % "sbt-header" % "5.2.0")
// Until we upgrade to Java 11, we can't use JFR with anything later than 0.3.3
addSbtPlugin("pl.project13.scala" % "sbt-jmh" % "0.3.3")
addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "1.0.0")
addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.6.0")
addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.23")
libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.9.0"
