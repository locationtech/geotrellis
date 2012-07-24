package geotrellis.vector.op.geometry

import geotrellis._
import geotrellis.geometry.Polygon

/**
  * Return the intersection of two geometries.
  *
  * Currently implemented only for a polygon and a rectangle extent.
  */
case class Intersect(p:Op[Polygon], e:Op[Extent]) extends Op2(p,e) ({
  (polygon,extent) => Result(polygon.bound(extent))
})
