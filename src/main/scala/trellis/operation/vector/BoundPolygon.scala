package trellis.operation

import scala.math.{min, max}
import trellis.geometry.rasterizer.Rasterizer
import trellis.process._
import trellis.Extent
import trellis.geometry.Polygon

/**
  * Return the extent of a given polygon.
  */
case class BoundPolygon(p:PolygonOperation, e:Operation[Extent])
extends Operation[List[Polygon]]
with SimpleOperation[List[Polygon]] {

  def childOperations = List(p, e)

  def _value(context:Context) = {
    val polygon = context.run(p)
    val extent = context.run(e)
    polygon.bound(extent)
  }
}
