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

import com.vividsolutions.jts.geom.Coordinate

import geotrellis.vector.{Extent, Point}
import geotrellis.util.MethodExtensions

trait VoronoiDiagramMethods extends MethodExtensions[Traversable[Point]] {
  @deprecated("call voronoiDiagram() on Traversable[Coordinate] instead", "1.2")
  def voronoiDiagram(): Voronoi = { new Voronoi(self.toArray) }
}

trait VoronoiDiagramArrayMethods extends MethodExtensions[Array[Point]] {
  @deprecated("call voronoiDiagram() on Array[Coordinate] instead", "1.2")
  def voronoiDiagram(): Voronoi = { new Voronoi(self) }
}

trait FastVoronoiDiagramMethods extends MethodExtensions[Traversable[Coordinate]] {
  def voronoiDiagram(extent: Extent): VoronoiDiagram = { VoronoiDiagram(self.toArray, extent) }
}

trait FastVoronoiDiagramArrayMethods extends MethodExtensions[Array[Coordinate]] {
  def voronoiDiagram(extent: Extent): VoronoiDiagram = { VoronoiDiagram(self, extent) }
}

trait FastVoronoiDiagramPairMethods extends MethodExtensions[Traversable[(Double, Double)]] {
  def voronoiDiagram(extent: Extent): VoronoiDiagram = { VoronoiDiagram(self.map{ case (x, y) => new Coordinate(x, y) }.toArray, extent) }
}

trait FastVoronoiDiagramPairArrayMethods extends MethodExtensions[Array[(Double, Double)]] {
  def voronoiDiagram(extent: Extent): VoronoiDiagram = { VoronoiDiagram(self.map{ case (x, y) => new Coordinate(x, y) }, extent) }
}

trait FastVoronoiDiagramTripleMethods extends MethodExtensions[Traversable[(Double, Double, Double)]] {
  def voronoiDiagram(extent: Extent): VoronoiDiagram = { VoronoiDiagram(self.map{ case (x, y, z) => new Coordinate(x, y, z) }.toArray, extent) }
}

trait FastVoronoiDiagramTripleArrayMethods extends MethodExtensions[Array[(Double, Double, Double)]] {
  def voronoiDiagram(extent: Extent): VoronoiDiagram = { VoronoiDiagram(self.map{ case (x, y, z) => new Coordinate(x, y, z) }, extent) }
}
