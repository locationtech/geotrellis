package geotrellis.feature

import com.vividsolutions.jts.{geom => jts}

abstract sealed trait Result
object Result {
  implicit def jtsToResult(geom: jts.Geometry): Result =
    geom match {
      case p: jts.Point => PointResult(p)
      case l: jts.LineString => LineResult(l)
      case p: jts.Polygon => PolygonResult(p)
      case mp: jts.MultiPoint => PointSetResult(mp)
      case ml: jts.MultiLineString => LineSetResult(ml)
      case mp: jts.MultiPolygon => PolygonSetResult(mp)
      case gc: jts.GeometryCollection => GeometryCollectionResult(gc)
      case _ => NoResult
    }
}

// -- Intersection

abstract sealed trait PointIntersectionResult
object PointIntersectionResult {
  implicit def jtsToResult(geom: jts.Geometry): PointIntersectionResult =
    geom match {
      case p: jts.Point => PointResult(p)
      case _ => NoResult
    }
}

abstract sealed trait LineLineIntersectionResult
object LineLineIntersectionResult {
  implicit def jtsToResult(geom: jts.Geometry): LineLineIntersectionResult =
    geom match {
      case p: jts.Point => PointResult(p)
      case l: jts.LineString => LineResult(l)
      case _ => NoResult
    }
}

abstract sealed trait PolygonLineIntersectionResult
object PolygonLineIntersectionResult {
  implicit def jtsToResult(geom: jts.Geometry): PolygonLineIntersectionResult =
    geom match {
      case p: jts.Point => PointResult(p)
      case l: jts.LineString => LineResult(l)
      case ml: jts.MultiLineString => LineSetResult(ml)
      case _ => NoResult
    }
}

abstract sealed trait PolygonPolygonIntersectionResult
object PolygonPolygonIntersectionResult {
  implicit def jtsToResult(geom: jts.Geometry): PolygonPolygonIntersectionResult =
    geom match {
      case p: jts.Point => PointResult(p)
      case l: jts.LineString => LineResult(l)
      case p: jts.Polygon => PolygonResult(p)
      case mp: jts.MultiPoint => PointSetResult(mp)
      case ml: jts.MultiLineString => LineSetResult(ml)
      case mp: jts.MultiPolygon => PolygonSetResult(mp)
      case gc: jts.GeometryCollection => GeometryCollectionResult(gc)
      case _ => NoResult
    }
}

abstract sealed trait PointSetIntersectionResult
object PointSetIntersectionResult {
  implicit def jtsToResult(geom: jts.Geometry): PointSetIntersectionResult =
    geom match {
      case p: jts.Point => if (p.isEmpty) NoResult else PointResult(p)
      case mp: jts.MultiPoint => PointSetResult(mp)
      case x => 
        sys.error(s"Unexpected result for PointSet intersection: ${geom.getGeometryType}")
    }
}

abstract sealed trait LineSetIntersectionResult
object LineSetIntersectionResult {
  implicit def jtsToResult(geom: jts.Geometry): LineSetIntersectionResult =
    geom match {
      case p: jts.Point => PointResult(p)
      case l: jts.LineString => if(l.isEmpty) NoResult else LineResult(l)
      case mp: jts.MultiPoint => PointSetResult(mp)
      case ml: jts.MultiLineString => LineSetResult(ml)
      case gc: jts.GeometryCollection => GeometryCollectionResult(gc)
      case x => 
        sys.error(s"Unexpected result for LineSet intersection: ${geom.getGeometryType}")
    }
}

abstract sealed trait PolygonSetIntersectionResult
object PolygonSetIntersectionResult {
  implicit def jtsToResult(geom: jts.Geometry): PolygonSetIntersectionResult =
    geom match {
      case p: jts.Point => PointResult(p)
      case l: jts.LineString => LineResult(l)
      case p: jts.Polygon => if(p.isEmpty) NoResult else PolygonResult(p)
      case mp: jts.MultiPoint => PointSetResult(mp)
      case ml: jts.MultiLineString => LineSetResult(ml)
      case mp: jts.MultiPolygon => PolygonSetResult(mp)
      case gc: jts.GeometryCollection => GeometryCollectionResult(gc)
      case _ => 
        sys.error(s"Unexpected result for PolygonSet intersection: ${geom.getGeometryType}")
    }
}

// -- Union

