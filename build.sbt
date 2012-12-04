import AssemblyKeys._

assemblySettings

mergeStrategy in assembly <<= (mergeStrategy in assembly) {
  (old) => {
    case "application.conf" => MergeStrategy.concat
    case "META-INF/MANIFEST.MF" => MergeStrategy.discard
    case "META-INF\\MANIFEST.MF" => MergeStrategy.discard
    case _ => MergeStrategy.first
  }
}

seq(lsSettings :_*)

(LsKeys.tags in LsKeys.lsync) :=
Seq("maps", "gis", "geographic", "data", "raster", "processing")

(LsKeys.docsUrl in LsKeys.lsync) := Some(new URL("http://azavea.github.com/geotrellis/getting_started/"))

(description in LsKeys.lsync) :=
"GeoTrellis is an open source geographic data processing engine for high performance applications."
