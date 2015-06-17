package geotrellis.raster.op.zonal.summary

import geotrellis.raster._
import geotrellis.vector._
import geotrellis.raster.rasterize._

object Sum extends TileIntersectionHandler[Long, Long] {
  def handlePartialTile(pt: PartialTileIntersection): Long = {
    val PartialTileIntersection(tile, _, polygon) = pt
    val rasterExtent = pt.rasterExtent
    var sum: Long = 0L

    Rasterizer.foreachCellByGeometry(polygon, rasterExtent) { (col: Int, row: Int) =>
      val z = tile.get(col, row)
      if (isData(z)) { sum = sum + z }
    }

    sum
  }

  def handleFullTile(ft: FullTileIntersection): Long = {
    var s = 0L
    ft.tile.foreach((x: Int) => if (isData(x)) s = s + x)
    s
  }

  def combineResults(rs: Seq[Long]) =
    rs.foldLeft(0L)(_+_)
}

object SumDouble extends TileIntersectionHandler[Double, Double] {
  def handlePartialTile(pt: PartialTileIntersection): Double = {
    val PartialTileIntersection(tile, _, polygon) = pt
    val rasterExtent = pt.rasterExtent
    var sum = 0.0

    Rasterizer.foreachCellByGeometry(polygon, rasterExtent) { (col: Int, row: Int) =>
      val z = tile.getDouble(col, row)
      if(isData(z)) { sum = sum + z }
    }

    sum
  }

  def handleFullTile(ft: FullTileIntersection): Double = {
    var s = 0.0
    ft.tile.foreachDouble((x: Double) => if (isData(x)) s = s + x)
    s
  }

  def combineResults(rs: Seq[Double]) =
    rs.foldLeft(0.0)(_+_)
}
