resolvers += Classpaths.typesafeResolver

resolvers += Resolver.url("sbt-plugin-releases",
  new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns)

addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse" % "1.5.0")

//addSbtPlugin("ch.craven" %% "scct-plugin" % "0.2")

addSbtPlugin("cc.spray" % "sbt-revolver" % "0.6.0-SNAPSHOT" from "http://n0d.es/jars/sbt-revolver-0.6.0-SNAPSHOT.jar")
