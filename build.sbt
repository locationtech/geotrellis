import AssemblyKeys._

import com.jsuereth.sbtsite.SiteKeys

import com.typesafe.startscript.StartScriptPlugin

seq(site.settings: _*)

seq(ghpages.settings: _*)

seq(Revolver.settings: _*)

assemblySettings

git.remoteRepo := "git@github.com:azavea/geotrellis.git"

//mainClass := Some("geotrellis.rest.WebRunner")

addCompilerPlugin("com.azavea.math.plugin" %% "optimized-numeric" % "0.1")

seq(StartScriptPlugin.startScriptForClassesSettings: _*)

mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) =>
  {
    case "application.conf" => MergeStrategy.concat
    case "org/apache/commons/collections/FastHashMap$EntrySet.class" => MergeStrategy.first
    case "FastHashMap$EntrySet.class" => MergeStrategy.first
    case x => MergeStrategy.first
  }
}
