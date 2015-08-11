package geotrellis.vector

import com.vividsolutions.jts.{geom => jts}

trait MultiGeometry extends Geometry {
  /** Computes a area containing these geometries and buffered by size d. */
  def buffer(d: Double): TwoDimensionsTwoDimensionsUnionResult =
    jtsGeom.buffer(d)

}