abstract sealed trait PointZeroDimensionsUnionResult
object PointZeroDimensionsUnionResult {
  implicit def jtsToResult(geom: jts.Geometry): PointZeroDimensionsUnionResult =
    geom match {
      case p: jts.Point => PointResult(p)
      case mp: jts.MultiPoint => PointSetResult(mp)
      case _ => 
        sys.error(s"Unexpected result for Point Point union: ${geom.getGeometryType}")
    }
}

abstract sealed trait PointLineUnionResult
object PointLineUnionResult {
  implicit def jtsToResult(geom: jts.Geometry): PointLineUnionResult =
    geom match {
      case l: jts.LineString => LineResult(l)
      case gc: jts.GeometryCollection => GeometryCollectionResult(gc)
      case _ => 
        sys.error(s"Unexpected result for Line Point union: ${geom.getGeometryType}")
    }
}

abstract sealed trait PointLineSetUnionResult
object PointLineSetUnionResult {
  implicit def jtsToResult(geom: jts.Geometry): PointLineSetUnionResult =
    geom match {
      case l: jts.LineString => LineResult(l)
      case ml: jts.MultiLineString => LineSetResult(ml)
      case gc: jts.GeometryCollection => GeometryCollectionResult(gc)
      case _ =>
        sys.error(s"Unexpected result for Point-LineSet union: ${geom.getGeometryType}")
    }
}

abstract sealed trait LineLineUnionResult
object LineLineUnionResult {
  implicit def jtsToResult(geom: jts.Geometry): LineLineUnionResult =
    geom match {
      case l: jts.LineString => LineResult(l)
      case ml: jts.MultiLineString => LineSetResult(ml)
      case _ => 
        sys.error(s"Unexpected result for Line Line union: ${geom.getGeometryType}")
    }
}

abstract sealed trait PolygonXUnionResult
object PolygonXUnionResult {
  implicit def jtsToResult(geom: jts.Geometry): PolygonXUnionResult =
    geom match {
      case gc: jts.GeometryCollection => GeometryCollectionResult(gc)
      case p: jts.Polygon => PolygonResult(Polygon(p))
      case _ => 
        sys.error(s"Unexpected result for Polygon union: ${geom.getGeometryType}")
    }
}

abstract sealed trait PolygonPolygonUnionResult
object PolygonPolygonUnionResult {
  implicit def jtsToResult(geom: jts.Geometry): PolygonPolygonUnionResult =
    geom match {
      case p: jts.Polygon => PolygonResult(p)
      case mp: jts.MultiPolygon => PolygonSetResult(mp)
      case _ =>
        sys.error(s"Unexpected result for Polygon-Polygon union: ${geom.getGeometryType}")
    }
}

abstract sealed trait PolygonSetUnionResult
object PolygonSetUnionResult {
  implicit def jtsToResult(geom: jts.Geometry): PolygonSetUnionResult =
    geom match {
      case p: jts.Polygon => PolygonResult(p)
      case mp: jts.MultiPolygon => PolygonSetResult(mp)
      case gc: jts.GeometryCollection => GeometryCollectionResult(gc)
      case _ =>
        sys.error(s"Unexpected result for Polygon set union: ${geom.getGeometryType}")
    }
}

// -- Difference

abstract sealed trait PointDifferenceResult
object PointDifferenceResult {
  implicit def jtsToResult(geom: jts.Geometry): PointDifferenceResult =
    geom match {
      case p: jts.Point => PointResult(p)
      case _ => NoResult
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
      case ml: jts.MultiLineString => LineSetResult(ml)
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
      case mp: jts.MultiPolygon => PolygonSetResult(mp)
      case _ => NoResult
    }
}

abstract sealed trait PointSetDifferenceResult
object PointSetDifferenceResult {
  implicit def jtsToResult(geom: jts.Geometry): PointSetDifferenceResult =
    geom match {
      case p: jts.Point => PointResult(p)
      case ps: jts.MultiPoint => PointSetResult(ps)
      case _ => NoResult
    }
}

abstract sealed trait LineSetPointDifferenceResult
object LineSetPointDifferenceResult {
  implicit def jtsToResult(geom: jts.Geometry): LineSetPointDifferenceResult =
    geom match {
      case ml: jts.MultiLineString => LineSetResult(ml)
      case _ =>
        sys.error(s"Unexpected result for Line-Point difference: ${geom.getGeometryType}")
    }
}

abstract sealed trait PolygonSetXDifferenceResult
object PolygonSetXDifferenceResult {
  implicit def jtsToResult(geom: jts.Geometry): PolygonSetXDifferenceResult =
    geom match {
      case mp: jts.MultiPolygon => PolygonSetResult(mp)
      case _ =>
        sys.error(s"Unexpected result for Polygon difference: ${geom.getGeometryType}")
    }
}

