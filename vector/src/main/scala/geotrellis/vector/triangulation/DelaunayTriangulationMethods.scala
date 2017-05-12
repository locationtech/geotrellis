/*
 * Copyright 2017 Azavea
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

package geotrellis.vector.triangulation

import com.vividsolutions.jts.geom.Coordinate

import geotrellis.util.MethodExtensions
import geotrellis.vector.MultiPoint

trait DelaunayTriangulationMethods extends MethodExtensions[Traversable[Coordinate]] {
  def delaunayTriangulation(): DelaunayTriangulation = { DelaunayTriangulation(self.toArray) }
}

trait DelaunayTriangulationArrayMethods extends MethodExtensions[Array[Coordinate]] {
  def delaunayTriangulation(): DelaunayTriangulation = { DelaunayTriangulation(self) }
}

trait DelaunayTriangulationPairMethods extends MethodExtensions[Traversable[(Double, Double)]] {
  def delaunayTriangulation(): DelaunayTriangulation = { DelaunayTriangulation(self.map{ case (x, y) => new Coordinate(x, y) }.toArray) }
}

trait DelaunayTriangulationPairArrayMethods extends MethodExtensions[Array[(Double, Double)]] {
  def delaunayTriangulation(): DelaunayTriangulation = { DelaunayTriangulation(self.map{ case (x, y) => new Coordinate(x, y) }) }
}

trait DelaunayTriangulationTripleMethods extends MethodExtensions[Traversable[(Double, Double, Double)]] {
  def delaunayTriangulation(): DelaunayTriangulation = { DelaunayTriangulation(self.map{ case (x, y, z) => new Coordinate(x, y, z) }.toArray) }
}

trait DelaunayTriangulationTripleArrayMethods extends MethodExtensions[Array[(Double, Double, Double)]] {
  def delaunayTriangulation(): DelaunayTriangulation = { DelaunayTriangulation(self.map{ case (x, y, z) => new Coordinate(x, y, z) }) }
}

trait DelaunayTriangulationMultiPointMethods extends MethodExtensions[MultiPoint] {
  def delaunayTriangulation(): DelaunayTriangulation = { DelaunayTriangulation(self.points.map(_.jtsGeom.getCoordinate)) }
}
