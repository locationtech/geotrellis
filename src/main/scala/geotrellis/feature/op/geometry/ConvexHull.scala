package geotrellis.feature.op.geometry

import geotrellis._
import geotrellis.feature._
import com.vividsolutions.jts.geom

/** Returns the convex hull of this geometry, which is the smallest polygon that contains it.
  *  @param g      geometry for convex hull calculation
  * 
  *  @tparam A    type of feature data
  *
  *  @see [[http://tsusiatsoftware.net/jts/javadoc/com/vividsolutions/jts/geom/Geometry.html#convexHull() "JTS documentation"]]
  */
case class ConvexHull[A](g:Op[Geometry[A]]) extends Op1(g) ({
  (g) => Result(g.mapGeom(_.convexHull))
})
