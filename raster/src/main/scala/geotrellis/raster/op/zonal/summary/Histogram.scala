package geotrellis.raster.op.zonal.summary

import geotrellis.raster._
import geotrellis.vector._
import geotrellis.raster.rasterize._
import geotrellis.raster.histogram._

object Histogram extends TileIntersectionHandler[Histogram, Histogram] {
  def handlePartialTile(pt: PartialTileIntersection): Histogram = {
    val PartialTileIntersection(tile, _, polygon) = pt
    val rasterExtent = pt.rasterExtent
    val histogram = FastMapHistogram()
    Rasterizer.foreachCellByGeometry(polygon, rasterExtent) { (col: Int, row: Int) =>
      val z = tile.get(col, row)
      if (isData(z)) histogram.countItem(z, 1)
    }
    histogram
  }

  def handleFullTile(ft: FullTileIntersection): Histogram = {
    val histogram = FastMapHistogram()
    ft.tile.foreach((z: Int) => if (isData(z)) histogram.countItem(z, 1))
    histogram
  }

  def combineResults(rs: Seq[Histogram]): Histogram =
    FastMapHistogram.fromHistograms(rs)
}
