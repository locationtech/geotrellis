resolvers += Classpaths.typesafeResolver

resolvers += Resolver.url("sbt-plugin-releases",
  new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns)

//addSbtPlugin("cc.spray" % "sbt-revolver" % "0.6.0-SNAPSHOT" from "http://plastic-idolatry.com/jars/sbt-revolver-0.6.0-SNAPSHOT.jar")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.8.3")
