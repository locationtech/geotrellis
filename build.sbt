import AssemblyKeys._
import com.jsuereth.sbtsite.SiteKeys

seq(assemblySettings: _*)

seq(site.settings: _*)

seq(ghpages.settings: _*)

git.remoteRepo := "git@github.com:azavea/trellis.git"

mainClass := Some("trellis.rest.WebRunner")

SiteKeys.siteMappings <<=
  (SiteKeys.siteMappings, PamfletKeys.write, PamfletKeys.output) map { (mappings, _, dir) =>
    mappings ++ (dir ** "*.*" x relativeTo(dir))
  }


