package geotrellis.raster.summary.polygonal

import geotrellis.raster._
import geotrellis.raster.histogram._
import geotrellis.raster.rasterize._
import geotrellis.vector._


object IntHistogramSummary extends TilePolygonalSummaryHandler[Histogram[Int]] {
  def handlePartialTile(raster: Raster[Tile], polygon: Polygon): Histogram[Int] = {
    val Raster(tile, _) = raster
    val rasterExtent = raster.rasterExtent
    val histogram = FastMapHistogram()
    polygon.foreach(rasterExtent)({ (col: Int, row: Int) =>
      val z = tile.get(col, row)
      if (isData(z)) histogram.countItem(z, 1)
    })
    histogram
  }

  def handleFullTile(tile: Tile): Histogram[Int] = {
    val histogram = FastMapHistogram()
    tile.foreach { (z: Int) => if (isData(z)) histogram.countItem(z, 1) }
    histogram
  }

  def combineResults(rs: Seq[Histogram[Int]]): Histogram[Int] =
    if (rs.nonEmpty) rs.reduce(_ merge _)
    else FastMapHistogram()
}
