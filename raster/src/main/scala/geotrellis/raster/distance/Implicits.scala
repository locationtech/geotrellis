package geotrellis.raster.distance

import geotrellis.vector.Point

object Implicits extends Implicits

trait Implicits {
  implicit class withEuclideanDistanceTileMethods(val self: Traversable[Point]) extends EuclideanDistanceTileMethods

  implicit class withEuclideanDistanceTileArrayMethods(val self: Array[Point]) extends EuclideanDistanceTileArrayMethods
}
