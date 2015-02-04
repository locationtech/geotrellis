package geotrellis.vector.io.shape.reader

import geotrellis.vector._

abstract sealed class ShapePointRecord

case class PointPointRecord(point: Point) extends ShapePointRecord

case class MultiPointPointRecord(multiPoint: MultiPoint) extends ShapePointRecord

case class MultiLinePointRecord(multiLine: MultiLine) extends ShapePointRecord

case class MultiPolygonPointRecord(multiPolygon: MultiPolygon) extends ShapePointRecord
