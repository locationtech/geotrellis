package geotrellis.feature.op.geometry

import geotrellis._
import geotrellis.feature._
import com.vividsolutions.jts.geom

/** Returns the union of these geometries.
  *  @param g      first geometry to union, whose data is preserved
  *  @param other  other geometry to union
  * 
  *  @tparam A    type of feature data
  *
  *  @see [[http://tsusiatsoftware.net/jts/javadoc/com/vividsolutions/jts/geom/Geometry.html#union(com.vividsolutions.jts.geom.Geometry) "JTS documentation"]]
  */
case class Union[A](g:Op[Geometry[A]], other:Op[Geometry[_]]) extends Op2(g,other) ({
  (g,other) => Result(Feature(g.geom.union(other.geom),g.data))
})
