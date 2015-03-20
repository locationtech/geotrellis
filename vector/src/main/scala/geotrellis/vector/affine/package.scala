package geotrellis.vector


package object affine {

  implicit class PointTransformations(val geom: Point) extends PointTransformationMethods
  implicit class LineTransformations(val geom: Line) extends LineTransformationMethods
  implicit class PolygonTransformations(val geom: Polygon) extends PolygonTransformationMethods

  implicit class MultiLineTransformations(val geom: MultiLine) extends MultiLineTransformationMethods
  implicit class MultiPointTransformations(val geom: MultiPoint) extends MultiPointTransformationMethods
  implicit class MultiPolygonTransformations(val geom: MultiPolygon) extends MultiPolygonTransformationMethods

  implicit class GeometryCollectionTransformations(val geom: GeometryCollection) extends GeometryCollectionTransformationMethods
}
