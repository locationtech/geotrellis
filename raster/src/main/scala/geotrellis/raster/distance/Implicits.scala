package geotrellis.raster.distance

import geotrellis.vector.Point

object Implicits extends Implicits

trait Implicits {
  implicit class withEuclideanDistanceTileMethods(val self: Array[Point]) extends EuclideanDistanceTileMethods
}
