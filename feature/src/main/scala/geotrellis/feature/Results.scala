package geotrellis.feature

import com.vividsolutions.jts.{geom => jts}

abstract sealed trait Result
object Result {
  implicit def jtsToResult(geom: jts.Geometry): Result =
    geom match {
      case g: jts.Geometry if g.isEmpty => NoResult
      case p: jts.Point => PointResult(p)
      case l: jts.LineString => LineResult(l)
      case p: jts.Polygon => PolygonResult(p)
      case mp: jts.MultiPoint => MultiPointResult(mp)
      case ml: jts.MultiLineString => MultiLineResult(ml)
      case mp: jts.MultiPolygon => MultiPolygonResult(mp)
      case gc: jts.GeometryCollection => GeometryCollectionResult(gc)
      case _ =>
        sys.error(s"Unexpected result: ${geom.getGeometryType}")
    }
}

// -- Intersection

abstract sealed trait PointGeometryIntersectionResult
object PointGeometryIntersectionResult {
  implicit def jtsToResult(geom: jts.Geometry): PointGeometryIntersectionResult =
    geom match {
      case g: jts.Geometry if g.isEmpty => NoResult
      case p: jts.Point => PointResult(p)
      case _ =>
        sys.error(s"Unexpected result for Point-Geometry intersection: ${geom.getGeometryType}")
    }
}

abstract sealed trait LineLineIntersectionResult
object LineLineIntersectionResult {
  implicit def jtsToResult(geom: jts.Geometry): LineLineIntersectionResult =
    geom match {
      case g: jts.Geometry if g.isEmpty => NoResult
      case p: jts.Point => PointResult(p)
      case l: jts.LineString => LineResult(l)
      case mp: jts.MultiPoint => MultiPointResult(mp)
      case ml: jts.MultiLineString => MultiLineResult(ml)
      case _ => sys.error(s"Unexpected result for Line-Line intersection: ${geom.getGeometryType}")
    }
}

abstract sealed trait LinePolygonIntersectionResult
object LinePolygonIntersectionResult {
  implicit def jtsToResult(geom: jts.Geometry): LinePolygonIntersectionResult =
    geom match {
      case g: jts.Geometry if g.isEmpty => NoResult
      case p: jts.Point => PointResult(p)
      case l: jts.LineString => LineResult(l)
      case mp: jts.MultiPoint => MultiPointResult(mp)
      case ml: jts.MultiLineString => MultiLineResult(ml)
      case gc: jts.GeometryCollection => GeometryCollectionResult(gc)
      case _ => sys.error(s"Unexpected result for Line-Polygon intersection: ${geom.getGeometryType}")
    }
}

abstract sealed trait PolygonPolygonIntersectionResult
object PolygonPolygonIntersectionResult {
  implicit def jtsToResult(geom: jts.Geometry): PolygonPolygonIntersectionResult =
    geom match {
      case p: jts.Point => PointResult(p)
      case l: jts.LineString => LineResult(l)
      case p: jts.Polygon => PolygonResult(p)
      case mp: jts.MultiPoint => MultiPointResult(mp)
      case ml: jts.MultiLineString => MultiLineResult(ml)
      case mp: jts.MultiPolygon => MultiPolygonResult(mp)
      case gc: jts.GeometryCollection => GeometryCollectionResult(gc)
      case _ => NoResult
    }
}

abstract sealed trait MultiPointIntersectionResult
object MultiPointIntersectionResult {
  implicit def jtsToResult(geom: jts.Geometry): MultiPointIntersectionResult =
    geom match {
      case p: jts.Point => if (p.isEmpty) NoResult else PointResult(p)
      case mp: jts.MultiPoint => MultiPointResult(mp)
      case x => 
        sys.error(s"Unexpected result for MultiPoint intersection: ${geom.getGeometryType}")
    }
}

