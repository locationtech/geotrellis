package geotrellis.raster.summary.polygonal

import geotrellis.raster._
import geotrellis.vector._
import geotrellis.raster.rasterize._
import geotrellis.raster.histogram._

object DoubleHistogramSummary extends TilePolygonalSummaryHandler[Histogram[Double]] {
  def handlePartialTile(raster: Raster[Tile], polygon: Polygon): Histogram[Double] = {
    val Raster(tile, _) = raster
    val rasterExtent = raster.rasterExtent
    val histogram = StreamingHistogram()
    Rasterizer.foreachCellByGeometry(polygon, rasterExtent) { (col: Int, row: Int) =>
      val z = tile.get(col, row)
      if (isData(z)) histogram.countItem(z, 1)
    }
    histogram
  }

  def handleFullTile(tile: Tile): Histogram[Double] = {
    val histogram = StreamingHistogram()
    tile.foreach { (z: Int) => if (isData(z)) histogram.countItem(z, 1) }
    histogram
  }

  def combineResults(rs: Seq[Histogram[Double]]): Histogram[Double] =
    if (rs.nonEmpty) rs.reduce(_ merge _)
    else StreamingHistogram()
}
