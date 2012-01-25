resolvers += "Proguard plugin repo" at "http://siasia.github.com/maven2"

resolvers += Classpaths.typesafeResolver

resolvers += Resolver.url("sbt-plugin-releases",
  new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns)

//libraryDependencies <+= sbtVersion(v => "com.github.siasia" %% "xsbt-proguard-plugin" % ")

addSbtPlugin("com.github.siasia" % "xsbt-proguard-plugin" % "0.1")

addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse" % "1.5.0")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.7.2")

//addSbtPlugin("ch.craven" %% "scct-plugin" % "0.2")
