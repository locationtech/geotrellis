package geotrellis.vector.op.geometry

import geotrellis.geometry.Polygon
import geotrellis._

/**
  * Create a Polygon from an array of coordinates represented as a tuple (x,y).
  */
case class SimplePolygon(pts:Op[Array[(Double, Double)]], v:Op[Int])
extends Op2(pts,v) ({
    (points,value) => Result(Polygon(points,value,null))
})
