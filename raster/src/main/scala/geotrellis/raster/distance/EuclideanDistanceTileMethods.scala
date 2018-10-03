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

package geotrellis.raster.distance

import org.locationtech.jts.geom.Coordinate

import geotrellis.raster.{RasterExtent, Tile}
import geotrellis.util.MethodExtensions
import geotrellis.vector.{MultiPoint, Point}

trait EuclideanDistanceTileArrayMethods extends MethodExtensions[Array[Point]] {
  def euclideanDistanceTile(rasterExtent: RasterExtent): Tile = { EuclideanDistanceTile(self, rasterExtent) }
}

trait EuclideanDistanceTileMethods extends MethodExtensions[Traversable[Point]] {
  def euclideanDistanceTile(rasterExtent: RasterExtent): Tile = { EuclideanDistanceTile(self.toArray, rasterExtent) }
}

trait EuclideanDistanceTileCoordinateArrayMethods extends MethodExtensions[Array[Coordinate]] {
  def euclideanDistanceTile(rasterExtent: RasterExtent): Tile = { EuclideanDistanceTile(self, rasterExtent) }
}

trait EuclideanDistanceTileCoordinateMethods extends MethodExtensions[Traversable[Coordinate]] {
  def euclideanDistanceTile(rasterExtent: RasterExtent): Tile = { EuclideanDistanceTile(self.toArray, rasterExtent) }
}

trait EuclideanDistanceTileMultiPointMethods extends MethodExtensions[MultiPoint] {
  def euclideanDistanceTile(rasterExtent: RasterExtent): Tile = { EuclideanDistanceTile(self.points.map{_.jtsGeom.getCoordinate}, rasterExtent) }
}
