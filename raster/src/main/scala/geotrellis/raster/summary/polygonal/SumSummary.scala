package geotrellis.raster.summary.polygonal

import geotrellis.raster._
import geotrellis.raster.rasterize._
import geotrellis.vector._


object SumSummary extends TilePolygonalSummaryHandler[Long] {
  def handlePartialTile(raster: Raster[Tile], polygon: Polygon): Long = {
    val Raster(tile, _) = raster
    val rasterExtent = raster.rasterExtent
    var sum: Long = 0L

    Rasterizer.foreachCellByGeometry(polygon, rasterExtent) { (col: Int, row: Int) =>
      val z = tile.get(col, row)
      if (isData(z)) { sum = sum + z }
    }

    sum
  }

  def handleFullTile(tile: Tile): Long = {
    var s = 0L
    tile.foreach { (x: Int) => if (isData(x)) s = s + x }
    s
  }

  def combineResults(rs: Seq[Long]) =
    rs.foldLeft(0L)(_+_)
}

object SumDoubleSummary extends TilePolygonalSummaryHandler[Double] {
  def handlePartialTile(raster: Raster[Tile], polygon: Polygon): Double = {
    val Raster(tile, _) = raster
    val rasterExtent = raster.rasterExtent
    var sum = 0.0

    Rasterizer.foreachCellByGeometry(polygon, rasterExtent) { (col: Int, row: Int) =>
      val z = tile.getDouble(col, row)
      if(isData(z)) { sum = sum + z }
    }

    sum
  }

  def handleFullTile(tile: Tile): Double = {
    var s = 0.0
    tile.foreachDouble((x: Double) => if (isData(x)) s = s + x)
    s
  }

  def combineResults(rs: Seq[Double]) =
    rs.foldLeft(0.0)(_+_)
}
