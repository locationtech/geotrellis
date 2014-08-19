package geotrellis.spark.tiling

import geotrellis.spark._
import geotrellis.vector.Extent

trait ZoomLevel extends Serializable {
  val level: Int
  val extent: Extent

  val pixelCols: Int
  val pixelRows: Int
  val tileCols: Int
  val tileRows: Int

  lazy val totalCols: Long = pixelCols.toLong * tileCols
  lazy val totalRows: Long = pixelRows.toLong * tileRows

  def tileId(tx: Int, ty: Int): TileId
  def tileXY(tileId: TileId): (Int, Int)

  def mapToTile(x: Double, y: Double): TileCoord = {
    val tx =
      ((x - extent.xmin) / extent.width) * tileCols

    val ty =
      ((extent.ymax - y) / extent.height) * tileRows

    new TileCoord(tx.toInt, ty.toInt)
  }

  def extentToTile(extent: Extent, tileSize: Int): TileExtent = {
    val ll = mapToTile(extent.xmin, extent.ymin)
    val ur = mapToTile(extent.xmax, extent.ymax)
    new TileExtent(ll.tx, ll.ty, ur.tx, ur.ty)
  }

  def tileExtentForExtent(extent: Extent): TileExtent = { 
    val ll = mapToTile(extent.xmin, extent.ymin)
    val ur = mapToTile(extent.xmax, extent.ymax)
    new TileExtent(ll.tx, ur.ty, ur.tx, ll.ty)
  }

  def tileIdsForExtent(extent: Extent): Seq[Long] = {
    val tileExtent = tileExtentForExtent(extent)

    val tileInfos =
      for { tcol <- tileExtent.xmin to tileExtent.xmax;
        trow <- tileExtent.ymin to tileExtent.ymax } yield {
        tileId(tcol, trow)
      }

    tileInfos.toSeq
  }

  def extentForTile(tileId: Long): Extent = {
    val (tx, ty) = tileXY(tileId)
    TmsTiling.tileToExtent(tx, ty, level, TmsTiling.DefaultTileSize)
  }
}
