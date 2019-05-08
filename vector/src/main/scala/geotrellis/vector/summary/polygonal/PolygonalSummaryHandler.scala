/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.vector
package summary.polygonal

/** Companion object to [[PolygonalSummaryHandler]] */
object PolygonalSummaryHandler {
  def apply[G <: Geometry, D, T]
  (handleContainsFn: Feature[G, D] => T)
  (handleIntersectionFn: (Polygon, Feature[G, D]) => T)
  (combineOpFn: (T, T) => T): PolygonalSummaryHandler[G, D, T] =
    new PolygonalSummaryHandler[G, D, T] {
      def handleContains(feature: Feature[G, D]): T = handleContainsFn(feature)
      def handleIntersection(polygon: Polygon, feature: Feature[G, D]): T = handleIntersectionFn(polygon, feature)
      def combineOp(v1: T, v2: T): T = combineOpFn(v1, v2)
    }
}

/** This trait provides the scaffolding necessary to generate aggregations
  * which depend on [[Polygon]] and [[MultiPolygon]] instances.
  *
  * @note  This trait specifies the type of operation to be carried out but *NOT* the "zero
  *        element" which determines the beginning value for a chain of operations (like the
  *        starting element in a fold) and *NOT* the [[Polygon]] or [[MultiPolygon]] to be
  *        used during a summary. Instead, those are specified in a call to mergeOp which
  *        returns the function used in an actual summarization.
  *
  * @tparam G  the [[Geometry]] type against which containment and intersection checks are
  *            carried out to determine the flow of aggregation logic
  * @tparam D  the type of any data expected to be associated with G in [[Feature]] instances
  * @tparam T  the type being aggregated to through summary mergeOp calls
  *
  * @param handleContains  the function to be called on a feature when it is determined that
  *                        said feature is contained by the [[Polygon]] or [[MultiPolygon]] used
  *                        in this trait's mergeOp functions
  * @param handleIntersection  the function to be called on a feature when it is determined that
  *                            said feature is not contained by the [[Polygon]] or
  *                            [[MultiPolygon]] used in this trait's mergeOp functions
  * @param combineOp  the function to be called when combining intermedite aggregation results
  *
  */
trait PolygonalSummaryHandler[G <: Geometry, D, T] extends Serializable {
  def handleContains(feature: Feature[G, D]): T
  def handleIntersection(polygon: Polygon, feature: Feature[G, D]): T
  def combineOp(v1: T, v2: T): T

  /** Given a polygon and a zerovalue, this function returns a function
    * which can be used to compute aggregations
    */
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

  /** Given a polygon and a zerovalue, this function returns a function
    * which can be used to compute aggregations
    */
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
