package geotrellis.feature

import com.vividsolutions.jts.geom

abstract sealed class GeometryType

object GeometryType {
  implicit def jtsToGeometryType(g:geom.Geometry) = 
    stringToGeometryType(g.getGeometryType)

  implicit def stringToGeometryType(s:String) =
    s match {
      case "GeometryCollection" => GeometryCollectionType
      case "Point" => PointType
      case "LineString" => LineStringType
      case "LinearRing" => LinearRingType
      case "Polygon" => PolygonType
      case "MultiPoint" => MultiPointType
      case "MultiLineString" => MultiLineStringType
      case "MultiPolygon" => MultiPolygonType

    }
}

case object GeometryCollectionType extends GeometryType
case object PointType extends GeometryType
case object LineStringType extends GeometryType
case object LinearRingType extends GeometryType
case object PolygonType extends GeometryType
case object MultiPointType extends GeometryType
case object MultiLineStringType extends GeometryType
case object MultiPolygonType extends GeometryType

object Main {
  def main(args:Array[String]) = {

  }
}

