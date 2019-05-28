package geotrellis.layers.file

import java.io.File

import geotrellis.layers.LayerId

object LayerPath {
  def apply(catalogPath: String, layerId: LayerId): String =
    new File(catalogPath, apply(layerId)).getAbsolutePath
  def apply(layerId: LayerId): String = s"${layerId.name}/${layerId.zoom}"
}
