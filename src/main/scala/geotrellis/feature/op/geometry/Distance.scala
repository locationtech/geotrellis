package geotrellis.feature.op.geometry

import geotrellis._
import geotrellis.feature._
import com.vividsolutions.jts.geom

/** Returns the minimum distance between these two geometries.
  * 
  *  @param g      first geometry for distance calculation
  *  @param other  second geometry for distance calculation 
  * 
  *  @see [[http://tsusiatsoftware.net/jts/javadoc/com/vividsolutions/jts/geom/Geometry.html#distance(com.vividsolutions.jts.geom.Geometry) "JTS documentation"]]
  */
case class Distance(g:Op[Geometry[_]], other:Op[Geometry[_]]) extends Op2(g,other) ({
  (g,other) => Result(g.geom.distance(other.geom))
})

