package geotrellis.feature.op.geometry

import geotrellis._
import geotrellis.feature._
import com.vividsolutions.jts.geom

/** Returns the area of a geometry.
  * 
  *  @param g      geometry for area calculation
  * 
  *  @see [[http://tsusiatsoftware.net/jts/javadoc/com/vividsolutions/jts/geom/Geometry.html#getArea() "JTS documentation"]]
  */
case class GetArea(g:Op[Geometry[_]]) extends Op1(g) ({
  (g) => Result(g.geom.getArea)
})

