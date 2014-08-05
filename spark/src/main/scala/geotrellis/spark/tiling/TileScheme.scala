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

    def zoomLevelFor(cellSize: CellSize): ZoomLevel = {
      val l = 
        math.max(TmsTiling.zoom(cellSize.width, 512),
          TmsTiling.zoom(cellSize.height, 512))
      zoomLevel(l)
    }

    def zoomLevel(l: Int): ZoomLevel = 
      new ZoomLevel {
        val level = l

        val tileCols = math.pow(2, l).toInt
        val tileRows = math.pow(2, l - 1).toInt
        val pixelCols = 512
        val pixelRows = 512
    }
  }
}

trait ZoomLevel extends Serializable {
  val level: Int

  val pixelCols: Int
  val pixelRows: Int
  val tileCols: Int
  val tileRows: Int

  lazy val totalCols: Long = pixelCols.toLong * tileCols
  lazy val totalRows: Long = pixelRows.toLong * tileRows

  lazy val tileSize: Int = pixelCols // TODO: Remove

  def latLonToTile(lat: Double, lon: Double, zoom: Int): TileCoord = {
    val tx = ((180 + lon) * (tileCols / 360.0)).toLong
    val ty = ((90 + lat) * (tileRows / 180.0)).toLong
    new TileCoord(tx, ty)
  }

  def tileExtentForExtent(extent: Extent): TileExtent = { 
    val ll = latLonToTile(extent.ymin, extent.xmin, level)
    val ur = latLonToTile(extent.ymax, extent.xmax, level)
    new TileExtent(ll.tx, ll.ty, ur.tx, ur.ty)
  }

  def tileIdsForExtent(extent: Extent): Seq[Long] = {
    val tileExtent = tileExtentForExtent(extent)

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
