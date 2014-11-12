package geotrellis.spark.op.zonal.summary

import geotrellis.raster.op.zonal.summary._
import geotrellis.raster.stats._
import geotrellis.raster._

import geotrellis.spark._

import geotrellis.vector._

import reflect._

trait ZonalSummaryRasterRDDMethods[K] extends RasterRDDMethods[K] {

  protected implicit val _sc: SpatialComponent[K]

  private def zonalSummary[T: ClassTag](
    polygon: Polygon,
    zeroValue: T,
    handleTileIntersection: TileIntersection => T,
    combineOp: (T, T) => T): T = {
    val sc = rasterRDD.sparkContext
    val bcPolygon = sc.broadcast(polygon)
    val bcMetaData = sc.broadcast(rasterRDD.metaData)

    def seqOp(v: T, t: (K, Tile)): T = {
      val p = bcPolygon.value
      val extent = bcMetaData.value.mapTransform(t.id)
      val tile = t.tile

      if (p.contains(extent)) handleTileIntersection(FullTileIntersection(tile))
      else {
        val polys = p.intersection(extent) match {
          case PolygonResult(intersectionPoly) => Seq(intersectionPoly)
          case MultiPolygonResult(mp) => mp.polygons.toSeq
          case _ => Seq()
        }

        polys.map(PartialTileIntersection(tile, extent, _))
          .map(handleTileIntersection).fold(zeroValue)(combineOp)
      }
    }

    rasterRDD.aggregate(zeroValue)(seqOp, combineOp)
  }

  private def zonalSummary[T: ClassTag](
    polygon: Polygon,
    zeroValue: T,
    handler: TileIntersectionHandler[T, T]): T = {
    zonalSummary(
      polygon,
      zeroValue,
      handler,
      (a: T, b: T) => handler.combineResults(Seq(a, b))
    )
  }

  def zonalHistogram(polygon: Polygon): Histogram =
    zonalSummary(polygon, FastMapHistogram.asInstanceOf[Histogram], Histogram)

  def zonalMax(polygon: Polygon): Int =
    zonalSummary(polygon, Int.MinValue, Max)

  def zonalMaxDouble(polygon: Polygon): Double =
    zonalSummary(polygon, Double.MinValue, MaxDouble)

  def zonalMin(polygon: Polygon): Int =
    zonalSummary(polygon, Int.MaxValue, Min)

  def zonalMinDouble(polygon: Polygon): Double =
    zonalSummary(polygon, Double.MaxValue, MinDouble)

  def zonalMean(polygon: Polygon): Double =
    zonalSummary(
      polygon,
      MeanResult(0.0, 0L),
      Mean,
      (a: MeanResult, b: MeanResult) => a + b
    ).mean

  def zonalSum(polygon: Polygon): Long =
    zonalSummary(polygon, 0L, Sum)

  def zonalSumDouble(polygon: Polygon): Double =
    zonalSummary(polygon, 0.0, SumDouble)

}
