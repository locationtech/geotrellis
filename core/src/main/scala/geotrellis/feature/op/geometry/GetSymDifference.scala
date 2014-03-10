package geotrellis.feature.op.geometry

import geotrellis._
import geotrellis.feature._
import com.vividsolutions.jts.geom

/**
 * Returns a geometry defined by the points that are in one of the two geometries
 * but not the other.
 *  @param g      first geometry to intersect, whose data is preserved
 *  @param other  other geometry to intersect
 * 
 *  @tparam A    type of feature data
 *
 *  @see [[http://tsusiatsoftware.net/jts/javadoc/com/vividsolutions/jts/geom/Geometry.html#intersection(com.vividsolutions.jts.geom.Geometry) "JTS documentation"]]
 */
case class GetSymDifference[A](g:Op[Geometry[A]], other:Op[Geometry[_]]) extends Op2(g,other) ({
  (g,other) => Result(g.mapGeom(_.symDifference(other.geom)))
})
