package geotrellis.op.vector.geometry

import geotrellis.geometry.Polygon
import geotrellis.op._
import geotrellis.process.Result

/**
  * Create a Polygon from an array of coordinates represented as a tuple (x,y).
  */
case class SimplePolygon(pts:Op[Array[(Double, Double)]], v:Op[Int])
extends Op2(pts,v) ({
    (points,value) => Result(Polygon(points,value,null))
})
