package geotrellis.spark.io.file

import geotrellis.spark._

import java.io.File

object LayerPath {
  def apply(catalogPath: String, layerId: LayerId): String =
    new File(catalogPath, apply(layerId)).getAbsolutePath
  def apply(layerId: LayerId): String = s"${layerId.name}/${layerId.zoom}"
}
