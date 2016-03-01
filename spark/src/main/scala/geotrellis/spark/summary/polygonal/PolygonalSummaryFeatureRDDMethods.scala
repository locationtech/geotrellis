package geotrellis.spark.summary.polygonal

import geotrellis.vector._
import geotrellis.vector.summary.polygonal._

import org.apache.spark.rdd._
import reflect.ClassTag

trait PolygonalSummaryFeatureRDDMethods[G <: Geometry, D] {
  val featureRdd: RDD[Feature[G, D]]

  def polygonalSummary[T: ClassTag](polygon: Polygon, zeroValue: T)(handler: PolygonalSummaryHandler[G, D, T]): T =
    featureRdd.aggregate(zeroValue)(handler.mergeOp(polygon, zeroValue), handler.combineOp)

  def polygonalSummary[T: ClassTag](multiPolygon: MultiPolygon, zeroValue: T)(handler: PolygonalSummaryHandler[G, D, T]): T =
    featureRdd.aggregate(zeroValue)(handler.mergeOp(multiPolygon, zeroValue), handler.combineOp)

}
