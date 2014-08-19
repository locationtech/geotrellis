package geotrellis.spark.tiling

import geotrellis.spark._
import geotrellis.raster._
import geotrellis.proj4._
import geotrellis.vector.Extent
import geotrellis.vector.reproject._

case class TmsTilingScheme(crs: CRS, tileSize: Int) extends TilingScheme {
  val extent = {
    val ll = Extent(-180, -90, 179.99999, 89.99999)
    if(crs != LatLng) { ll.reproject(LatLng, crs) } else ll
  }

  def zoomLevelFor(cellSize: CellSize): ZoomLevel = {
    val l =
      math.max(
        TmsTiling.zoom(cellSize.width, 512),
        TmsTiling.zoom(cellSize.height, 512)
      )
    zoomLevel(l)
  }

  def zoomLevel(l: Int): ZoomLevel =
    new TmsZoomLevel(l, tileSize, extent)
}

class TmsZoomLevel(val level: Int, tileSize: Int, val extent: Extent) extends ZoomLevel {
  val tileCols = math.pow(2, level).toInt
  val tileRows = math.pow(2, level - 1).toInt
  val pixelCols = tileSize
  val pixelRows = tileSize

  def tileId(tx: Int, ty: Int): TileId = 
    (ty * tileCols) + tx

  def tileXY(tileId: TileId): (Int, Int) = {
    val ty = tileId / tileRows
    val tx = tileId - (ty * tileRows)
    (tx.toInt, ty.toInt)
  }
}
