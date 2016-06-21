package geotrellis.vector.voronoi

import geotrellis.vector.Point

object Implicits extends Implicits

trait Implicits {
  implicit class withDelaunayTriangulationMethods(val self: Traversable[Point]) extends DelaunayTriangulationMethods

  implicit class withVoronoiDiagramMethods(val self: Traversable[Point]) extends VoronoiDiagramMethods
}
