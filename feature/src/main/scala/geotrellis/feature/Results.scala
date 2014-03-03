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
      case mp: jts.MultiPoint => PointSetResult(mp)
      case ml: jts.MultiLineString => LineSetResult(ml)
      case mp: jts.MultiPolygon => PolygonSetResult(mp)
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

abstract sealed trait OneDimensionsAtLeastOneDimensionsIntersectionResult
object OneDimensionsAtLeastOneDimensionsIntersectionResult {
  implicit def jtsToResult(geom: jts.Geometry): OneDimensionsAtLeastOneDimensionsIntersectionResult =
    geom match {
      case g: jts.Geometry if g.isEmpty => NoResult
      case p: jts.Point => PointResult(p)
      case l: jts.LineString => LineResult(l)
      case mp: jts.MultiPoint => PointSetResult(mp)
      case ml: jts.MultiLineString => LineSetResult(ml)
      case gc: jts.GeometryCollection => GeometryCollectionResult(gc)
      case _ => sys.error(s"Unexpected result for Line-AtLeastOneDimensions intersection: ${geom.getGeometryType}")
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

abstract sealed trait PointSetGeometryIntersectionResult
object PointSetGeometryIntersectionResult {
  implicit def jtsToResult(geom: jts.Geometry): PointSetGeometryIntersectionResult =
    geom match {
      case g: jts.Geometry if g.isEmpty => NoResult
      case p: jts.Point => PointResult(p)
      case mp: jts.MultiPoint => PointSetResult(mp)
      case x => 
        sys.error(s"Unexpected result for PointSet-Geometry intersection: ${geom.getGeometryType}")
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

abstract sealed trait PointLineSetUnionResult
object PointLineSetUnionResult {
  implicit def jtsToResult(geom: jts.Geometry): PointLineSetUnionResult =
    geom match {
      case l: jts.LineString => LineResult(l)  // e.g. LineSet has only 1 line
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
      case mp: jts.MultiPolygon => PolygonSetResult(mp)
      case _ =>
        sys.error(s"Unexpected result for Polygon-Polygon union: ${geom.getGeometryType}")
    }
}

abstract sealed trait AtMostOneDimensionsPolygonSetUnionResult
object AtMostOneDimensionsPolygonSetUnionResult {
  implicit def jtsToResult(geom: jts.Geometry): AtMostOneDimensionsPolygonSetUnionResult =
    geom match {
      case p: jts.Polygon => PolygonResult(p)
      case mp: jts.MultiPolygon => PolygonSetResult(mp)
      case gc: jts.GeometryCollection => GeometryCollectionResult(gc)
      case _ =>
        sys.error(s"Unexpected result for AtMostOneDimensions-PolygonSet union: ${geom.getGeometryType}")
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

abstract sealed trait OneDimensionsBoundaryResult
object OneDimensionsBoundaryResult {
  implicit def jtsToResult(geom: jts.Geometry): OneDimensionsBoundaryResult =
    geom match {
      case g: jts.Geometry if g.isEmpty => NoResult  // do we need this case? A Line can't be empty. What about empty LineSet?
      case mp: jts.MultiPoint => PointSetResult(mp)
      case _ =>
        sys.error(s"Unexpected result for Line boundary: ${geom.getGeometryType}")
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
      case g: jts.Geometry if g.isEmpty => NoResult
      case mp: jts.MultiPoint => PointSetResult(mp)
      case _ =>
        sys.error(s"Unexpected result for Point-Point symDifference: ${geom.getGeometryType}")

    }
}

abstract sealed trait ZeroDimensionsPointSetSymDifferenceResult
object ZeroDimensionsPointSetSymDifferenceResult {
  implicit def jtsToResult(geom: jts.Geometry): ZeroDimensionsPointSetSymDifferenceResult =
    geom match {
      case g: jts.Geometry if g.isEmpty => NoResult
      case p: jts.Point => PointResult(p)
      case mp: jts.MultiPoint => PointSetResult(mp)
      case _ =>
        sys.error(s"Unexpected result for ZeroDimensions-PointSet symDifference: ${geom.getGeometryType}")
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
        sys.error(s"Unexpected result for ZeroDimensions-LineSet symDifference: ${geom.getGeometryType}")
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

abstract sealed trait ZeroDimensionsPolygonSetSymDifferenceResult
object ZeroDimensionsPolygonSetSymDifferenceResult {
  implicit def jtsToResult(geom: jts.Geometry): ZeroDimensionsPolygonSetSymDifferenceResult =
    geom match {
      case p: jts.Polygon => PolygonResult(p)
      case mp: jts.MultiPolygon => PolygonSetResult(mp)
      case gc: jts.GeometryCollection => GeometryCollectionResult(gc)
      case _ =>
        sys.error(s"Unexpected result for ZeroDimensions-PolygonSet symDifference: ${geom.getGeometryType}")
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
  with PointGeometryIntersectionResult
  with OneDimensionsAtLeastOneDimensionsIntersectionResult
  with PolygonPolygonIntersectionResult
  with PointSetGeometryIntersectionResult
  with PolygonSetIntersectionResult
  with OneDimensionsBoundaryResult
  with PointGeometryDifferenceResult
  with LineXDifferenceResult
  with PolygonPolygonDifferenceResult
  with PointSetDifferenceResult
  with PointPointSymDifferenceResult
  with OneDimensionsSymDifferenceResult
  with TwoDimensionsSymDifferenceResult
  with ZeroDimensionsPointSetSymDifferenceResult

case class PointResult(p: Point) extends Result
  with PointGeometryIntersectionResult
  with OneDimensionsAtLeastOneDimensionsIntersectionResult
  with PolygonPolygonIntersectionResult
  with PointSetGeometryIntersectionResult
  with PolygonSetIntersectionResult
  with PointZeroDimensionsUnionResult
  with PointGeometryDifferenceResult
  with PointSetDifferenceResult
  with ZeroDimensionsPointSetSymDifferenceResult

case class LineResult(l: Line) extends Result
  with OneDimensionsAtLeastOneDimensionsIntersectionResult
  with PolygonPolygonIntersectionResult
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
  with AtMostOneDimensionsPolygonUnionResult
  with PolygonPolygonUnionResult
  with PolygonSetIntersectionResult
  with AtMostOneDimensionsPolygonSetUnionResult
  with PolygonXDifferenceResult
  with PolygonPolygonDifferenceResult
  with ZeroDimensionsPolygonSymDifferenceResult
  with OneDimensionsPolygonSymDifferenceResult
  with TwoDimensionsSymDifferenceResult
  with ZeroDimensionsPolygonSetSymDifferenceResult
  with OneDimensionsPolygonSetSymDifferenceResult

case class PointSetResult(ps: Set[Point]) extends Result
  with PolygonPolygonIntersectionResult
  with PointSetGeometryIntersectionResult
  with PolygonSetIntersectionResult
  with PointZeroDimensionsUnionResult
  with OneDimensionsBoundaryResult
  with PointSetDifferenceResult
  with PointPointSymDifferenceResult
  with ZeroDimensionsPointSetSymDifferenceResult
  with OneDimensionsAtLeastOneDimensionsIntersectionResult

case class LineSetResult(ls: Set[Line]) extends Result
  with PolygonPolygonIntersectionResult
  with PolygonSetIntersectionResult
  with LineLineUnionResult
  with LinePointDifferenceResult
  with LineXDifferenceResult
  with PointLineSetUnionResult
  with LineSetPointDifferenceResult
  with PolygonBoundaryResult
  with OneDimensionsSymDifferenceResult
  with ZeroDimensionsLineSetSymDifferenceResult
  with OneDimensionsAtLeastOneDimensionsIntersectionResult

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
  with AtMostOneDimensionsPolygonSetUnionResult
  with PolygonPolygonDifferenceResult
  with PolygonSetXDifferenceResult
  with TwoDimensionsSymDifferenceResult
  with ZeroDimensionsPolygonSetSymDifferenceResult
  with OneDimensionsPolygonSetSymDifferenceResult

case class GeometryCollectionResult(gc: GeometryCollection) extends Result
  with PolygonPolygonIntersectionResult
  with PolygonSetIntersectionResult
  with PointLineUnionResult
  with AtMostOneDimensionsPolygonUnionResult
  with AtMostOneDimensionsPolygonSetUnionResult
  with PointLineSetUnionResult
  with ZeroDimensionsLineSymDifferenceResult
  with ZeroDimensionsPolygonSymDifferenceResult
  with OneDimensionsPolygonSymDifferenceResult
  with ZeroDimensionsLineSetSymDifferenceResult
  with ZeroDimensionsPolygonSetSymDifferenceResult
  with OneDimensionsPolygonSetSymDifferenceResult
  with OneDimensionsAtLeastOneDimensionsIntersectionResult