abstract sealed trait MultiLineIntersectionResult
object MultiLineIntersectionResult {
  implicit def jtsToResult(geom: jts.Geometry): MultiLineIntersectionResult =
    geom match {
      case p: jts.Point => PointResult(p)
      case l: jts.LineString => if(l.isEmpty) NoResult else LineResult(l)
      case mp: jts.MultiPoint => MultiPointResult(mp)
      case ml: jts.MultiLineString => MultiLineResult(ml)
      case gc: jts.GeometryCollection => GeometryCollectionResult(gc)
      case x => 
        sys.error(s"Unexpected result for MultiLine intersection: ${geom.getGeometryType}")
    }
}

abstract sealed trait MultiPolygonIntersectionResult
object MultiPolygonIntersectionResult {
  implicit def jtsToResult(geom: jts.Geometry): MultiPolygonIntersectionResult =
    geom match {
      case p: jts.Point => PointResult(p)
      case l: jts.LineString => LineResult(l)
      case p: jts.Polygon => if(p.isEmpty) NoResult else PolygonResult(p)
      case mp: jts.MultiPoint => MultiPointResult(mp)
      case ml: jts.MultiLineString => MultiLineResult(ml)
      case mp: jts.MultiPolygon => MultiPolygonResult(mp)
      case gc: jts.GeometryCollection => GeometryCollectionResult(gc)
      case _ => 
        sys.error(s"Unexpected result for MultiPolygon intersection: ${geom.getGeometryType}")
    }
}

// -- Union

abstract sealed trait PointZeroDimensionsUnionResult
object PointZeroDimensionsUnionResult {
  implicit def jtsToResult(geom: jts.Geometry): PointZeroDimensionsUnionResult =
    geom match {
      case p: jts.Point => PointResult(p)
      case mp: jts.MultiPoint => MultiPointResult(mp)
      case _ => 
        sys.error(s"Unexpected result for Point-ZeroDimensions union: ${geom.getGeometryType}")
    }
}

abstract sealed trait PointLineUnionResult
object PointLineUnionResult {
  implicit def jtsToResult(geom: jts.Geometry): PointLineUnionResult =
    geom match {
      case l: jts.LineString => LineResult(l)
      case gc: jts.GeometryCollection => GeometryCollectionResult(gc)
      case _ => 
        sys.error(s"Unexpected result for Line-Point union: ${geom.getGeometryType}")
    }
}

abstract sealed trait PointMultiLineUnionResult
object PointMultiLineUnionResult {
  implicit def jtsToResult(geom: jts.Geometry): PointMultiLineUnionResult =
    geom match {
      case l: jts.LineString => LineResult(l)  // e.g. MultiLine has only 1 line
      case ml: jts.MultiLineString => MultiLineResult(ml)
      case gc: jts.GeometryCollection => GeometryCollectionResult(gc)
      case _ =>
        sys.error(s"Unexpected result for Point-MultiLine union: ${geom.getGeometryType}")
    }
}

abstract sealed trait LineLineUnionResult
object LineLineUnionResult {
  implicit def jtsToResult(geom: jts.Geometry): LineLineUnionResult =
    geom match {
      case l: jts.LineString => LineResult(l)
      case ml: jts.MultiLineString => MultiLineResult(ml)
      case _ => 
        sys.error(s"Unexpected result for Line-Line union: ${geom.getGeometryType}")
    }
}

abstract sealed trait AtMostOneDimensionsPolygonUnionResult
object AtMostOneDimensionsPolygonUnionResult {
  implicit def jtsToResult(geom: jts.Geometry): AtMostOneDimensionsPolygonUnionResult =
    geom match {
      case p: jts.Polygon => PolygonResult(Polygon(p))
      case gc: jts.GeometryCollection => GeometryCollectionResult(gc)
      case _ =>
        sys.error(s"Unexpected result for AtMostOneDimensions-Polygon union: ${geom.getGeometryType}")
    }
}

