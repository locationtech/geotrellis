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

import geotrellis.raster._
import geotrellis.tiling._
import geotrellis.spark._
import geotrellis.vector._
import geotrellis.util._

import org.apache.spark.rdd._
import scala.reflect._

object Implicits extends Implicits

trait Implicits {
  implicit class withZonalSummaryTileLayerRDDMethods[
    K,
    M: GetComponent[?, LayoutDefinition]
  ](val self: RDD[(K, Tile)] with Metadata[M])
    (implicit val keyClassTag: ClassTag[K], implicit val _sc: SpatialComponent[K])
      extends PolygonalSummaryTileLayerRDDMethods[K, M] with Serializable

  implicit class withZonalSummaryMultibandTileLayerRDDMethods[
    K,
    M: GetComponent[?, LayoutDefinition]
  ](val self: RDD[(K, MultibandTile)] with Metadata[M])
    (implicit val keyClassTag: ClassTag[K], implicit val _sc: SpatialComponent[K])
      extends PolygonalSummaryMultibandTileLayerRDDMethods[K, M] with Serializable

  implicit class withZonalSummaryFeatureRDDMethods[G <: Geometry, D](val featureRdd: RDD[Feature[G, D]])
      extends PolygonalSummaryFeatureRDDMethods[G, D]

  implicit class withZonalSummaryKeyedFeatureRDDMethods[K, G <: Geometry, D](val featureRdd: RDD[(K, Feature[G, D])])
    (implicit val keyClassTag: ClassTag[K])
      extends PolygonalSummaryKeyedFeatureRDDMethods[K, G, D]

  implicit class withZonalSummaryTileLayerCollectionMethods[
    K,
    M: GetComponent[?, LayoutDefinition]
  ](val self: Seq[(K, Tile)] with Metadata[M])
   (implicit val keyClassTag: ClassTag[K], implicit val _sc: SpatialComponent[K])
    extends PolygonalSummaryTileLayerCollectionMethods[K, M] with Serializable

  implicit class withZonalSummaryMultibandTileLayerCollectionMethods[
    K,
    M: GetComponent[?, LayoutDefinition]
  ](val self: Seq[(K, MultibandTile)] with Metadata[M])
   (implicit val keyClassTag: ClassTag[K], implicit val _sc: SpatialComponent[K])
    extends PolygonalSummaryMultibandTileLayerCollectionMethods[K, M] with Serializable

  implicit class withZonalSummaryFeatureCollectionMethods[G <: Geometry, D](val featureCollection: Seq[Feature[G, D]])
    extends PolygonalSummaryFeatureCollectionMethods[G, D]

  implicit class withZonalSummaryKeyedFeatureCollectionMethods[K, G <: Geometry, D](val featureCollection: Seq[(K, Feature[G, D])])
    (implicit val keyClassTag: ClassTag[K])
      extends PolygonalSummaryKeyedFeatureCollectionMethods[K, G, D]

}
