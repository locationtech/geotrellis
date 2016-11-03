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
