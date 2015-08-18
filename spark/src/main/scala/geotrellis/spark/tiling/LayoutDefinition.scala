package geotrellis.spark.tiling

import geotrellis.raster.{RasterExtent, TileLayout}
import geotrellis.vector.Extent

case class LayoutDefinition(extent: Extent, tileLayout: TileLayout) extends Product {
  lazy val rasterExtent = RasterExtent(extent, tileLayout.totalCols, tileLayout.totalRows)
  lazy val mapTransform = MapKeyTransform(extent, tileLayout.layoutDimensions)

  def tileCols = tileLayout.tileCols
  def tileRows = tileLayout.tileRows
  def layoutCols = tileLayout.layoutCols
  def layoutRows = tileLayout.layoutCols
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

    LayoutDefinition(layoutExtent, layout)
  }
}
