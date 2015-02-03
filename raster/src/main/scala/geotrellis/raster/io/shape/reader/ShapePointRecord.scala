package geotrellis.raster.io.shape.reader

import geotrellis.vector._

trait ShapePointRecord

case class PointPointRecord(point: Point) extends ShapePointRecord

case class MultiPointPointRecord(multiPoint: MultiPoint) extends ShapePointRecord

case class MultiLinePointRecord(multiLine: MultiLine) extends ShapePointRecord

case class MultiPolygonPointRecord(multiPolygon: MultiPolygon) extends ShapePointRecord
