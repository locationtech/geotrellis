package geotrellis.vector.io.shape.reader

import geotrellis.vector._

case class ShapeRecord(geom: Geometry, data: Map[String, ShapeDBaseRecord])
