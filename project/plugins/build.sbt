resolvers += "Proguard plugin repo" at "http://siasia.github.com/maven2"

resolvers += Classpaths.typesafeResolver

//libraryDependencies <+= sbtVersion(v => "com.github.siasia" %% "xsbt-proguard-plugin" % ")
addSbtPlugin("com.github.siasia" % "xsbt-proguard-plugin" % "0.1")

addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse" % "1.5.0")

addSbtPlugin("ch.craven" %% "scct-plugin" % "0.2")