abstract sealed trait PolygonPolygonUnionResult
object PolygonPolygonUnionResult {
  implicit def jtsToResult(geom: jts.Geometry): PolygonPolygonUnionResult =
    geom match {
      case p: jts.Polygon => PolygonResult(p)
      case mp: jts.MultiPolygon => MultiPolygonResult(mp)
      case _ =>
        sys.error(s"Unexpected result for Polygon-Polygon union: ${geom.getGeometryType}")
    }
}

abstract sealed trait AtMostOneDimensionsMultiPolygonUnionResult
object AtMostOneDimensionsMultiPolygonUnionResult {
  implicit def jtsToResult(geom: jts.Geometry): AtMostOneDimensionsMultiPolygonUnionResult =
    geom match {
      case p: jts.Polygon => PolygonResult(p)
      case mp: jts.MultiPolygon => MultiPolygonResult(mp)
      case gc: jts.GeometryCollection => GeometryCollectionResult(gc)
      case _ =>
        sys.error(s"Unexpected result for AtMostOneDimensions-MultiPolygon union: ${geom.getGeometryType}")
    }
}

// -- Difference

abstract sealed trait PointGeometryDifferenceResult
object PointGeometryDifferenceResult {
  implicit def jtsToResult(geom: jts.Geometry): PointGeometryDifferenceResult =
    geom match {
      case g: jts.Geometry if g.isEmpty => NoResult
      case p: jts.Point => PointResult(p)
      case _ =>
        sys.error(s"Unexpected result for Point-Geometry difference: ${geom.getGeometryType}")
    }
}

abstract sealed trait LinePointDifferenceResult
object LinePointDifferenceResult {
  implicit def jtsToResult(geom: jts.Geometry): LinePointDifferenceResult =
    geom match {
      case l: jts.LineString => LineResult(l)
      case _ =>
        sys.error(s"Unexpected result for Line-Point difference: ${geom.getGeometryType}")
    }
}

abstract sealed trait LineXDifferenceResult
object LineXDifferenceResult {
  implicit def jtsToResult(geom: jts.Geometry): LineXDifferenceResult =
    geom match {
      case l: jts.LineString => LineResult(l)
      case ml: jts.MultiLineString => MultiLineResult(ml)
      case _ => NoResult
    }
}

abstract sealed trait PolygonXDifferenceResult
object PolygonXDifferenceResult {
  implicit def jtsToResult(geom: jts.Geometry): PolygonXDifferenceResult =
    geom match {
      case p: jts.Polygon => PolygonResult(p)
      case _ =>
        sys.error(s"Unexpected result for Polygon difference: ${geom.getGeometryType}")
    }
}

abstract sealed trait PolygonPolygonDifferenceResult
object PolygonPolygonDifferenceResult {
  implicit def jtsToResult(geom: jts.Geometry): PolygonPolygonDifferenceResult =
    geom match {
      case p: jts.Polygon => PolygonResult(p)
      case mp: jts.MultiPolygon => MultiPolygonResult(mp)
      case _ => NoResult
    }
}

abstract sealed trait MultiPointDifferenceResult
object MultiPointDifferenceResult {
  implicit def jtsToResult(geom: jts.Geometry): MultiPointDifferenceResult =
    geom match {
      case p: jts.Point => PointResult(p)
      case ps: jts.MultiPoint => MultiPointResult(ps)
      case _ => NoResult
    }
}

abstract sealed trait MultiLinePointDifferenceResult
object MultiLinePointDifferenceResult {
  implicit def jtsToResult(geom: jts.Geometry): MultiLinePointDifferenceResult =
    geom match {
      case ml: jts.MultiLineString => MultiLineResult(ml)
      case _ =>
        sys.error(s"Unexpected result for Line-Point difference: ${geom.getGeometryType}")
    }
}

