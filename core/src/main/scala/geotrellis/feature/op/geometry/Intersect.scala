package geotrellis.feature.op.geometry

import geotrellis._
import geotrellis.feature._
import com.vividsolutions.jts.geom

/** Returns the intersection of these geometries.
  *  @param g      first geometry to intersect, whose data is preserved
  *  @param other  other geometry to intersect
  * 
  *  @tparam A    type of feature data
  *
  *  @see [[http://tsusiatsoftware.net/jts/javadoc/com/vividsolutions/jts/geom/Geometry.html#intersection(com.vividsolutions.jts.geom.Geometry) "JTS documentation"]]
  */
case class Intersect[A](g:Op[Geometry[A]], other:Op[Geometry[_]]) extends Op2(g,other) ({
  (g,other) => Result(Feature(g.geom.intersection(other.geom), g.data))
})

