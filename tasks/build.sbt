import AssemblyKeys._

libraryDependencies <+= (sbtVersion) { v =>
  v.split('.').toList match {
    case "0" :: "11" :: "3" :: Nil  =>
       "org.scala-sbt" %%
        "launcher-interface" %
          v % "provided"
    case _ =>
      "org.scala-sbt" %
        "launcher-interface" %
          v % "provided"
  }
}

resolvers <+= sbtResolver

name := "geotrellis-tasks"

organization := "com.azavea.geotrellis"

version := "0.7.0-SNAPSHOT"

//seq(Revolver.settings: _*)

assemblySettings

addCompilerPlugin("com.azavea.math.plugin" %% "optimized-numeric" % "0.1")

mergeStrategy in assembly <<= (mergeStrategy in assembly) {
  (old) => {
    case "application.conf" => MergeStrategy.concat
    case "META-INF/MANIFEST.MF" => MergeStrategy.discard
    case "META-INF\\MANIFEST.MF" => MergeStrategy.discard
    case _ => MergeStrategy.first
  }
}


publishMavenStyle := true

publishTo <<= version { (v: String) =>
      val nexus = "https://oss.sonatype.org/"
      if (v.trim.endsWith("SNAPSHOT"))
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    }

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

licenses := Seq("GPL3" -> url("http://www.gnu.org/licenses/gpl-3.0-standalone.html"))

homepage := Some(url("http://github.com/azavea/geotrellis"))

pomExtra := (
  <scm>
    <url>git@github.com:azavea/geotrellis.git</url>
    <connection>scm:git:git@github.com:azavea/geotrellis.git</connection>
  </scm>
  <developers>
    <developer>
      <id>non</id>
      <name>Erik Osheim</name>
      <url>http://github.com/non/</url>
    </developer>
    <developer>
      <id>josh.marcus</id>
      <name>Josh Marcus</name>
      <url>http://github.com/josh.marcus/</url>
    </developer>
  </developers>
)
