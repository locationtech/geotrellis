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
    case "META-INF\MANIFEST.MF" => MergeStrategy.discard
    case _ => MergeStrategy.first
  }
}

