package geotrellis.operation

import geotrellis._
import geotrellis.process._
import geotrellis.geometry.Polygon

/**
  * Return the extent of a given polygon.
  */
case class PolygonExtent(p:Op[Polygon], e:Op[Extent]) extends Op2(p,e) ({
  (polygon,extent) => Result(polygon.bound(extent))
})
