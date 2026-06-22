resolvers += sbt.Resolver.bintrayIvyRepo("typesafe", "sbt-plugins")

addDependencyTreePlugin
addSbtPlugin("com.eed3si9n"       % "sbt-assembly"    % "2.3.1")
addSbtPlugin("com.github.sbt"     % "sbt-unidoc"      % "0.6.1")
addSbtPlugin("com.timushev.sbt"   % "sbt-updates"     % "0.6.4")
addSbtPlugin("com.github.sbt"     % "sbt-header"      % "5.11.0")
addSbtPlugin("pl.project13.scala" % "sbt-jmh"         % "0.4.8")
addSbtPlugin("com.typesafe"       % "sbt-mima-plugin" % "1.1.6")
addSbtPlugin("com.thesamet"       % "sbt-protoc"      % "1.0.8")
addSbtPlugin("ch.epfl.scala"      % "sbt-scalafix"    % "0.14.7")
addSbtPlugin("org.scalameta"      % "sbt-mdoc"        % "2.9.0" )
addSbtPlugin("com.github.sbt"     % "sbt-ci-release"  % "1.11.2")
libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.11.20"
