package geotrellis.feature.op.geometry

import geotrellis._
import geotrellis.feature._

/**
 * Computes the centroid of this geometry.
 */
case class GetCentroid[A](f:Op[Geometry[A]]) extends Op1(f) ({
  (f) => Result(f.mapGeom(_.getCentroid))
})