// -- Boundary

abstract sealed trait LineBoundaryResult
object LineBoundaryResult {
  implicit def jtsToResult(geom: jts.Geometry): LineBoundaryResult =
    geom match {
      case mp: jts.MultiPoint => PointSetResult(mp)
      case _ => NoResult
    }
}

abstract sealed trait PolygonBoundaryResult
object PolygonBoundaryResult {
  implicit def jtsToResult(geom: jts.Geometry): PolygonBoundaryResult =
    geom match {
      case l: jts.LineString => LineResult(l)
      case ml: jts.MultiLineString => LineSetResult(ml)
      case _ =>
        sys.error(s"Unexpected result for Polygon boundary: ${geom.getGeometryType}")
    }
}

// -- SymDifference

abstract sealed trait PointPointSymDifferenceResult
object PointPointSymDifferenceResult {
  implicit def jtsToResult(geom: jts.Geometry): PointPointSymDifferenceResult =
    geom match {
      case mp: jts.MultiPoint => PointSetResult(mp)
      case _ => NoResult
    }
}

abstract sealed trait ZeroDimensionsPointSetSymDifferenceResult
object ZeroDimensionsPointSetSymDifferenceResult {
  implicit def jtsToResult(geom: jts.Geometry): ZeroDimensionsPointSetSymDifferenceResult =
    geom match {
      case p: jts.Point => PointResult(p)
      case mp: jts.MultiPoint => PointSetResult(mp)
      case _ => NoResult
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

abstract sealed trait ZeroDimensionsLineSetSymDifferenceResult
object ZeroDimensionsLineSetSymDifferenceResult {
  implicit def jtsToResult(geom: jts.Geometry): ZeroDimensionsLineSetSymDifferenceResult =
    geom match {
      case l: jts.LineString => LineResult(l)
      case ml: jts.MultiLineString => LineSetResult(ml)
      case gc: jts.GeometryCollection => GeometryCollectionResult(gc)
      case _ =>
        sys.error(s"Unexpected result for Point-Line symDifference: ${geom.getGeometryType}")
    }
}

abstract sealed trait ZeroDimensionsPolygonSymDifferenceResult
object ZeroDimensionsPolygonSymDifferenceResult {
  implicit def jtsToResult(geom: jts.Geometry): ZeroDimensionsPolygonSymDifferenceResult =
    geom match {
      case p: jts.Polygon => PolygonResult(p)
      case gc: jts.GeometryCollection => GeometryCollectionResult(gc)
      case _ =>
        sys.error(s"Unexpected result for Point-Polygon symDifference: ${geom.getGeometryType}")
    }
}

abstract sealed trait ZeroDimensionsPolygonSetSymDifferenceResult
object ZeroDimensionsPolygonSetSymDifferenceResult {
  implicit def jtsToResult(geom: jts.Geometry): ZeroDimensionsPolygonSetSymDifferenceResult =
    geom match {
      case p: jts.Polygon => PolygonResult(p)
      case mp: jts.MultiPolygon => PolygonSetResult(mp)
      case gc: jts.GeometryCollection => GeometryCollectionResult(gc)
      case _ =>
        sys.error(s"Unexpected result for Point-Polygon symDifference: ${geom.getGeometryType}")
    }
}

abstract sealed trait OneDimensionsSymDifferenceResult
object OneDimensionsSymDifferenceResult {
  implicit def jtsToResult(geom: jts.Geometry): OneDimensionsSymDifferenceResult =
    geom match {
      case l: jts.LineString => LineResult(l)
      case ml: jts.MultiLineString => LineSetResult(ml)
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

abstract sealed trait OneDimensionsPolygonSetSymDifferenceResult
object OneDimensionsPolygonSetSymDifferenceResult {
  implicit def jtsToResult(geom: jts.Geometry): OneDimensionsPolygonSetSymDifferenceResult =
    geom match {
      case p: jts.Polygon => PolygonResult(p)
      case mp: jts.MultiPolygon => PolygonSetResult(mp)
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
      case mp: jts.MultiPolygon => PolygonSetResult(mp)
      case _ => NoResult
    }
}

case object NoResult extends Result
  with PointIntersectionResult
  with LineLineIntersectionResult
  with PolygonLineIntersectionResult
  with PolygonPolygonIntersectionResult
  with PointSetIntersectionResult
  with LineSetIntersectionResult
  with PolygonSetIntersectionResult
  with LineBoundaryResult
  with PointDifferenceResult
  with LineXDifferenceResult
  with PolygonPolygonDifferenceResult
  with PointSetDifferenceResult
  with PointPointSymDifferenceResult
  with OneDimensionsSymDifferenceResult
  with TwoDimensionsSymDifferenceResult
  with ZeroDimensionsPointSetSymDifferenceResult

case class PointResult(p: Point) extends Result
  with PointIntersectionResult
  with LineLineIntersectionResult
  with PolygonLineIntersectionResult
  with PolygonPolygonIntersectionResult
  with PointSetIntersectionResult
  with LineSetIntersectionResult
  with PolygonSetIntersectionResult
  with PointZeroDimensionsUnionResult
  with PointDifferenceResult
  with PointSetDifferenceResult
  with ZeroDimensionsPointSetSymDifferenceResult

case class LineResult(l: Line) extends Result
  with LineLineIntersectionResult
  with PolygonLineIntersectionResult
  with PolygonPolygonIntersectionResult
  with LineSetIntersectionResult
  with PolygonSetIntersectionResult
  with PointLineUnionResult
  with LineLineUnionResult
  with LinePointDifferenceResult
  with LineXDifferenceResult
  with PointLineSetUnionResult
  with PolygonBoundaryResult
  with ZeroDimensionsLineSymDifferenceResult
  with OneDimensionsSymDifferenceResult
  with ZeroDimensionsLineSetSymDifferenceResult

case class PolygonResult(p: Polygon) extends Result
  with PolygonPolygonIntersectionResult
  with PolygonXUnionResult
  with PolygonPolygonUnionResult
  with PolygonSetIntersectionResult
  with PolygonSetUnionResult
  with PolygonXDifferenceResult
  with PolygonPolygonDifferenceResult
  with ZeroDimensionsPolygonSymDifferenceResult
  with OneDimensionsPolygonSymDifferenceResult
  with TwoDimensionsSymDifferenceResult
  with ZeroDimensionsPolygonSetSymDifferenceResult
  with OneDimensionsPolygonSetSymDifferenceResult

case class PointSetResult(ps: Set[Point]) extends Result
  with PolygonPolygonIntersectionResult
  with PointSetIntersectionResult
  with LineSetIntersectionResult
  with PolygonSetIntersectionResult
  with PointZeroDimensionsUnionResult
  with LineBoundaryResult
  with PointSetDifferenceResult
  with PointPointSymDifferenceResult
  with ZeroDimensionsPointSetSymDifferenceResult

case class LineSetResult(ls: Set[Line]) extends Result
  with PolygonLineIntersectionResult
  with PolygonPolygonIntersectionResult
  with LineSetIntersectionResult
  with PolygonSetIntersectionResult
  with LineLineUnionResult
  with LinePointDifferenceResult
  with LineXDifferenceResult
  with PointLineSetUnionResult
  with LineSetPointDifferenceResult
  with PolygonBoundaryResult
  with OneDimensionsSymDifferenceResult
  with ZeroDimensionsLineSetSymDifferenceResult

object LineSetResult {
  implicit def jtsToResult(geom: jts.Geometry): LineSetResult =
    geom match {
      case ml: jts.MultiLineString => LineSetResult(ml)
      case _ =>
        sys.error(s"Unexpected result: ${geom.getGeometryType}")
    }
}

case class PolygonSetResult(ps: Set[Polygon]) extends Result
  with PolygonPolygonIntersectionResult
  with PolygonPolygonUnionResult
  with PolygonSetIntersectionResult
  with PolygonSetUnionResult
  with PolygonPolygonDifferenceResult
  with PolygonSetXDifferenceResult
  with TwoDimensionsSymDifferenceResult
  with ZeroDimensionsPolygonSetSymDifferenceResult
  with OneDimensionsPolygonSetSymDifferenceResult

case class GeometryCollectionResult(gc: GeometryCollection) extends Result
  with PolygonPolygonIntersectionResult
  with LineSetIntersectionResult
  with PolygonSetIntersectionResult
  with PointLineUnionResult
  with PolygonXUnionResult
  with PolygonSetUnionResult
  with PointLineSetUnionResult
  with ZeroDimensionsLineSymDifferenceResult
  with ZeroDimensionsPolygonSymDifferenceResult
  with OneDimensionsPolygonSymDifferenceResult
  with ZeroDimensionsLineSetSymDifferenceResult
  with ZeroDimensionsPolygonSetSymDifferenceResult
  with OneDimensionsPolygonSetSymDifferenceResult
