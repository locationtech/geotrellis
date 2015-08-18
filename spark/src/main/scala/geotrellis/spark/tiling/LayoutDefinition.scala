package geotrellis.spark.tiling

import geotrellis.raster.{RasterExtent, TileLayout}
import geotrellis.vector.Extent

class LayoutDefinition(val extent: Extent, val tileLayout: TileLayout) {
  lazy val rasterExtent = RasterExtent(extent, tileLayout.totalCols, tileLayout.totalRows)
  lazy val mapTransform = MapKeyTransform(extent, tileLayout.layoutDimensions)

  def tileCols = tileLayout.tileCols
  def tileRows = tileLayout.tileRows
}

object LayoutDefinition {
  /**
   * Divides given RasterExtent into a TileLayout given a required tileSize.
   * Since padding may be required on the lower/right tiles to preserve the original resolution of the
   * raster a new Extent is returned, covering the padding.
   */
  def apply(rasterExtent: RasterExtent, tileSize: Int): LayoutDefinition = {
    val extent = rasterExtent.extent
    val cellSize = rasterExtent.cellSize
    val totalPixelWidth = extent.width / cellSize.width
    val totalPixelHeight = extent.height / cellSize.height
    val tileLayoutCols = (totalPixelWidth / tileSize).ceil.toInt
    val tileLayoutRows = (totalPixelHeight / tileSize).ceil.toInt

    val layout = TileLayout(tileLayoutCols, tileLayoutRows, tileSize, tileSize)
    // we may have added padding on the lower/right border, need to compensate for that in new extent
    val layoutExtent = Extent(
    extent.xmin,
    extent.ymin,
    extent.xmin + layout.totalCols * cellSize.width,
    extent.ymin + layout.totalRows * cellSize.height
    )

    new LayoutDefinition(layoutExtent, layout)
  }

  def apply(extent: Extent, layout: TileLayout) =
    new LayoutDefinition(extent, layout)
}
