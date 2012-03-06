package geotrellis.operation

import geotrellis.geometry.Polygon
import geotrellis.process._

/**
  * Create a Polygon from an array of coordinates represented as a tuple (x,y).
  */
case class CreateSimplePolygon(pts:Op[Array[(Double, Double)]], v:Op[Int])
extends Op2(pts,v) ({
    (points,value) => Result(Polygon(points,value,null))
})
