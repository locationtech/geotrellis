package geotrellis.feature.op.geometry

import geotrellis._
import geotrellis.feature._
import com.vividsolutions.jts.geom

/** Tests if one geometry contains another.
  *  @param g      geometry that may contain the second
  *  @param other  second geometry for contains test
  * 
  *  @tparam A    type of feature data
  *
  *  @see [[http://tsusiatsoftware.net/jts/javadoc/com/vividsolutions/jts/geom/Geometry.html#contains(com.vividsolutions.jts.geom.Geometry) "JTS documentation"]]
  */
case class Contains(g:Op[Geometry[_]], other:Op[Geometry[_]]) extends Op2(g,other) ({
  (g,other) => Result(g.geom.contains(other.geom))
})

