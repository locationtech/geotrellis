package geotrellis.feature

import com.vividsolutions.jts.{geom=>jts}

abstract sealed trait Result
object Result {
  implicit def jtsToResult(geom:jts.Geometry):Result =
    geom match {
      case p:jts.Point => PointResult(p)
      case l:jts.LineString => LineResult(l)
      case p:jts.Polygon => PolygonResult(p)
      case ps:jts.MultiPoint => PointSetResult(ps)
      case ps:jts.MultiPolygon => PolygonSetResult(ps)
      case ls:jts.MultiLineString => LineSetResult(ls)
      case gc:jts.GeometryCollection => GeometryCollectionResult(gc)
      case _ => NoResult
    }
}

abstract sealed trait PointIntersectionResult
object PointIntersectionResult {
  implicit def jtsToResult(geom:jts.Geometry):PointIntersectionResult =
    geom match {
      case p:jts.Point => PointResult(p)
      case _ => NoResult
    }
}

abstract sealed trait LineLineIntersectionResult
object LineLineIntersectionResult {
  implicit def jtsToResult(geom:jts.Geometry):LineLineIntersectionResult =
    geom match {
      case p:jts.Point => PointResult(p)
      case l:jts.LineString => LineResult(l)
      case _ => NoResult
    }
}

abstract sealed trait PolygonLineIntersectionResult
object PolygonLineIntersectionResult {
  implicit def jtsToResult(geom:jts.Geometry):PolygonLineIntersectionResult =
    geom match {
      case p:jts.Point => PointResult(p)
      case l:jts.LineString => LineResult(l)
      case l:jts.MultiLineString => LineSetResult(l)
      case _ => NoResult
    }
}

abstract sealed trait PolygonPolygonIntersectionResult
object PolygonPolygonIntersectionResult {
  implicit def jtsToResult(geom:jts.Geometry):PolygonPolygonIntersectionResult =
    geom match {
      case p:jts.Point => PointResult(p)
      case l:jts.LineString => LineResult(l)
      case p:jts.Polygon => PolygonResult(p)
      case ps:jts.MultiPoint => PointSetResult(ps)
      case ps:jts.MultiPolygon => PolygonSetResult(ps)
      case ls:jts.MultiLineString => LineSetResult(ls)
      case gc:jts.GeometryCollection => GeometryCollectionResult(gc)
      case _ => NoResult
    }
}

abstract sealed trait PointSetIntersectionResult
object PointSetIntersectionResult {
  implicit def jtsToResult(geom:jts.Geometry):PointSetIntersectionResult =
    geom match {
      case p:jts.Point => if(p.isEmpty) NoResult else PointResult(p)
      case mp:jts.MultiPoint => PointSetResult(mp)
      case x => 
        sys.error(s"Unexpected result for PointSet intersection: ${geom.getGeometryType}")
    }
}

