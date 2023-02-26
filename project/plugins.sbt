resolvers += sbt.Resolver.bintrayIvyRepo("typesafe", "sbt-plugins")

addDependencyTreePlugin
addSbtPlugin("com.eed3si9n"       % "sbt-assembly"    % "1.2.0")
addSbtPlugin("com.github.sbt"     % "sbt-unidoc"      % "0.5.0")
addSbtPlugin("com.timushev.sbt"   % "sbt-updates"     % "0.6.1")
addSbtPlugin("de.heikoseeberger"  % "sbt-header"      % "5.6.5")
addSbtPlugin("pl.project13.scala" % "sbt-jmh"         % "0.4.2")
addSbtPlugin("com.typesafe"       % "sbt-mima-plugin" % "1.0.1")
addSbtPlugin("com.thesamet"       % "sbt-protoc"      % "1.0.6")
addSbtPlugin("ch.epfl.scala"      % "sbt-scalafix"    % "0.9.34")
addSbtPlugin("org.scalameta"      % "sbt-mdoc"        % "2.2.19" )
addSbtPlugin("com.github.sbt"     % "sbt-ci-release"  % "1.5.10")
libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.11.13"