abstract sealed trait MultiPolygonXDifferenceResult
object MultiPolygonXDifferenceResult {
  implicit def jtsToResult(geom: jts.Geometry): MultiPolygonXDifferenceResult =
    geom match {
      case mp: jts.MultiPolygon => MultiPolygonResult(mp)
      case _ =>
        sys.error(s"Unexpected result for Polygon difference: ${geom.getGeometryType}")
    }
}

// -- Boundary

abstract sealed trait LineBoundaryResult
object LineBoundaryResult {
  implicit def jtsToResult(geom: jts.Geometry): LineBoundaryResult =
    geom match {
      case mp: jts.MultiPoint => MultiPointResult(mp)
      case _ => NoResult
    }
}

abstract sealed trait PolygonBoundaryResult
object PolygonBoundaryResult {
  implicit def jtsToResult(geom: jts.Geometry): PolygonBoundaryResult =
    geom match {
      case l: jts.LineString => LineResult(l)
      case ml: jts.MultiLineString => MultiLineResult(ml)
      case _ =>
        sys.error(s"Unexpected result for Polygon boundary: ${geom.getGeometryType}")
    }
}

// -- SymDifference

abstract sealed trait PointPointSymDifferenceResult
object PointPointSymDifferenceResult {
  implicit def jtsToResult(geom: jts.Geometry): PointPointSymDifferenceResult =
    geom match {
      case g: jts.Geometry if g.isEmpty => NoResult
      case mp: jts.MultiPoint => MultiPointResult(mp)
      case _ =>
        sys.error(s"Unexpected result for Point-Point symDifference: ${geom.getGeometryType}")

    }
}

abstract sealed trait ZeroDimensionsMultiPointSymDifferenceResult
object ZeroDimensionsMultiPointSymDifferenceResult {
  implicit def jtsToResult(geom: jts.Geometry): ZeroDimensionsMultiPointSymDifferenceResult =
    geom match {
      case g: jts.Geometry if g.isEmpty => NoResult
      case p: jts.Point => PointResult(p)
      case mp: jts.MultiPoint => MultiPointResult(mp)
      case _ =>
        sys.error(s"Unexpected result for ZeroDimensions-MultiPoint symDifference: ${geom.getGeometryType}")
    }
}

abstract sealed trait ZeroDimensionsLineSymDifferenceResult
object ZeroDimensionsLineSymDifferenceResult {
  implicit def jtsToResult(geom: jts.Geometry): ZeroDimensionsLineSymDifferenceResult =
    geom match {
      case l: jts.LineString => LineResult(l)
      case gc: jts.GeometryCollection => GeometryCollectionResult(gc)
      case _ =>
        sys.error(s"Unexpected result for ZeroDimensions-Line symDifference: ${geom.getGeometryType}")
    }
}

abstract sealed trait ZeroDimensionsMultiLineSymDifferenceResult
object ZeroDimensionsMultiLineSymDifferenceResult {
  implicit def jtsToResult(geom: jts.Geometry): ZeroDimensionsMultiLineSymDifferenceResult =
    geom match {
      case l: jts.LineString => LineResult(l)
      case ml: jts.MultiLineString => MultiLineResult(ml)
      case gc: jts.GeometryCollection => GeometryCollectionResult(gc)
      case _ =>
        sys.error(s"Unexpected result for ZeroDimensions-MultiLine symDifference: ${geom.getGeometryType}")
    }
}

abstract sealed trait ZeroDimensionsPolygonSymDifferenceResult
object ZeroDimensionsPolygonSymDifferenceResult {
  implicit def jtsToResult(geom: jts.Geometry): ZeroDimensionsPolygonSymDifferenceResult =
    geom match {
      case p: jts.Polygon => PolygonResult(p)
      case gc: jts.GeometryCollection => GeometryCollectionResult(gc)
      case _ =>
        sys.error(s"Unexpected result for ZeroDimensions-Polygon symDifference: ${geom.getGeometryType}")
    }
}

