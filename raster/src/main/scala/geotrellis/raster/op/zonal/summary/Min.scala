package geotrellis.raster.op.zonal.summary

import geotrellis.raster._
import geotrellis.vector._
import geotrellis.raster.rasterize._

object Min extends TileIntersectionHandler[Int, Int] {
  def handlePartialTile(pt: PartialTileIntersection): Int = {
    val PartialTileIntersection(tile, _, polygon) = pt
    val rasterExtent = pt.rasterExtent
    var min = NODATA
    Rasterizer.foreachCellByGeometry(polygon, rasterExtent)(
      new Callback {
        def apply(col: Int, row: Int) {
          val z = tile.get(col, row)
          if (isData(z) && (z < min || isNoData(min)) ) { min = z }
        }
      }
    )
    min
  }

  def handleFullTile(ft: FullTileIntersection): Int = {
    var min = NODATA
    ft.tile.foreach { (x: Int) =>
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

object MinDouble extends TileIntersectionHandler[Double, Double] {
  def handlePartialTile(pt: PartialTileIntersection): Double = {
    val PartialTileIntersection(tile, _, polygon) = pt
    val rasterExtent = pt.rasterExtent
    var min = Double.NaN
    Rasterizer.foreachCellByGeometry(polygon, rasterExtent)(
      new Callback {
        def apply(col: Int, row: Int) {
          val z = tile.getDouble(col, row)
          if (isData(z) && (z < min || isNoData(min))) { min = z }
        }
      }
    )

    min
  }

  def handleFullTile(ft: FullTileIntersection): Double = {
    var min = Double.NaN
    ft.tile.foreachDouble { (x: Double) => 
      if (isData(x) && (x < min || isNoData(min))) { min = x  
      }
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
