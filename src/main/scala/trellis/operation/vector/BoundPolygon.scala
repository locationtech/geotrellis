package trellis.operation

import scala.math.{min, max}
import trellis.geometry.rasterizer.Rasterizer
import trellis.process.Server
import trellis.Extent
import trellis.geometry.Polygon

/**
  * Return the extent of a given polygon.
  */
case class BoundPolygon(p:PolygonOperation, e:Operation[Extent])
extends Operation[List[Polygon]]
with SimpleOperation[List[Polygon]] {

  def childOperations = List(p, e)

  def _value(server:Server) = {
    val polygon = server.run(p)
    val extent = server.run(e)
    polygon.bound(extent)
  }
}
