package geotrellis.vector

import com.vividsolutions.jts.{geom => jts}

trait MultiGeometry extends Geometry {
  /** Computes a area containing these geometries and buffered by size d. */
  def buffer(d: Double): TwoDimensionsTwoDimensionsUnionResult =
    jtsGeom.buffer(d)

  /**
   * Returns the minimum extent that contains all the points
   * of this MultiGeometry.
   */
  lazy val envelope: Extent =
    if(jtsGeom.isEmpty) Extent(0.0, 0.0, 0.0, 0.0)
    else jtsGeom.getEnvelopeInternal

}
