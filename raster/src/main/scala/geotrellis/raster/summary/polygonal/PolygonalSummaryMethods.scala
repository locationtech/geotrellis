package geotrellis.raster.summary.polygonal

import geotrellis.raster._
import geotrellis.raster.histogram.Histogram
import geotrellis.vector._
import geotrellis.util.MethodExtensions


trait PolygonalSummaryMethods extends MethodExtensions[Tile] {
  def polygonalSummary[T](extent: Extent, polygon: Polygon, handler: TilePolygonalSummaryHandler[T]): T = {
    val results = {
      if(polygon.contains(extent)) {
        Seq(handler.handleFullTile(self))
      } else {
        polygon.intersection(extent) match {
          case PolygonResult(intersection) =>
            Seq(handler.handlePartialTile(Raster(self, extent), intersection))
          case MultiPolygonResult(mp) =>
            mp.polygons.map { intersection =>
              handler.handlePartialTile(Raster(self, extent), intersection)
            }
          case _ => Seq()
        }
      }
    }

    handler.combineResults(results)
  }

  def polygonalSummary[T](extent: Extent, multiPolygon: MultiPolygon, handler: TilePolygonalSummaryHandler[T]): T = {
    val results = {
      if(multiPolygon.contains(extent)) {
        Seq(handler.handleFullTile(self))
      } else {
        multiPolygon.intersection(extent) match {
          case PolygonResult(intersection) =>
            Seq(handler.handlePartialTile(Raster(self, extent), intersection))
          case MultiPolygonResult(mp) =>
            mp.polygons.map { intersection =>
              handler.handlePartialTile(Raster(self, extent), intersection)
            }
          case _ => Seq()
        }
      }
    }

    handler.combineResults(results)
  }

  def polygonalHistogram(extent: Extent, geom: Polygon): Histogram[Int] =
    polygonalSummary(extent, geom, IntHistogramSummary)

  def polygonalHistogram(extent: Extent, geom: MultiPolygon): Histogram[Int] =
    polygonalSummary(extent, geom, IntHistogramSummary)

  def polygonalHistogramDouble(extent: Extent, geom: Polygon): Histogram[Double] =
    polygonalSummary(extent, geom, DoubleHistogramSummary)

  def polygonalHistogramDouble(extent: Extent, geom: MultiPolygon): Histogram[Double] =
    polygonalSummary(extent, geom, DoubleHistogramSummary)

  def polygonalMax(extent: Extent, geom: Polygon): Int =
    polygonalSummary(extent, geom, MaxSummary)

  def polygonalMax(extent: Extent, geom: MultiPolygon): Int =
    polygonalSummary(extent, geom, MaxSummary)

  def polygonalMaxDouble(extent: Extent, geom: Polygon): Double =
    polygonalSummary(extent, geom, MaxDoubleSummary)

  def polygonalMaxDouble(extent: Extent, geom: MultiPolygon): Double =
    polygonalSummary(extent, geom, MaxDoubleSummary)

  def polygonalMin(extent: Extent, geom: Polygon): Int =
    polygonalSummary(extent, geom, MinSummary)

  def polygonalMin(extent: Extent, geom: MultiPolygon): Int =
    polygonalSummary(extent, geom, MinSummary)

  def polygonalMinDouble(extent: Extent, geom: Polygon): Double =
    polygonalSummary(extent, geom, MinDoubleSummary)

  def polygonalMinDouble(extent: Extent, geom: MultiPolygon): Double =
    polygonalSummary(extent, geom, MinDoubleSummary)

  def polygonalMean(extent: Extent, geom: Polygon): Double =
    polygonalSummary(extent, geom, MeanSummary).mean

  def polygonalMean(extent: Extent, geom: MultiPolygon): Double =
    polygonalSummary(extent, geom, MeanSummary).mean

  def polygonalSum(extent: Extent, geom: Polygon): Long =
    polygonalSummary(extent, geom, SumSummary)

  def polygonalSum(extent: Extent, geom: MultiPolygon): Long =
    polygonalSummary(extent, geom, SumSummary)

  def polygonalSumDouble(extent: Extent, geom: Polygon): Double =
    polygonalSummary(extent, geom, SumDoubleSummary)

  def polygonalSumDouble(extent: Extent, geom: MultiPolygon): Double =
    polygonalSummary(extent, geom, SumDoubleSummary)

}
