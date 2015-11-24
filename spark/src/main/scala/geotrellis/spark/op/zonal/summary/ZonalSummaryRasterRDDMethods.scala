package geotrellis.spark.op.zonal.summary

import geotrellis.raster.op.zonal.summary._
import geotrellis.raster.histogram._
import geotrellis.raster._
import geotrellis.spark._
import geotrellis.vector._
import geotrellis.vector.op._

import org.apache.spark.rdd._
import org.apache.spark.SparkContext._
import org.joda.time._
import reflect.ClassTag

trait ZonalSummaryFeatureRDDMethods[G <: Geometry, D] {
  val featureRdd: RDD[Feature[G, D]]

  def zonalSummary[T: ClassTag](polygon: Polygon, zeroValue: T)(handler: ZonalSummaryHandler[G, D, T]): T =
    featureRdd.aggregate(zeroValue)(handler.mergeOp(polygon, zeroValue), handler.combineOp)

  def zonalSummary[T: ClassTag](multiPolygon: MultiPolygon, zeroValue: T)(handler: ZonalSummaryHandler[G, D, T]): T =
    featureRdd.aggregate(zeroValue)(handler.mergeOp(multiPolygon, zeroValue), handler.combineOp)

}

trait ZonalSummaryKeyedFeatureRDDMethods[K, G <: Geometry, D] {
  val featureRdd: RDD[(K, Feature[G, D])]
  implicit val keyClassTag: ClassTag[K]

  def zonalSummaryByKey[T: ClassTag](polygon: Polygon, zeroValue: T)(handler: ZonalSummaryHandler[G, D, T]): RDD[(K, T)] =
    featureRdd.aggregateByKey(zeroValue)(handler.mergeOp(polygon, zeroValue), handler.combineOp)

  def zonalSummaryByKey[T: ClassTag](multiPolygon: MultiPolygon, zeroValue: T)(handler: ZonalSummaryHandler[G, D, T]): RDD[(K, T)] =
    featureRdd.aggregateByKey(zeroValue)(handler.mergeOp(multiPolygon, zeroValue), handler.combineOp)
}

trait ZonalSummaryRasterRDDMethods[K] extends RasterRDDMethods[K] {

  protected implicit val _sc: SpatialComponent[K]

  def zonalSummary[T: ClassTag](
    polygon: Polygon,
    zeroValue: T,
    handler: TileIntersectionHandler[T]
  ): T =
    rasterRDD
      .asRasters
      .map(_._2.asFeature)
      .zonalSummary(polygon, zeroValue)(handler)

  def zonalSummary[T: ClassTag](
    multiPolygon: MultiPolygon,
    zeroValue: T,
    handler: TileIntersectionHandler[T]
  ): T =
    rasterRDD
      .asRasters
      .map(_._2.asFeature)
      .zonalSummary(multiPolygon, zeroValue)(handler)

  def zonalSummaryByKey[T: ClassTag, L: ClassTag](
    polygon: Polygon,
    zeroValue: T,
    handler: TileIntersectionHandler[T],
    fKey: K => L
  ): RDD[(L, T)] =    
    rasterRDD
      .asRasters
      .map { case (key, raster) => (fKey(key), raster.asFeature) }
      .zonalSummaryByKey(polygon, zeroValue)(handler)

  def zonalSummaryByKey[T: ClassTag, L: ClassTag](
    multiPolygon: MultiPolygon,
    zeroValue: T,
    handler: TileIntersectionHandler[T],
    fKey: K => L
  ): RDD[(L, T)] =    
    rasterRDD
      .asRasters
      .map { case (key, raster) => (fKey(key), raster.asFeature) }
      .zonalSummaryByKey(multiPolygon, zeroValue)(handler)

  def zonalHistogram(polygon: Polygon): Histogram =
    zonalSummary(polygon, FastMapHistogram(), Histogram)

  def zonalHistogram(multiPolygon: MultiPolygon): Histogram =
    zonalSummary(multiPolygon, FastMapHistogram(), Histogram)

  def zonalMax(polygon: Polygon): Int =
    zonalSummary(polygon, Int.MinValue, Max)

  def zonalMax(multiPolygon: MultiPolygon): Int =
    zonalSummary(multiPolygon, Int.MinValue, Max)

  def zonalMaxDouble(polygon: Polygon): Double =
    zonalSummary(polygon, Double.MinValue, MaxDouble)

  def zonalMaxDouble(multiPolygon: MultiPolygon): Double =
    zonalSummary(multiPolygon, Double.MinValue, MaxDouble)

  def zonalMin(polygon: Polygon): Int =
    zonalSummary(polygon, Int.MaxValue, Min)

  def zonalMin(multiPolygon: MultiPolygon): Int =
    zonalSummary(multiPolygon, Int.MaxValue, Min)

  def zonalMinDouble(polygon: Polygon): Double =
    zonalSummary(polygon, Double.MaxValue, MinDouble)

  def zonalMinDouble(multiPolygon: MultiPolygon): Double =
    zonalSummary(multiPolygon, Double.MaxValue, MinDouble)

  def zonalMean(polygon: Polygon): Double =
    zonalSummary(polygon, MeanResult(0.0, 0L), Mean).mean

  def zonalMean(multiPolygon: MultiPolygon): Double =
    zonalSummary(multiPolygon, MeanResult(0.0, 0L), Mean).mean

  def zonalSum(polygon: Polygon): Long =
    zonalSummary(polygon, 0L, Sum)

  def zonalSum(multiPolygon: MultiPolygon): Long =
    zonalSummary(multiPolygon, 0L, Sum)

  def zonalSumDouble(polygon: Polygon): Double =
    zonalSummary(polygon, 0.0, SumDouble)

  def zonalSumDouble(multiPolygon: MultiPolygon): Double =
    zonalSummary(multiPolygon, 0.0, SumDouble)

}
