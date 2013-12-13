// libraryDependencies ++= Seq(
//   "org.jacoco" % "org.jacoco.core" % "0.5.9.201207300726" artifacts(Artifact("org.jacoco.core", "jar", "jar")),
//   "org.jacoco" % "org.jacoco.report" % "0.5.9.201207300726" artifacts(Artifact("org.jacoco.report", "jar", "jar")))

resolvers ++= Seq(
  "less is" at "http://repo.lessis.me",
  "coda" at "http://repo.codahale.com")

addSbtPlugin("de.johoop" % "jacoco4sbt" % "2.1.2")

addSbtPlugin("me.lessis" % "ls-sbt" % "0.1.2")

addSbtPlugin("io.spray" % "sbt-revolver" % "0.7.1")
