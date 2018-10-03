/*
 * Copyright 2018 Azavea
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

package geotrellis.spark.distance

import org.locationtech.jts.geom.Coordinate
import org.apache.spark.rdd.RDD

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.util.MethodExtensions
import geotrellis.vector.{Extent, MultiPoint, Point}

trait EuclideanDistanceRDDMethods extends MethodExtensions[RDD[(SpatialKey, Array[Coordinate])]] {
  def euclideanDistance(layout: LayoutDefinition): RDD[(SpatialKey, Tile)] = { EuclideanDistance(self, layout) }
}

trait EuclideanDistancePointRDDMethods extends MethodExtensions[RDD[(SpatialKey, Array[Point])]] {
  def euclideanDistance(layout: LayoutDefinition): RDD[(SpatialKey, Tile)] = { EuclideanDistance(self.mapValues(_.map(_.jtsGeom.getCoordinate)), layout) }
}

trait EuclideanDistanceMultiPointRDDMethods extends MethodExtensions[RDD[(SpatialKey, MultiPoint)]] {
  def euclideanDistance(layout: LayoutDefinition): RDD[(SpatialKey, Tile)] =
    EuclideanDistance(self.mapValues(_.points.map(_.jtsGeom.getCoordinate)), layout) 
}
