package geotrellis.raster.io.shape.reader

import geotrellis.vector._

case class ShapeRecord(shape: ShapePointRecord, data: Map[String, ShapeDBaseRecord])
