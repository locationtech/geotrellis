package geotrellis.raster.io.shape.reader

import geotrellis.vector._

trait ShapeRecord

case class PointRecord(point: Point) extends ShapeRecord

case class MultiPointRecord(multiPoint: MultiPoint) extends ShapeRecord

case class MultiLineRecord(multiLine: MultiLine) extends ShapeRecord

case class PolygonRecord(polygon: Polygon) extends ShapeRecord

case class MultiPatchRecord(multiLine: MultiLine) extends ShapeRecord
