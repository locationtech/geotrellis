resolvers += sbt.Resolver.bintrayIvyRepo("typesafe", "sbt-plugins")

addDependencyTreePlugin
addSbtPlugin("com.eed3si9n"       % "sbt-assembly"    % "2.3.0")
addSbtPlugin("com.github.sbt"     % "sbt-unidoc"      % "0.5.0")
addSbtPlugin("com.timushev.sbt"   % "sbt-updates"     % "0.6.4")
addSbtPlugin("de.heikoseeberger"  % "sbt-header"      % "5.10.0")
addSbtPlugin("pl.project13.scala" % "sbt-jmh"         % "0.4.7")
addSbtPlugin("com.typesafe"       % "sbt-mima-plugin" % "1.1.4")
addSbtPlugin("com.thesamet"       % "sbt-protoc"      % "1.0.7")
addSbtPlugin("ch.epfl.scala"      % "sbt-scalafix"    % "0.13.0")
addSbtPlugin("org.scalameta"      % "sbt-mdoc"        % "2.6.1" )
addSbtPlugin("com.github.sbt"     % "sbt-ci-release"  % "1.7.0")
libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.11.17"