abstract sealed trait ZeroDimensionsMultiPolygonSymDifferenceResult
object ZeroDimensionsMultiPolygonSymDifferenceResult {
  implicit def jtsToResult(geom: jts.Geometry): ZeroDimensionsMultiPolygonSymDifferenceResult =
    geom match {
      case p: jts.Polygon => PolygonResult(p)
      case mp: jts.MultiPolygon => MultiPolygonResult(mp)
      case gc: jts.GeometryCollection => GeometryCollectionResult(gc)
      case _ =>
        sys.error(s"Unexpected result for ZeroDimensions-MultiPolygon symDifference: ${geom.getGeometryType}")
    }
}

abstract sealed trait OneDimensionsSymDifferenceResult
object OneDimensionsSymDifferenceResult {
  implicit def jtsToResult(geom: jts.Geometry): OneDimensionsSymDifferenceResult =
    geom match {
      case l: jts.LineString => LineResult(l)
      case ml: jts.MultiLineString => MultiLineResult(ml)
      case _ => NoResult
    }
}

abstract sealed trait OneDimensionsPolygonSymDifferenceResult
object OneDimensionsPolygonSymDifferenceResult {
  implicit def jtsToResult(geom: jts.Geometry): OneDimensionsPolygonSymDifferenceResult =
    geom match {
      case p: jts.Polygon => PolygonResult(p)
      case gc: jts.GeometryCollection => GeometryCollectionResult(gc)
      case _ =>
        sys.error(s"Unexpected result for Line-Polygon symDifference: ${geom.getGeometryType}")
    }
}

abstract sealed trait OneDimensionsMultiPolygonSymDifferenceResult
object OneDimensionsMultiPolygonSymDifferenceResult {
  implicit def jtsToResult(geom: jts.Geometry): OneDimensionsMultiPolygonSymDifferenceResult =
    geom match {
      case p: jts.Polygon => PolygonResult(p)
      case mp: jts.MultiPolygon => MultiPolygonResult(mp)
      case gc: jts.GeometryCollection => GeometryCollectionResult(gc)
      case _ =>
        sys.error(s"Unexpected result for Line-Polygon symDifference: ${geom.getGeometryType}")
    }
}

abstract sealed trait TwoDimensionsSymDifferenceResult
object TwoDimensionsSymDifferenceResult {
  implicit def jtsToResult(geom: jts.Geometry): TwoDimensionsSymDifferenceResult =
    geom match {
      case p: jts.Polygon => PolygonResult(p)
      case mp: jts.MultiPolygon => MultiPolygonResult(mp)
      case _ => NoResult
    }
}

case object NoResult extends Result
  with PointGeometryIntersectionResult
  with LineLineIntersectionResult
  with LinePolygonIntersectionResult
  with PolygonPolygonIntersectionResult
  with MultiPointIntersectionResult
  with MultiLineIntersectionResult
  with MultiPolygonIntersectionResult
  with LineBoundaryResult
  with PointGeometryDifferenceResult
  with LineXDifferenceResult
  with PolygonPolygonDifferenceResult
  with MultiPointDifferenceResult
  with PointPointSymDifferenceResult
  with OneDimensionsSymDifferenceResult
  with TwoDimensionsSymDifferenceResult
  with ZeroDimensionsMultiPointSymDifferenceResult

case class PointResult(p: Point) extends Result
  with PointGeometryIntersectionResult
  with LineLineIntersectionResult
  with LinePolygonIntersectionResult
  with PolygonPolygonIntersectionResult
  with MultiPointIntersectionResult
  with MultiLineIntersectionResult
  with MultiPolygonIntersectionResult
  with PointZeroDimensionsUnionResult
  with PointGeometryDifferenceResult
  with MultiPointDifferenceResult
  with ZeroDimensionsMultiPointSymDifferenceResult

