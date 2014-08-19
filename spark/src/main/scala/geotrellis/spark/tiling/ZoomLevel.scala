package geotrellis.spark.tiling

import geotrellis.spark._
import geotrellis.vector.Extent

// Consists of a isomorphism TileCoord -> TileId
// and an isomporphism TileId -> (Double, Double)
// Should these be broken out, possibly for variance in index schemes (e.g. z-ordering)?
trait ZoomLevel extends TileIdTransform with Serializable {
  val level: Int
  val extent: Extent

  val pixelCols: Int
  val pixelRows: Int
  val tileCols: Int
  val tileRows: Int

  lazy val tileWidth: Double = extent.width / tileCols
  lazy val tileHeight: Double = extent.height / tileRows

  lazy val totalCols: Long = pixelCols.toLong * tileCols
  lazy val totalRows: Long = pixelRows.toLong * tileRows

  def mapToTile(x: Double, y: Double): TileCoord = {
    val tcol =
      ((x - extent.xmin) / extent.width) * tileCols

    val trow =
      ((extent.ymax - y) / extent.height) * tileRows

    new TileCoord(tcol.toInt, trow.toInt)
  }

  def tileExtent(xmin: Int, ymin: Int, xmax: Int, ymax: Int): TileExtent =
    TileExtent(xmin, ymin, xmax, ymax)(this)

  def tileExtent(extent: Extent): TileExtent = { 
    val (llx, lly) = mapToTile(extent.xmin, extent.ymin)
    val (urx, ury) = mapToTile(extent.xmax, extent.ymax)
    tileExtent(llx, ury, urx, lly)
  }

  def extent(tcol: Long, trow: Long): Extent =
    Extent(
      extent.xmin + tcol * tileWidth,
      extent.ymax - (trow + 1) * tileHeight,
      extent.xmin + (tcol + 1) * tileWidth,
      extent.ymax - trow * tileHeight
    )

  def extent(tileId: TileId): Extent = {
    val (tcol,trow) = tileCoord(tileId)
    extent(tcol, trow)
  }
}
