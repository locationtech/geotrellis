package geotrellis.feature.op.geometry

import geotrellis._
import geotrellis.feature._
import com.vividsolutions.jts.geom

/** Tests if one geometry covers another.
  *  @param g      first geometry for covering test
  *  @param other  second geometry for covering test
  * 
  *  @tparam A    type of feature data
  *
  *  @see [[http://tsusiatsoftware.net/jts/javadoc/com/vividsolutions/jts/geom/Geometry.html#covers(com.vividsolutions.jts.geom.Geometry) "JTS documentation"]]
  */
case class Covers(g:Op[Geometry[_]], other:Op[Geometry[_]]) extends Op2(g,other) ({
  (g,other) => Result(g.geom.covers(other.geom))
})

