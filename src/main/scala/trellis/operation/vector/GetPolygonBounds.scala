package trellis.operation

import scala.math.{min, max}
import trellis.geometry.rasterizer.Rasterizer
import trellis.process._

/**
  * Return the extent of a given polygon.
  */
case class GetPolygonBounds(p:PolygonOperation) extends Operation[(Double, Double, Double, Double)] 
     with SimpleOperation[(Double,Double,Double,Double)] {
  def childOperations = List(p)

  def _value(server:Server)(implicit t:Timer) = {
    val polygon = server.run(p)
    polygon.getBounds()
  }
}
