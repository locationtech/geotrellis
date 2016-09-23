package geotrellis.spark.summary.polygonal

import geotrellis.vector._
import geotrellis.vector.summary.polygonal._

trait PolygonalSummaryFeatureCollectionMethods[G <: Geometry, D] {
  val featureCollection: Seq[Feature[G, D]]

  def polygonalSummary[T](polygon: Polygon, zeroValue: T)(handler: PolygonalSummaryHandler[G, D, T]): T =
    featureCollection.aggregate(zeroValue)(handler.mergeOp(polygon, zeroValue), handler.combineOp)

  def polygonalSummary[T](multiPolygon: MultiPolygon, zeroValue: T)(handler: PolygonalSummaryHandler[G, D, T]): T =
    featureCollection.aggregate(zeroValue)(handler.mergeOp(multiPolygon, zeroValue), handler.combineOp)

}
