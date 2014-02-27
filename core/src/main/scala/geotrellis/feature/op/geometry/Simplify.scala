package geotrellis.feature.op.geometry

import geotrellis._
import geotrellis.feature._

import com.vividsolutions.jts.simplify.TopologyPreservingSimplifier

/**
  * Simplify a polygon or multipolygon.
  *
  * This operation uses a topology preserving simplifier that ensures the result has the same
  * characteristics as the input.  
  *
  * @param g  Geometry to simplify
  * @param distance  Distance tolerance for simplification
  *
  *
  *  @see [[http://tsusiatsoftware.net/jts/javadoc/com/vividsolutions/jts/simplify/TopologyPreservingSimplifier.html "JTS documentation"]]
  */
case class Simplify[A](g:Op[Geometry[A]], distanceTolerance:Op[Double]) extends Op2(g,distanceTolerance) ({
  (g,distanceTolerance) => 
    Result(g.mapGeom( TopologyPreservingSimplifier.simplify(_, distanceTolerance)))
})
