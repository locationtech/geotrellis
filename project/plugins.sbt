resolvers += sbt.Resolver.bintrayIvyRepo("typesafe", "sbt-plugins")

addDependencyTreePlugin
addSbtPlugin("com.eed3si9n"       % "sbt-assembly"    % "2.1.1")
addSbtPlugin("com.github.sbt"     % "sbt-unidoc"      % "0.5.0")
addSbtPlugin("com.timushev.sbt"   % "sbt-updates"     % "0.6.4")
addSbtPlugin("de.heikoseeberger"  % "sbt-header"      % "5.9.0")
addSbtPlugin("pl.project13.scala" % "sbt-jmh"         % "0.4.2")
addSbtPlugin("com.typesafe"       % "sbt-mima-plugin" % "1.1.2")
addSbtPlugin("com.thesamet"       % "sbt-protoc"      % "1.0.6")
addSbtPlugin("ch.epfl.scala"      % "sbt-scalafix"    % "0.10.4")
addSbtPlugin("org.scalameta"      % "sbt-mdoc"        % "2.3.7" )
addSbtPlugin("com.github.sbt"     % "sbt-ci-release"  % "1.5.12")
libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.11.13"
