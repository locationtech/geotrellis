package geotrellis.raster.summary.polygonal

import geotrellis.raster._
import geotrellis.raster.rasterize._
import geotrellis.vector._


object MaxSummary extends TilePolygonalSummaryHandler[Int] {
  def handlePartialTile(raster: Raster[Tile], polygon: Polygon): Int = {
    val Raster(tile, _) = raster
    val rasterExtent = raster.rasterExtent
    var max = NODATA
    polygon.foreach(rasterExtent)({ (col: Int, row: Int) =>
      val z = tile.get(col, row)
      if (isData(z) && (z > max || isNoData(max)) ) { max = z }
    })
    max
  }

  def handleFullTile(tile: Tile): Int = {
    var max = NODATA
    tile.foreach { (x: Int) =>
      if (isData(x) && (x > max || isNoData(max))) { max = x }
    }
    max
  }

  def combineResults(rs: Seq[Int]): Int =
    if(rs.isEmpty) NODATA
    else
      rs.reduce { (a, b) =>
        if(isNoData(a)) { b }
        else if(isNoData(b)) { a }
        else { math.max(a, b) }
      }
}

object MaxDoubleSummary extends TilePolygonalSummaryHandler[Double] {
  def handlePartialTile(raster: Raster[Tile], polygon: Polygon): Double = {
    val Raster(tile, _) = raster
    val rasterExtent = raster.rasterExtent
    var max = Double.NaN
    polygon.foreach(rasterExtent)({ (col: Int, row: Int) =>
      val z = tile.getDouble(col, row)
      if (isData(z) && (z > max || isNoData(max))) { max = z }
    })
    max
  }

  def handleFullTile(tile: Tile): Double = {
    var max = Double.NaN
    tile.foreachDouble { (x: Double) =>
      if (isData(x) && (x > max || isNoData(max))) { max = x }
    }
    max
  }

  def combineResults(rs: Seq[Double]) =
    if(rs.isEmpty) Double.NaN
    else
      rs.reduce { (a, b) =>
        if(isNoData(a)) { b }
        else if(isNoData(b)) { a }
        else { math.max(a, b) }
      }
}
