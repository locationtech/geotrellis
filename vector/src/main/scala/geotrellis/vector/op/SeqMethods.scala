package geotrellis.vector

import geotrellis.vector._

package object op {

  implicit class SeqLineExtensions(val lines: Traversable[Line]) {
    def unionGeometries() = MultiLine(lines).union
  }

  implicit class SeqPointExtensions(val points: Traversable[Point]) {
    def unionGeometries() = MultiPoint(points).union
  }

  implicit class SeqPolygonExtensions(val polygons: Traversable[Polygon]) {
    def unionGeometries() = MultiPolygon(polygons).union
  }

  implicit class SeqMultiLineExtensions(val multilines: Traversable[MultiLine]) {
    def unionGeometries() = MultiLine(multilines.map(_.lines).flatten).union
  }

  implicit class SeqMultiPointExtensions(val multipoints: Traversable[MultiPoint]) {
    def unionGeometries() = MultiPoint(multipoints.map(_.points).flatten).union
  }

  implicit class SeqMultiPolygonExtensions(val multipolygons: Traversable[MultiPolygon]) {
    def unionGeometries() = MultiPolygon(multipolygons.map(_.polygons).flatten).union
  }
}
