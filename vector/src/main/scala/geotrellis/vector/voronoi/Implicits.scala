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

package geotrellis.vector.voronoi

import org.locationtech.jts.geom.Coordinate
import geotrellis.vector.{MultiPoint, Point}

object Implicits extends Implicits

trait Implicits {
  implicit class withDelaunayTriangulationPointMethods(val self: Traversable[Point]) extends DelaunayTriangulationPointMethods

  implicit class withDelaunayTriangulationCoordinateMethods(val self: Traversable[Coordinate]) extends DelaunayTriangulationCoordinateMethods

  implicit class withDelaunayTriangulationPointArrayMethods(val self: Array[Point]) extends DelaunayTriangulationArrayMethods

  implicit class withVoronoiDiagramPointMethods(val self: Traversable[Point]) extends VoronoiDiagramPointMethods

  implicit class withVoronoiDiagramPointArrayMethods(val self: Array[Point]) extends VoronoiDiagramPointArrayMethods

  implicit class withVoronoiDiagramCoordinateMethods(val self: Traversable[Coordinate]) extends VoronoiDiagramCoordinateMethods

  implicit class withVoronoiDiagramCoordinateArrayMethods(val self: Array[Coordinate]) extends VoronoiDiagramCoordinateArrayMethods

  implicit class withVoronoiDiagramMultiPointMethods(val self: MultiPoint) extends VoronoiDiagramMultiPointMethods
}
