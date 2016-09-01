package geotrellis.spark.summary.polygonal

import geotrellis.vector._
import geotrellis.vector.summary.polygonal._

trait PolygonalSummaryKeyedFeatureCollectionMethods[K, G <: Geometry, D] {
  val featureCollection: Seq[(K, Feature[G, D])]

  private def aggregateByKey[T](self: Seq[(K, Feature[G, D])])(zeroValue: T)(seqOp: (T, Feature[G, D]) => T, combOp: (T, T) => T): Seq[(K, T)] =
    self.groupBy(_._1).mapValues { _.map(_._2).aggregate(zeroValue)(seqOp, combOp) } toSeq

  def polygonalSummaryByKey[T](polygon: Polygon, zeroValue: T)(handler: PolygonalSummaryHandler[G, D, T]): Seq[(K, T)] =
    aggregateByKey(featureCollection)(zeroValue)(handler.mergeOp(polygon, zeroValue), handler.combineOp)

  def polygonalSummaryByKey[T](multiPolygon: MultiPolygon, zeroValue: T)(handler: PolygonalSummaryHandler[G, D, T]): Seq[(K, T)] =
    aggregateByKey(featureCollection)(zeroValue)(handler.mergeOp(multiPolygon, zeroValue), handler.combineOp)
}
