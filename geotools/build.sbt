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

name := "geotrellis-geotools"

organization := "com.azavea.geotrellis"

version := "0.8.1-RC1"

assemblySettings

mergeStrategy in assembly <<= (mergeStrategy in assembly) {
  (old) => {
    case "reference.conf" => MergeStrategy.concat
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

homepage := Some(url("http://github.com/geotrellis/geotrellis"))

pomExtra := (
<scm>
  <url>git@github.com:geotrellis/geotrellis.git</url>
  <connection>scm:git:git@github.com:geotrellis/geotrellis.git</connection>
</scm>
<developers>
  <developer>
    <id>joshmarcus</id>
    <name>Josh Marcus</name>
    <url>http://github.com/joshmarcus/</url>
  </developer>
  <developer>
    <id>lossyrob</id>
    <name>Rob Emanuele</name>
    <url>http://github.com/lossyrob/</url>
  </developer>
</developers>
)
