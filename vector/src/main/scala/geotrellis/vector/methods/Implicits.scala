package geotrellis.vector.methods

import geotrellis.vector._

object Implicits extends Implicits

trait Implicits {
  implicit class withExtraPointMethods(val self: Point) extends ExtraPointMethods
  implicit class withExtraLineStringMethods(val self: LineString) extends ExtraLineStringMethods
  implicit class withExtraPolygonMethods(val self: Polygon) extends ExtraPolygonMethods
  implicit class withExtraMultiPointMethods(val self: MultiPoint) extends ExtraMultiPointMethods
  implicit class withExtraMultiLineStringMethods(val self: MultiLineString) extends ExtraMultiLineStringMethods
  implicit class withExtraMultiPolygonMethods(val self: MultiPolygon) extends ExtraMultiPolygonMethods
  implicit class withExtraGeometryMethods(val self: Geometry) extends ExtraGeometryMethods
  implicit class withExtraGeometryCollectionMethods(val self: GeometryCollection) extends ExtraGeometryCollectionMethods
}
