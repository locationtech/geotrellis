package geotrellis.feature.op.geometry

import geotrellis._
import geotrellis.feature._
import com.vividsolutions.jts.geom

/** Tests if one geometry equals another.
  *  @param g      first geometry for equality test
  *  @param other  second geometry for equality test
  * 
  *  @tparam A    type of feature data
  *
  *  @see [[http://tsusiatsoftware.net/jts/javadoc/com/vividsolutions/jts/geom/Geometry.html#contains(com.vividsolutions.jts.geom.Geometry) "JTS documentation"]]
  */
case class Equals[A](g:Op[Geometry[_]], other:Op[Geometry[_]]) extends Op2(g,other) ({
  (g,other) => Result(g.geom.equals(other.geom))
})

