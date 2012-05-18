resolvers += Classpaths.typesafeResolver

resolvers += Resolver.url("sbt-plugin-releases",
  new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns)

addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse" % "1.5.0")

//addSbtPlugin("ch.craven" %% "scct-plugin" % "0.2")

addSbtPlugin("cc.spray" % "sbt-revolver" % "0.6.0-SNAPSHOT" from "http://plastic-idolatry.com/jars/sbt-revolver-0.6.0-SNAPSHOT.jar")

addSbtPlugin("com.typesafe.startscript" % "xsbt-start-script-plugin" % "0.5.1")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.8.0")

