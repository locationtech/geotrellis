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

trait PolygonalSummaryFeatureCollectionMethods[G <: Geometry, D] {
  val featureCollection: Seq[Feature[G, D]]

  def polygonalSummary[T](polygon: Polygon, zeroValue: T)(handler: PolygonalSummaryHandler[G, D, T]): T =
    featureCollection.aggregate(zeroValue)(handler.mergeOp(polygon, zeroValue), handler.combineOp)

  def polygonalSummary[T](multiPolygon: MultiPolygon, zeroValue: T)(handler: PolygonalSummaryHandler[G, D, T]): T =
    featureCollection.aggregate(zeroValue)(handler.mergeOp(multiPolygon, zeroValue), handler.combineOp)

}
