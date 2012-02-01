import AssemblyKeys._

import com.jsuereth.sbtsite.SiteKeys

seq(assemblySettings: _*)

seq(site.settings: _*)

seq(ghpages.settings: _*)

git.remoteRepo := "git@github.com:azavea/trellis.git"

seq(Revolver.settings: _*)

// mainClass in (Compile, run) := Some("trellis.rest.WebRunner")

mainClass := Some("trellis.rest.WebRunner")

//autoCompilerPlugins := true

//addCompilerPlugin("com.azavea.math.plugin" %% "optimized-numeric" % "0.2")

//scalacOptions += "-Xplugin:lib/optimized-numeric.jar"

SiteKeys.siteMappings <<=
  (SiteKeys.siteMappings, PamfletKeys.write, PamfletKeys.output) map { (mappings, _, dir) =>
    mappings ++ (dir ** "*.*" x relativeTo(dir))
  }
