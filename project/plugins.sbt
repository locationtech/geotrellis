resolvers += sbt.Resolver.bintrayIvyRepo("typesafe", "sbt-plugins")

addDependencyTreePlugin
addSbtPlugin("com.eed3si9n"       % "sbt-assembly"    % "2.3.1")
addSbtPlugin("com.github.sbt"     % "sbt-unidoc"      % "0.5.0")
addSbtPlugin("com.timushev.sbt"   % "sbt-updates"     % "0.6.4")
addSbtPlugin("de.heikoseeberger"  % "sbt-header"      % "5.10.0")
addSbtPlugin("pl.project13.scala" % "sbt-jmh"         % "0.4.7")
addSbtPlugin("com.typesafe"       % "sbt-mima-plugin" % "1.1.4")
addSbtPlugin("com.thesamet"       % "sbt-protoc"      % "1.0.7")
addSbtPlugin("ch.epfl.scala"      % "sbt-scalafix"    % "0.14.2")
addSbtPlugin("org.scalameta"      % "sbt-mdoc"        % "2.7.1" )
addSbtPlugin("com.github.sbt"     % "sbt-ci-release"  % "1.9.3")
libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.11.17"
