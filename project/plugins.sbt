resolvers += sbt.Resolver.bintrayIvyRepo("typesafe", "sbt-plugins")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.10.0-RC1")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.15.0")
addSbtPlugin("com.eed3si9n" % "sbt-unidoc" % "0.4.3")
addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.5.1")
addSbtPlugin("de.heikoseeberger" % "sbt-header" % "5.5.0")
addSbtPlugin("pl.project13.scala" % "sbt-jmh" % "0.3.7")
addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "1.0.0")
addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.7.0")
addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.34")
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.9.19")
libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.9.8"
