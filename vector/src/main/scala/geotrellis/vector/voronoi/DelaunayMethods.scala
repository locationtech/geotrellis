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

import geotrellis.vector.triangulation._
import geotrellis.vector.mesh._
import geotrellis.vector.Point
import geotrellis.util.MethodExtensions

import org.locationtech.jts.geom.Coordinate


trait DelaunayTriangulationPointMethods extends MethodExtensions[Traversable[Point]] {
  def delaunayTriangulation(): DelaunayTriangulation = {
    val ips = IndexedPointSet(self.map({ _.jtsGeom.getCoordinate }).toArray)
    DelaunayTriangulation(ips)
  }
}

trait DelaunayTriangulationCoordinateMethods extends MethodExtensions[Traversable[Coordinate]] {
  def delaunayTriangulation(): DelaunayTriangulation = {
    val ips = IndexedPointSet(self.toArray)
    DelaunayTriangulation(ips)
  }
}

trait DelaunayTriangulationArrayMethods extends MethodExtensions[Array[Point]] {
  def delaunayTriangulation(): DelaunayTriangulation = {
    val ips = IndexedPointSet(self.map({ _.jtsGeom.getCoordinate }))
    DelaunayTriangulation(ips)
  }
}
