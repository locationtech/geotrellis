package geotrellis.raster.op.zonal.summary

import geotrellis.raster._
import geotrellis.vector._

trait ZonalSummaryMethods extends TileMethods {
  def zonalSummary[T, U](
    extent: Extent, 
    polygon: Polygon,
    handleFullTile: FullTileIntersection => T,
    handlePartialTile: PartialTileIntersection => T,
    combineResults: Seq[T] => U): U = {
    val results = {
      if(polygon.contains(extent)) {
        Seq(handleFullTile(tile))
      } else {
        polygon.intersection(extent) match {
          case PolygonResult(intersection) =>
            Seq(handlePartialTile(PartialTileIntersection(tile, extent, intersection)))
          case MultiPolygonResult(mp) =>
            mp.polygons.map { intersection =>
              handlePartialTile(PartialTileIntersection(tile, extent, intersection))
            }
          case _ => Seq()
        }
      }
    }

    combineResults(results)
  }

  def zonalSummary[T, U](extent: Extent, polygon: Polygon, handler: TileIntersectionHandler[T, U]): U =
    zonalSummary(extent, polygon, handler.handleFullTile, handler.handlePartialTile, handler.combineResults)

  def zonalHistogram(extent: Extent, polygon: Polygon): stats.Histogram =
    zonalSummary(extent, polygon, Histogram)

  def zonalMax(extent: Extent, polygon: Polygon): Int =
    zonalSummary(extent, polygon, Max)

  def zonalMaxDouble(extent: Extent, polygon: Polygon): Double =
    zonalSummary(extent, polygon, MaxDouble)

  def zonalMin(extent: Extent, polygon: Polygon): Int =
    zonalSummary(extent, polygon, Min)

  def zonalMinDouble(extent: Extent, polygon: Polygon): Double =
    zonalSummary(extent, polygon, MinDouble)

  def zonalMean(extent: Extent, polygon: Polygon): Double =
    zonalSummary(extent, polygon, Mean)

  def zonalSum(extent: Extent, polygon: Polygon): Long =
    zonalSummary(extent, polygon, Sum)

  def zonalSumDouble(extent: Extent, polygon: Polygon): Double =
    zonalSummary(extent, polygon, SumDouble)

}
