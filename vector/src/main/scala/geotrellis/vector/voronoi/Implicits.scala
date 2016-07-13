package geotrellis.vector.voronoi

import geotrellis.vector.Point

object Implicits extends Implicits

trait Implicits {
  implicit class withDelaunayTriangulationMethods(val self: Traversable[Point]) extends DelaunayTriangulationMethods

  implicit class withDelaunayTriangulationArrayMethods(val self: Array[Point]) extends DelaunayTriangulationArrayMethods

  implicit class withVoronoiDiagramMethods(val self: Traversable[Point]) extends VoronoiDiagramMethods

  implicit class withVoronoiDiagramArrayMethods(val self: Array[Point]) extends VoronoiDiagramArrayMethods
}
