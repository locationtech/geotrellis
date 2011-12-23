package trellis.operation

import scala.math.{min, max}
import trellis.geometry.rasterizer.Rasterizer
import trellis.process.Server
import trellis.Extent

/**
  * Return the extent of a given polygon.
  */
case class GetPolygonExtent(p:PolygonOperation) extends Op[Extent] 
     with SimpleOp[Extent] {
  def childOperations = List(p)

  def _value(server:Server) = {
    val polygon = server.run(p)
    val (xmin, ymin, xmax, ymax) = polygon.getBounds()
    Extent(xmin, ymin, xmax, ymax)
  }
}
