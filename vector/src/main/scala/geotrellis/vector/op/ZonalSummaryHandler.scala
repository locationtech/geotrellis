package geotrellis.vector.op

import geotrellis.vector._

object ZonalSummaryHandler {
  def apply[G <: Geometry, D, T]
    (handleContainsFn: Feature[G, D] => T)
    (handleIntersectionFn: (Polygon, Feature[G, D]) => T)
    (combineOpFn: (T, T) => T): ZonalSummaryHandler[G, D, T] =
    new ZonalSummaryHandler[G, D, T] {
      def handleContains(feature: Feature[G, D]): T = handleContainsFn(feature)
      def handleIntersection(polygon: Polygon, feature: Feature[G, D]): T = handleIntersectionFn(polygon, feature)
      def combineOp(v1: T, v2: T): T = combineOpFn(v1, v2)
    }
}

trait ZonalSummaryHandler[G <: Geometry, D, T] extends Serializable {
  def handleContains(feature: Feature[G, D]): T
  def handleIntersection(polygon: Polygon, feature: Feature[G, D]): T
  def combineOp(v1: T, v2: T): T

  def mergeOp(polygon: Polygon, zeroValue: T): (T, Feature[G, D]) => T = {
    def seqOp(v: T, feature: Feature[G, D]): T = {
      val rs: T =
        if (polygon.contains(feature.geom)) handleContains(feature)
        else {
          val polys = 
            polygon.intersection(feature.geom) match {
              case PolygonResult(intersectionPoly) => Seq(intersectionPoly)
              case MultiPolygonResult(mp) => mp.polygons.toSeq
              case _ => Seq()
            }

          polys
            .map { polygon => handleIntersection(polygon, feature) }
            .fold(zeroValue) { (v1: T, v2: T) => combineOp(v1, v2) }
        }

      combineOp(v, rs)
    }

    seqOp
  }

  def mergeOp(multiPolygon: MultiPolygon, zeroValue: T): (T, Feature[G, D]) => T = {
    def seqOp(v: T, feature: Feature[G, D]): T = {
      val rs: T =
        if (multiPolygon.contains(feature.geom)) handleContains(feature)
        else {
          val polys = 
            multiPolygon.intersection(feature.geom) match {
              case PolygonResult(intersectionPoly) => Seq(intersectionPoly)
              case MultiPolygonResult(mp) => mp.polygons.toSeq
              case _ => Seq()
            }

          polys
            .map { polygon => handleIntersection(polygon, feature) }
            .fold(zeroValue) { (v1: T, v2: T) => combineOp(v1, v2) }
        }

      combineOp(v, rs)
    }

    seqOp
  }
}