case class LineResult(l: Line) extends Result
  with LineLineIntersectionResult
  with LinePolygonIntersectionResult
  with PolygonPolygonIntersectionResult
  with MultiLineIntersectionResult
  with MultiPolygonIntersectionResult
  with PointLineUnionResult
  with LineLineUnionResult
  with LinePointDifferenceResult
  with LineXDifferenceResult
  with PointMultiLineUnionResult
  with PolygonBoundaryResult
  with ZeroDimensionsLineSymDifferenceResult
  with OneDimensionsSymDifferenceResult
  with ZeroDimensionsMultiLineSymDifferenceResult

case class PolygonResult(p: Polygon) extends Result
  with PolygonPolygonIntersectionResult
  with AtMostOneDimensionsPolygonUnionResult
  with PolygonPolygonUnionResult
  with MultiPolygonIntersectionResult
  with AtMostOneDimensionsMultiPolygonUnionResult
  with PolygonXDifferenceResult
  with PolygonPolygonDifferenceResult
  with ZeroDimensionsPolygonSymDifferenceResult
  with OneDimensionsPolygonSymDifferenceResult
  with TwoDimensionsSymDifferenceResult
  with ZeroDimensionsMultiPolygonSymDifferenceResult
  with OneDimensionsMultiPolygonSymDifferenceResult

case class MultiPointResult(ps: Set[Point]) extends Result
  with PolygonPolygonIntersectionResult
  with MultiPointIntersectionResult
  with MultiLineIntersectionResult
  with MultiPolygonIntersectionResult
  with PointZeroDimensionsUnionResult
  with LineBoundaryResult
  with MultiPointDifferenceResult
  with PointPointSymDifferenceResult
  with ZeroDimensionsMultiPointSymDifferenceResult
  with LinePolygonIntersectionResult
  with LineLineIntersectionResult

case class MultiLineResult(ls: Set[Line]) extends Result
  with LinePolygonIntersectionResult
  with PolygonPolygonIntersectionResult
  with MultiLineIntersectionResult
  with MultiPolygonIntersectionResult
  with LineLineUnionResult
  with LinePointDifferenceResult
  with LineXDifferenceResult
  with PointMultiLineUnionResult
  with MultiLinePointDifferenceResult
  with PolygonBoundaryResult
  with OneDimensionsSymDifferenceResult
  with ZeroDimensionsMultiLineSymDifferenceResult
  with LineLineIntersectionResult

object MultiLineResult {
  implicit def jtsToResult(geom: jts.Geometry): MultiLineResult =
    geom match {
      case ml: jts.MultiLineString => MultiLineResult(ml)
      case _ =>
        sys.error(s"Unexpected result: ${geom.getGeometryType}")
    }
}

case class MultiPolygonResult(ps: Set[Polygon]) extends Result
  with PolygonPolygonIntersectionResult
  with PolygonPolygonUnionResult
  with MultiPolygonIntersectionResult
  with AtMostOneDimensionsMultiPolygonUnionResult
  with PolygonPolygonDifferenceResult
  with MultiPolygonXDifferenceResult
  with TwoDimensionsSymDifferenceResult
  with ZeroDimensionsMultiPolygonSymDifferenceResult
  with OneDimensionsMultiPolygonSymDifferenceResult

case class GeometryCollectionResult(gc: GeometryCollection) extends Result
  with PolygonPolygonIntersectionResult
  with MultiLineIntersectionResult
  with MultiPolygonIntersectionResult
  with PointLineUnionResult
  with AtMostOneDimensionsPolygonUnionResult
  with AtMostOneDimensionsMultiPolygonUnionResult
  with PointMultiLineUnionResult
  with ZeroDimensionsLineSymDifferenceResult
  with ZeroDimensionsPolygonSymDifferenceResult
  with OneDimensionsPolygonSymDifferenceResult
  with ZeroDimensionsMultiLineSymDifferenceResult
  with ZeroDimensionsMultiPolygonSymDifferenceResult
  with OneDimensionsMultiPolygonSymDifferenceResult
  with LinePolygonIntersectionResult
