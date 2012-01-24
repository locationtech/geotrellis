package trellis.operation

import trellis._
import trellis.process._
import trellis.geometry.Polygon

/**
  * Return the extent of a given polygon.
  */
case class PolygonExtent(p:Op[Polygon], e:Op[Extent]) extends Op2(p,e) ({
  (polygon,extent) => Result(polygon.bound(extent))
})
