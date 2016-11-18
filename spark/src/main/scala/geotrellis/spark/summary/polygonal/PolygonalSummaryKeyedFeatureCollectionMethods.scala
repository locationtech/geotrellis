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
