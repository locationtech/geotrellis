package trellis.operation

import scala.math.{min, max}
import trellis.geometry.rasterizer.Rasterizer
import trellis.process._

/**
  * Return the extent of a given polygon.
  */
case class GetPolygonBounds(p:PolygonOperation)
extends SimpleOperation[(Double,Double,Double,Double)] {
  def childOperations = List(p)

  def _value(context:Context) = {
    val polygon = context.run(p)
    polygon.getBounds()
  }
}
