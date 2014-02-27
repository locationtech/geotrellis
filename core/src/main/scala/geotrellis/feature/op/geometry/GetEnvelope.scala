package geotrellis.feature.op.geometry

import geotrellis._
import geotrellis.feature._

/**
 * Returns this Geometry's bounding box.
 * 
 * @see [[http://www.vividsolutions.com/jts/javadoc/com/vividsolutions/jts/geom/Geometry.html#getEnvelope()]]
 */
case class GetEnvelope[A](f:Op[Geometry[A]]) extends Op1(f) ({
  (f) => Result(f.mapGeom(_.getEnvelope))
})

