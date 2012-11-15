package geotrellis.feature.op.geometry

import geotrellis._
import geotrellis.feature._
import com.vividsolutions.jts.geom

/** Tests if one geometry overlaps another.
  *  @param g      first geometry for overlap test
  *  @param other  second geometry for overlap test
  * 
  *  @see [[http://tsusiatsoftware.net/jts/javadoc/com/vividsolutions/jts/geom/Geometry.html#intersects(com.vividsolutions.jts.geom.Geometry) "JTS documentation"]]
  */
case class Overlaps(g:Op[Geometry[_]], other:Op[Geometry[_]]) extends Op2(g,other) ({
  (g,other) => Result(g.geom.overlaps(other.geom))
})

