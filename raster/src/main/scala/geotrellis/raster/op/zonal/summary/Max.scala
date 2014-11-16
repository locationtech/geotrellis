package geotrellis.raster.op.zonal.summary

import geotrellis.raster._
import geotrellis.vector._
import geotrellis.raster.rasterize._

object Max extends TileIntersectionHandler[Int, Int] {
  def handlePartialTile(pt: PartialTileIntersection): Int = {
    val PartialTileIntersection(tile, _, polygon) = pt
    val rasterExtent = pt.rasterExtent
    var max = NODATA
    Rasterizer.foreachCellByGeometry(polygon, rasterExtent)(
      new Callback {
        def apply(col: Int, row: Int) {
          val z = tile.get(col, row)
          if (isData(z) && (z > max || isNoData(max)) ) { max = z }
        }
      }
    )
    max
  }

  def handleFullTile(ft: FullTileIntersection): Int = {
    var max = NODATA
    ft.tile.foreach { (x: Int) =>
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

object MaxDouble extends TileIntersectionHandler[Double, Double] {
  def handlePartialTile(pt: PartialTileIntersection): Double = {
    val PartialTileIntersection(tile, _, polygon) = pt
    val rasterExtent = pt.rasterExtent
    var max = Double.NaN
    Rasterizer.foreachCellByGeometry(polygon, rasterExtent)(
      new Callback {
        def apply(col: Int, row: Int) {
          val z = tile.getDouble(col, row)
          if (isData(z) && (z > max || isNoData(max))) { max = z }
        }
      }
    )

    max
  }

  def handleFullTile(ft: FullTileIntersection): Double = {
    var max = Double.NaN
    ft.tile.foreachDouble { (x: Double) =>
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
