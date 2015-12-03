package geotrellis.raster.op.zonal.summary

import geotrellis.raster._
import geotrellis.vector._
import geotrellis.raster.rasterize._

object Min extends TileIntersectionHandler[Int] {
  def handlePartialTile(raster: Raster, polygon: Polygon): Int = {
    val Raster(tile, _) = raster
    val rasterExtent = raster.rasterExtent
    var min = NODATA
    Rasterizer.foreachCellByGeometry(polygon, rasterExtent) { (col: Int, row: Int) =>
      val z = tile.get(col, row)
      if (isData(z) && (z < min || isNoData(min)) ) { min = z }
    }
    min
  }

  def handleFullTile(tile: Tile): Int = {
    var min = NODATA
    tile.foreach { (x: Int) =>
      if (isData(x) && (x < min || isNoData(min))) { min = x }
    }
    min
  }

  def combineResults(rs: Seq[Int]): Int =
    if(rs.isEmpty) NODATA
    else
      rs.reduce { (a, b) =>
        if(isNoData(a)) { b }
        else if(isNoData(b)) { a }
        else { math.min(a, b) }
      }
}

object MinDouble extends TileIntersectionHandler[Double] {
  def handlePartialTile(raster: Raster, polygon: Polygon): Double = {
    val Raster(tile, _) = raster
    val rasterExtent = raster.rasterExtent
    var min = Double.NaN
    Rasterizer.foreachCellByGeometry(polygon, rasterExtent) { (col: Int, row: Int) =>
      val z = tile.getDouble(col, row)
      if (isData(z) && (z < min || isNoData(min))) { min = z }
    }

    min
  }

  def handleFullTile(tile: Tile): Double = {
    var min = Double.NaN
    tile.foreachDouble { (x: Double) =>
      if (isData(x) && (x < min || isNoData(min))) { min = x }
    }
    min
  }

  def combineResults(rs: Seq[Double]): Double =
    if(rs.isEmpty) Double.NaN
    else
      rs.reduce { (a, b) =>
        if(isNoData(a)) { b }
        else if(isNoData(b)) { a }
        else { math.min(a, b) }
      }
}
