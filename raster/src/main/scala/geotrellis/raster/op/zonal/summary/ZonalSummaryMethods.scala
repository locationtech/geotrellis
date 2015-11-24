package geotrellis.raster.op.zonal.summary

import geotrellis.raster._
import geotrellis.vector._

trait ZonalSummaryMethods extends TileMethods {
  def zonalSummary[T](extent: Extent, polygon: Polygon, handler: TileIntersectionHandler[T]): T = {
    val results = {
      if(polygon.contains(extent)) {
        Seq(handler.handleFullTile(tile))
      } else {
        polygon.intersection(extent) match {
          case PolygonResult(intersection) =>
            Seq(handler.handlePartialTile(Raster(tile, extent), intersection))
          case MultiPolygonResult(mp) =>
            mp.polygons.map { intersection =>
              handler.handlePartialTile(Raster(tile, extent), intersection)
            }
          case _ => Seq()
        }
      }
    }

    handler.combineResults(results)
  }

  def zonalSummary[T](extent: Extent, multiPolygon: MultiPolygon, handler: TileIntersectionHandler[T]): T = {
    val results = {
      if(multiPolygon.contains(extent)) {
        Seq(handler.handleFullTile(tile))
      } else {
        multiPolygon.intersection(extent) match {
          case PolygonResult(intersection) =>
            Seq(handler.handlePartialTile(Raster(tile, extent), intersection))
          case MultiPolygonResult(mp) =>
            mp.polygons.map { intersection =>
              handler.handlePartialTile(Raster(tile, extent), intersection)
            }
          case _ => Seq()
        }
      }
    }

    handler.combineResults(results)
  }

  def zonalHistogram(extent: Extent, geom: Polygon): histogram.Histogram =
    zonalSummary(extent, geom, Histogram)

  def zonalHistogram(extent: Extent, geom: MultiPolygon): histogram.Histogram =
    zonalSummary(extent, geom, Histogram)

  def zonalMax(extent: Extent, geom: Polygon): Int =
    zonalSummary(extent, geom, Max)

  def zonalMax(extent: Extent, geom: MultiPolygon): Int =
    zonalSummary(extent, geom, Max)

  def zonalMaxDouble(extent: Extent, geom: Polygon): Double =
    zonalSummary(extent, geom, MaxDouble)

  def zonalMaxDouble(extent: Extent, geom: MultiPolygon): Double =
    zonalSummary(extent, geom, MaxDouble)

  def zonalMin(extent: Extent, geom: Polygon): Int =
    zonalSummary(extent, geom, Min)

  def zonalMin(extent: Extent, geom: MultiPolygon): Int =
    zonalSummary(extent, geom, Min)

  def zonalMinDouble(extent: Extent, geom: Polygon): Double =
    zonalSummary(extent, geom, MinDouble)

  def zonalMinDouble(extent: Extent, geom: MultiPolygon): Double =
    zonalSummary(extent, geom, MinDouble)

  def zonalMean(extent: Extent, geom: Polygon): Double =
    zonalSummary(extent, geom, Mean).mean

  def zonalMean(extent: Extent, geom: MultiPolygon): Double =
    zonalSummary(extent, geom, Mean).mean

  def zonalSum(extent: Extent, geom: Polygon): Long =
    zonalSummary(extent, geom, Sum)

  def zonalSum(extent: Extent, geom: MultiPolygon): Long =
    zonalSummary(extent, geom, Sum)

  def zonalSumDouble(extent: Extent, geom: Polygon): Double =
    zonalSummary(extent, geom, SumDouble)

  def zonalSumDouble(extent: Extent, geom: MultiPolygon): Double =
    zonalSummary(extent, geom, SumDouble)

}
