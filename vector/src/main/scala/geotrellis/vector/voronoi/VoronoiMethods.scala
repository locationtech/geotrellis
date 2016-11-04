package geotrellis.vector.voronoi

import geotrellis.vector.{Extent, Point}
import geotrellis.util.MethodExtensions

trait VoronoiDiagramMethods extends MethodExtensions[Traversable[Point]] {
  def voronoiDiagram(): Voronoi = { new Voronoi(self.toArray) }
}

trait VoronoiDiagramArrayMethods extends MethodExtensions[Array[Point]] {
  def voronoiDiagram(): Voronoi = { new Voronoi(self) }
}
