package geotrellis.layers

import geotrellis.vector.Extent
import geotrellis.tiling.LayoutDefinition
import geotrellis.layers.index.KeyIndex


package object cog {
  val GTKey     = "GT_KEY"
  val Extension = "tiff"

  object COGAttributeStore {
    object Fields {
      val metadataBlob = "metadata"
      val metadata = "metadata"
      val header   = "header"
    }
  }

  implicit class withExtentMethods(extent: Extent) {
    def bufferByLayout(layout: LayoutDefinition): Extent =
      Extent(
        extent.xmin + layout.cellwidth / 2,
        extent.ymin + layout.cellheight / 2,
        extent.xmax - layout.cellwidth / 2,
        extent.ymax - layout.cellheight / 2
      )
  }
}
