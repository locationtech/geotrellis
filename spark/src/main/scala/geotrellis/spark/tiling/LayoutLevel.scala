package geotrellis.spark.tiling

import geotrellis.spark._
import geotrellis.raster._
import geotrellis.proj4._
import geotrellis.vector.Extent
import geotrellis.vector.reproject._

case class LayoutLevel(id: Int, tileLayout: TileLayout) {
  // TODO: Generalize this
  def up: LayoutLevel =
    LayoutLevel(id - 1,
      TileLayout(tileLayout.tileCols / 2, tileLayout.tileRows / 2,
        tileLayout.pixelCols / 2, tileLayout.pixelRows / 2)
    )

  def down: LayoutLevel =
    LayoutLevel(id + 1,
    TileLayout(tileLayout.tileCols * 2, tileLayout.tileRows * 2,
      tileLayout.pixelCols * 2, tileLayout.pixelRows * 2)
  )
}
