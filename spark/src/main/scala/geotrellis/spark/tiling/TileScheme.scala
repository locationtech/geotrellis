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

  def zoomLevel(level: Int): ZoomLevel
}

object TilingScheme {
  //* Default tiling scheme for WSG84
  def GEODETIC = new TilingScheme {
    val extent = Extent(-180, -90, 179.99999, 89.99999)
    val tileSize = 512

    def zoomLevel(l: Int): ZoomLevel = 
      new ZoomLevel {
        val level = l

        val tileCols = math.pow(2, l).toLong
        val tileRows = math.pow(2, l - 1).toLong
        val pixelCols = tileSize
        val pixelRows = tileSize
    }
  }
}

trait ZoomLevel extends Serializable {
  val level: Int

  val pixelCols: Int
  val pixelRows: Int
  val tileCols: Int
  val tileRows: Int

  val totalCols: Long = pixelCols.toLong * tileCols
  val totalRows: Long = pixelRows.toLong * tileRows

  def latLonToTile(lat: Double, lon: Double, zoom: Int): TileCoord = {
    val tx =  { 
      (180 + lon) * (tileCols / 360.0)
    }.toLong
    val ty = ((90 + lat) * (tileRows / 180.0)).toLong
    new TileCoord(tx, ty)
  }

  def tileIdsForExtent(extent: Extent): Seq[Long] = {
    val tileExtent = { 
      val ll = latLonToTile(extent.ymin, extent.xmin, level)
      val ur = latLonToTile(extent.ymax, extent.xmax, level)
      new TileExtent(ll.tx, ll.ty, ur.tx, ur.ty)
    }

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
