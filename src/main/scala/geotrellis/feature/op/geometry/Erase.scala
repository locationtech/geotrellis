package geotrellis.feature.op.geometry

import geotrellis._
import geotrellis.feature._
import com.vividsolutions.jts.geom

/**
 * Returns a geometry that contains the points in the first geometry that 
 * aren't in the second geometry.
 *  @param g      first geometry  
 *  @param other  other geometry 
 * 
 *  @tparam A    type of feature data
 *
 *  @see [[http://tsusiatsoftware.net/jts/javadoc/com/vividsolutions/jts/geom/Geometry.html#difference(com.vividsolutions.jts.geom.Geometry) "JTS documentation"]]
  */
case class Erase[A](g:Op[Geometry[A]], other:Op[Geometry[_]]) extends Op2(g,other) ({
  (g,other) => Result(g.mapGeom(_.difference(other.geom)))
})


object GetDifference {
/**
 * Returns a geometry that contains the points in the first geometry that 
 * aren't in the second geometry.  Creates a [geotrellis.feature.op.overlay.Erase] operation.
 *
 *  @param g      first geometry  
 *  @param other  other geometry 
 * 
 *  @tparam A    type of feature data
 *
 *  @see [[http://tsusiatsoftware.net/jts/javadoc/com/vividsolutions/jts/geom/Geometry.html#difference(com.vividsolutions.jts.geom.Geometry) "JTS documentation"]]
  */
  def apply[A](g:Op[Geometry[A]], other:Op[Geometry[_]]) = Erase(g, other)
}
