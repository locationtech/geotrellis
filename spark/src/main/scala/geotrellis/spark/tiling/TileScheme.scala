package geotrellis.spark.tiling

import geotrellis.raster._
import geotrellis.vector.Extent

// trait TileCoordinate {
//   def toLong
// }

// trait TilingScheme[T <: TileCoordinate] {

// }

// case class TmsCoordinate(x: Int, y: Int, z: Int) extends TileCoordinate {

// }

// object TmsTilingScheme extends TilingScheme[TmsCoordinate] {

// }

trait TilingScheme {
  val extent: Extent

  def zoomLevelFor(cellSize: CellSize): ZoomLevel
}

trait ZoomLevel extends Serializable {
  val level: Int

  val pixelCols: Int
  val pixelRows: Int
  val tileCols: Int
  val tileRows: Int

  val totalCols: Long = pixelCols.toLong * tileCols
  val totalRows: Long = pixelRows.toLong * tileRows

  def tileIdsForExtent(extent: Extent): Seq[Long] = {
    val tileSize = TmsTiling.DefaultTileSize
    val tileExtent =
      TmsTiling.extentToTile(
        extent,
        level,
        tileSize
      )
    val tileInfos =
      for { tcol <- tileExtent.xmin to tileExtent.xmax;
        trow <- tileExtent.ymin to tileExtent.ymax } yield {
        TmsTiling.tileId(tcol, trow, level)
      }

    tileInfos.toSeq
  }

  def extentForTile(tileId: Long): Extent =
    TmsTiling.tileToExtent(tileId, level, TmsTiling.DefaultTileSize)
}

object DefaultTilingScheme extends TilingScheme {

}