abstract sealed trait LineSetIntersectionResult
object LineSetIntersectionResult {
  implicit def jtsToResult(geom:jts.Geometry): LineSetIntersectionResult =
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
  implicit def jtsToResult(geom:jts.Geometry): PolygonSetIntersectionResult =
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

// Union

abstract sealed trait PointPointUnionResult
object PointPointUnionResult {
  implicit def jtsToResult(geom: jts.Geometry): PointPointUnionResult =
    geom match {
      case l:jts.Point => PointResult(l)
      case gc:jts.MultiPoint => PointSetResult(gc)
      case _ => 
        sys.error(s"Unexpected result for Point Point union: ${geom.getGeometryType}")
    }
}

abstract sealed trait LinePointUnionResult
object LinePointUnionResult {
  implicit def jtsToResult(geom: jts.Geometry): LinePointUnionResult =
    geom match {
      case l:jts.LineString => LineResult(l)
      case gc:jts.GeometryCollection => GeometryCollectionResult(gc)
      case _ => 
        sys.error(s"Unexpected result for Line Point union: ${geom.getGeometryType}")
    }
}

abstract sealed trait LineLineUnionResult
object LineLineUnionResult {
  implicit def jtsToResult(geom: jts.Geometry): LineLineUnionResult =
    geom match {
      case l:jts.LineString => LineResult(l)
      case ml:jts.MultiLineString => LineSetResult(ml)
      case _ => 
        sys.error(s"Unexpected result for Line Line union: ${geom.getGeometryType}")
    }
}

abstract sealed trait PolygonXUnionResult
object PolygonXUnionResult {
  implicit def jtsToResult(geom:jts.Geometry): PolygonXUnionResult =
    geom match {
      case gc:jts.GeometryCollection => GeometryCollectionResult(gc)
      case p:jts.Polygon => PolygonResult(Polygon(p))
      case _ => 
        sys.error(s"Unexpected result for Polygon union: ${geom.getGeometryType}")
    }
}

abstract sealed trait PolygonPolygonUnionResult
object PolygonPolygonUnionResult {
  implicit def jtsToResult(geom:jts.Geometry): PolygonPolygonUnionResult =
    geom match {
      case p:jts.Polygon => PolygonResult(p)
      case mp:jts.MultiPolygon => PolygonSetResult(mp)
      case _ =>
        sys.error(s"Unexpected result for Polygon-Polygon union: ${geom.getGeometryType}")
    }
}

abstract sealed trait PolygonSetUnionResult
object PolygonSetUnionResult {
  implicit def jtsToResult(geom:jts.Geometry): PolygonSetUnionResult =
    geom match {
      case p:jts.Polygon => PolygonResult(p)
      case mp:jts.MultiPolygon => PolygonSetResult(mp)
      case gc:jts.GeometryCollection => GeometryCollectionResult(gc)
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
      case ml: jts.MultiLineString => LineSetResult(ml)
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

// -- Boundary

abstract sealed trait LineBoundaryResult
object LineBoundaryResult {
  implicit def jtsToResult(geom:jts.Geometry): LineBoundaryResult =
    geom match {
      case p:jts.Point => PointResult(p)
      case mp:jts.MultiPoint => PointSetResult(mp)
      case _ => NoResult
    }
}

case object NoResult 
    extends Result
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

case class PointResult(p:Point) 
    extends Result
       with PointIntersectionResult
       with LineLineIntersectionResult
       with PolygonLineIntersectionResult
       with PolygonPolygonIntersectionResult
       with PointSetIntersectionResult
       with LineSetIntersectionResult
       with PolygonSetIntersectionResult
       with PointPointUnionResult
       with LineBoundaryResult
       with PointDifferenceResult

case class LineResult(l:Line) 
    extends Result
      with LineLineIntersectionResult
      with PolygonLineIntersectionResult
      with PolygonPolygonIntersectionResult
      with LineSetIntersectionResult
      with PolygonSetIntersectionResult
      with LinePointUnionResult
      with LineLineUnionResult
      with LinePointDifferenceResult
      with LineXDifferenceResult

case class PolygonResult(p:Polygon) 
    extends Result
       with PolygonPolygonIntersectionResult
       with PolygonXUnionResult
       with PolygonPolygonUnionResult
       with PolygonSetIntersectionResult
       with PolygonSetUnionResult

case class PointSetResult(ls:Set[Point]) 
    extends Result
       with PolygonPolygonIntersectionResult
       with PointSetIntersectionResult
       with LineSetIntersectionResult
       with PolygonSetIntersectionResult
       with PointPointUnionResult
       with LineBoundaryResult

case class LineSetResult(ls:Set[Line]) 
    extends Result
      with PolygonLineIntersectionResult
      with PolygonPolygonIntersectionResult
      with LineSetIntersectionResult
      with PolygonSetIntersectionResult
      with LineLineUnionResult
      with LinePointDifferenceResult
      with LineXDifferenceResult

case class PolygonSetResult(ps:Set[Polygon]) 
    extends Result
       with PolygonPolygonIntersectionResult
       with PolygonPolygonUnionResult
       with PolygonSetIntersectionResult
       with PolygonSetUnionResult

case class GeometryCollectionResult(gc:GeometryCollection) 
    extends Result
       with PolygonPolygonIntersectionResult
       with LineSetIntersectionResult
       with PolygonSetIntersectionResult
       with LinePointUnionResult
       with PolygonXUnionResult
       with PolygonSetUnionResult
