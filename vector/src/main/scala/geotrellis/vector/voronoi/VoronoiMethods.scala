package geotrellis.vector.voronoi

import geotrellis.vector.{Extent, Point}
import geotrellis.util.MethodExtensions

trait VoronoiDiagramMethods extends MethodExtensions[Traversable[Point]] {
  def voronoiDiagram(extent: Extent): Voronoi = { new Voronoi(self.toArray, extent) }
}

trait VoronoiDiagramArrayMethods extends MethodExtensions[Array[Point]] {
  def voronoiDiagram(extent: Extent): Voronoi = { new Voronoi(self, extent) }
}
