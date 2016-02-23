package geotrellis.spark.summary.polygonal

import geotrellis.vector._
import geotrellis.vector.summary.polygonal._
import org.apache.spark.Partitioner

import org.apache.spark.rdd._
import reflect.ClassTag

trait PolygonalSummaryKeyedFeatureRDDMethods[K, G <: Geometry, D] {
  val featureRdd: RDD[(K, Feature[G, D])]
  implicit val keyClassTag: ClassTag[K]

  def polygonalSummaryByKey[T: ClassTag](polygon: Polygon, zeroValue: T)(handler: PolygonalSummaryHandler[G, D, T]): RDD[(K, T)] =
    featureRdd.aggregateByKey(zeroValue)(handler.mergeOp(polygon, zeroValue), handler.combineOp)

  def polygonalSummaryByKey[T: ClassTag](polygon: Polygon, zeroValue: T, partitioner: Option[Partitioner])(handler: PolygonalSummaryHandler[G, D, T]): RDD[(K, T)] =
    partitioner
      .fold(featureRdd.aggregateByKey(zeroValue) _)(featureRdd.aggregateByKey(zeroValue, _)) (
        handler.mergeOp(polygon, zeroValue), handler.combineOp
      )

  def polygonalSummaryByKey[T: ClassTag](multiPolygon: MultiPolygon, zeroValue: T)(handler: PolygonalSummaryHandler[G, D, T]): RDD[(K, T)] =
    featureRdd.aggregateByKey(zeroValue)(handler.mergeOp(multiPolygon, zeroValue), handler.combineOp)

  def polygonalSummaryByKey[T: ClassTag](multiPolygon: MultiPolygon, zeroValue: T, partitioner: Option[Partitioner])(handler: PolygonalSummaryHandler[G, D, T]): RDD[(K, T)] =
    partitioner
      .fold(featureRdd.aggregateByKey(zeroValue) _)(featureRdd.aggregateByKey(zeroValue, _)) (
        handler.mergeOp(multiPolygon, zeroValue), handler.combineOp
      )
}
