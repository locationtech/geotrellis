package geotrellis.vector.voronoi

import geotrellis.vector.Point
import geotrellis.util.MethodExtensions

trait DelaunayTriangulationMethods extends MethodExtensions[Traversable[Point]] {
  def delaunayTriangulation(): Delaunay = { new Delaunay(self.toArray) }
}

trait DelaunayTriangulationArrayMethods extends MethodExtensions[Array[Point]] {
  def delaunayTriangulation(): Delaunay = { new Delaunay(self) }
}
