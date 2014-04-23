/*
 * Copyright (c) 2014 Azavea.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.feature

import com.vividsolutions.jts.{geom => jts}

abstract sealed trait Result
object Result {
  implicit def jtsToResult(geom: jts.Geometry): Result =
    geom match {
      case null => NoResult
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

abstract sealed trait OneDimensionAtLeastOneDimensionIntersectionResult
object OneDimensionAtLeastOneDimensionIntersectionResult {
  implicit def jtsToResult(geom: jts.Geometry): OneDimensionAtLeastOneDimensionIntersectionResult =
    geom match {
      case g: jts.Geometry if g.isEmpty => NoResult
      case p: jts.Point => PointResult(p)
      case l: jts.LineString => LineResult(l)
      case mp: jts.MultiPoint => MultiPointResult(mp)
      case ml: jts.MultiLineString => MultiLineResult(ml)
      case gc: jts.GeometryCollection => GeometryCollectionResult(gc)
      case _ => 
        sys.error(s"Unexpected result for OneDimension-AtLeastOneDimension intersection: ${geom.getGeometryType}")
    }
}

abstract sealed trait TwoDimensionsTwoDimensionsIntersectionResult
object TwoDimensionsTwoDimensionsIntersectionResult {
  implicit def jtsToResult(geom: jts.Geometry): TwoDimensionsTwoDimensionsIntersectionResult =
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
        sys.error(s"Unexpected result for TwoDimensions-TwoDimensions intersection: ${geom.getGeometryType}")
    }
}

abstract sealed trait MultiPointMultiPointIntersectionResult
object MultiPointMultiPointIntersectionResult {
  implicit def jtsToResult(geom: jts.Geometry): MultiPointMultiPointIntersectionResult =
    geom match {
      case g: jts.Geometry if g.isEmpty => NoResult
      case p: jts.Point => PointResult(p)
      case mp: jts.MultiPoint => MultiPointResult(mp)
      case x =>
        sys.error(s"Unexpected result for MultiPoint-MultiPoint intersection: ${geom.getGeometryType}")
    }
}

abstract sealed trait MultiPointAtLeastOneDimensionIntersectionResult
object MultiPointAtLeastOneDimensionIntersectionResult {
  implicit def jtsToResult(geom: jts.Geometry): MultiPointAtLeastOneDimensionIntersectionResult =
    geom match {
      case g: jts.Geometry if g.isEmpty => NoResult
      case p: jts.Point => PointResult(p)
      case mp: jts.MultiPoint => MultiPointResult(mp)
      case x =>
        sys.error(s"Unexpected result for MultiPoint-AtLeastOneDimension intersection: ${geom.getGeometryType}")
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

abstract sealed trait MultiPointMultiPointUnionResult
object MultiPointMultiPointUnionResult {
  implicit def jtsToResult(geom: jts.Geometry): MultiPointMultiPointUnionResult =
    geom match {
      case g: jts.Geometry if g.isEmpty => NoResult
      case p: jts.Point => PointResult(p)
      case mp: jts.MultiPoint => MultiPointResult(mp)
      case _ =>
        sys.error(s"Unexpected result for MultiPoint-MultiPoint union: ${geom.getGeometryType}")
    }
}

abstract sealed trait ZeroDimensionsLineUnionResult
object ZeroDimensionsLineUnionResult {
  implicit def jtsToResult(geom: jts.Geometry): ZeroDimensionsLineUnionResult =
    geom match {
      case l: jts.LineString => LineResult(l)
      case gc: jts.GeometryCollection => GeometryCollectionResult(gc)
      case _ => 
        sys.error(s"Unexpected result for ZeroDimensions-Line union: ${geom.getGeometryType}")
    }
}

abstract sealed trait PointMultiLineUnionResult
object PointMultiLineUnionResult {
  implicit def jtsToResult(geom: jts.Geometry): PointMultiLineUnionResult =
    geom match {
      case p: jts.Point => PointResult(p)
      case l: jts.LineString => LineResult(l)
      case ml: jts.MultiLineString => MultiLineResult(ml)
      case gc: jts.GeometryCollection => GeometryCollectionResult(gc)
      case _ =>
        sys.error(s"Unexpected result for Point-MultiLine union: ${geom.getGeometryType}")
    }
}

abstract sealed trait MultiPointMultiLineUnionResult
object MultiPointMultiLineUnionResult {
  implicit def jtsToResult(geom: jts.Geometry): MultiPointMultiLineUnionResult =
    geom match {
      case g: jts.Geometry if g.isEmpty => NoResult
      case l: jts.LineString => LineResult(l)
      case mp: jts.MultiPoint => MultiPointResult(mp)
      case ml: jts.MultiLineString => MultiLineResult(ml)
      case gc: jts.GeometryCollection => GeometryCollectionResult(gc)
      case _ =>
        sys.error(s"Unexpected result for MultiPoint-MultiLine union: ${geom.getGeometryType}")
    }
}

abstract sealed trait LineOneDimensionUnionResult
object LineOneDimensionUnionResult {
  implicit def jtsToResult(geom: jts.Geometry): LineOneDimensionUnionResult =
    geom match {
      case l: jts.LineString => LineResult(l)
      case ml: jts.MultiLineString => MultiLineResult(ml)
      case _ => 
        sys.error(s"Unexpected result for Line-OneDimension union: ${geom.getGeometryType}")
    }
}

abstract sealed trait AtMostOneDimensionPolygonUnionResult
object AtMostOneDimensionPolygonUnionResult {
  implicit def jtsToResult(geom: jts.Geometry): AtMostOneDimensionPolygonUnionResult =
    geom match {
      case p: jts.Polygon => PolygonResult(Polygon(p))
      case gc: jts.GeometryCollection => GeometryCollectionResult(gc)
      case _ =>
        sys.error(s"Unexpected result for AtMostOneDimension-Polygon union: ${geom.getGeometryType}")
    }
}

abstract sealed trait TwoDimensionsTwoDimensionsUnionResult
object TwoDimensionsTwoDimensionsUnionResult {
  implicit def jtsToResult(geom: jts.Geometry): TwoDimensionsTwoDimensionsUnionResult =
    geom match {
      case p: jts.Polygon => PolygonResult(p)
      case mp: jts.MultiPolygon => MultiPolygonResult(mp)
      case _ =>
        sys.error(s"Unexpected result for TwoDimensions-TwoDimensions union: ${geom.getGeometryType}")
    }
}

abstract sealed trait PointMultiPolygonUnionResult
object PointMultiPolygonUnionResult {
  implicit def jtsToResult(geom: jts.Geometry): PointMultiPolygonUnionResult =
    geom match {
      case pt: jts.Point => PointResult(pt)
      case p: jts.Polygon => PolygonResult(p)
      case mp: jts.MultiPolygon => MultiPolygonResult(mp)
      case gc: jts.GeometryCollection => GeometryCollectionResult(gc)
      case _ =>
        sys.error(s"Unexpected result for Point-MultiPolygon union: ${geom.getGeometryType}")
    }
}

abstract sealed trait LineMultiPolygonUnionResult
object LineMultiPolygonUnionResult {
  implicit def jtsToResult(geom: jts.Geometry): LineMultiPolygonUnionResult =
    geom match {
      case l: jts.LineString => LineResult(l)
      case p: jts.Polygon => PolygonResult(p)
      case mp: jts.MultiPolygon => MultiPolygonResult(mp)
      case gc: jts.GeometryCollection => GeometryCollectionResult(gc)
      case _ =>
        sys.error(s"Unexpected result for Line-MultiPolygon union: ${geom.getGeometryType}")
    }
}

abstract sealed trait MultiPointMultiPolygonUnionResult
object MultiPointMultiPolygonUnionResult {
  implicit def jtsToResult(geom: jts.Geometry): MultiPointMultiPolygonUnionResult =
    geom match {
      case g: jts.Geometry if g.isEmpty => NoResult
      case p: jts.Polygon => PolygonResult(p)
      case mpt: jts.MultiPoint => MultiPointResult(mpt)
      case mp: jts.MultiPolygon => MultiPolygonResult(mp)
      case gc: jts.GeometryCollection => GeometryCollectionResult(gc)
      case _ =>
        sys.error(s"Unexpected result for MultiPoint-MultiPolygon union: ${geom.getGeometryType}")
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

abstract sealed trait LineAtLeastOneDimensionDifferenceResult
object LineAtLeastOneDimensionDifferenceResult {
  implicit def jtsToResult(geom: jts.Geometry): LineAtLeastOneDimensionDifferenceResult =
    geom match {
      case g: jts.Geometry if g.isEmpty => NoResult
      case l: jts.LineString => LineResult(l)
      case ml: jts.MultiLineString => MultiLineResult(ml)
      case _ =>
        sys.error(s"Unexpected result for Line-AtLeastOneDimension difference: ${geom.getGeometryType}")
    }
}

abstract sealed trait PolygonAtMostOneDimensionDifferenceResult
object PolygonAtMostOneDimensionDifferenceResult {
  implicit def jtsToResult(geom: jts.Geometry): PolygonAtMostOneDimensionDifferenceResult =
    geom match {
      case p: jts.Polygon => PolygonResult(p)
      case _ =>
        sys.error(s"Unexpected result for Polygon-AtMostOneDimension difference: ${geom.getGeometryType}")
    }
}

abstract sealed trait TwoDimensionsTwoDimensionsDifferenceResult
object TwoDimensionsTwoDimensionsDifferenceResult {
  implicit def jtsToResult(geom: jts.Geometry): TwoDimensionsTwoDimensionsDifferenceResult =
    geom match {
      case g: jts.Geometry if g.isEmpty => NoResult
      case p: jts.Polygon => PolygonResult(p)
      case mp: jts.MultiPolygon => MultiPolygonResult(mp)
      case _ =>
        sys.error(s"Unexpected result for TwoDimensions-TwoDimensions difference: ${geom.getGeometryType}")
    }
}

abstract sealed trait MultiPointGeometryDifferenceResult
object MultiPointGeometryDifferenceResult {
  implicit def jtsToResult(geom: jts.Geometry): MultiPointGeometryDifferenceResult =
    geom match {
      case g: jts.Geometry if g.isEmpty => NoResult
      case p: jts.Point => PointResult(p)
      case mp: jts.MultiPoint => MultiPointResult(mp)
      case _ =>
        sys.error(s"Unexpected result for MultiPoint-Geometry difference: ${geom.getGeometryType}")
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

abstract sealed trait OneDimensionBoundaryResult
object OneDimensionBoundaryResult {
  implicit def jtsToResult(geom: jts.Geometry): OneDimensionBoundaryResult =
    geom match {
      case g: jts.Geometry if g.isEmpty => NoResult  // do we need this case? A Line can't be empty. What about empty LineSet?
      case mp: jts.MultiPoint => MultiPointResult(mp)
      case _ =>
        sys.error(s"Unexpected result for Line boundary: ${geom.getGeometryType}")
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

abstract sealed trait PointMultiLineSymDifferenceResult
object PointMultiLineSymDifferenceResult {
  implicit def jtsToResult(geom: jts.Geometry): PointMultiLineSymDifferenceResult =
    geom match {
      case p: jts.Point => PointResult(p)
      case l: jts.LineString => LineResult(l)
      case ml: jts.MultiLineString => MultiLineResult(ml)
      case gc: jts.GeometryCollection => GeometryCollectionResult(gc)
      case _ =>
        sys.error(s"Unexpected result for Point-MultiLine symDifference: ${geom.getGeometryType}")
    }
}

abstract sealed trait MultiPointMultiLineSymDifferenceResult
object MultiPointMultiLineSymDifferenceResult {
  implicit def jtsToResult(geom: jts.Geometry): MultiPointMultiLineSymDifferenceResult =
    geom match {
      case g: jts.Geometry if g.isEmpty => NoResult
      case l: jts.LineString => LineResult(l)
      case mp: jts.MultiPoint => MultiPointResult(mp)
      case ml: jts.MultiLineString => MultiLineResult(ml)
      case gc: jts.GeometryCollection => GeometryCollectionResult(gc)
      case _ =>
        sys.error(s"Unexpected result for MultiPoint-MultiLine symDifference: ${geom.getGeometryType}")
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

abstract sealed trait PointMultiPolygonSymDifferenceResult
object PointMultiPolygonSymDifferenceResult {
  implicit def jtsToResult(geom: jts.Geometry): PointMultiPolygonSymDifferenceResult =
    geom match {
      case pt: jts.Point => PointResult(pt)
      case p: jts.Polygon => PolygonResult(p)
      case mp: jts.MultiPolygon => MultiPolygonResult(mp)
      case gc: jts.GeometryCollection => GeometryCollectionResult(gc)
      case _ =>
        sys.error(s"Unexpected result for Point-MultiPolygon symDifference: ${geom.getGeometryType}")
    }
}

abstract sealed trait MultiPointMultiPolygonSymDifferenceResult
object MultiPointMultiPolygonSymDifferenceResult {
  implicit def jtsToResult(geom: jts.Geometry): MultiPointMultiPolygonSymDifferenceResult =
    geom match {
      case g: jts.Geometry if g.isEmpty => NoResult
      case p: jts.Polygon => PolygonResult(p)
      case mpt: jts.MultiPoint => MultiPointResult(mpt)
      case mp: jts.MultiPolygon => MultiPolygonResult(mp)
      case gc: jts.GeometryCollection => GeometryCollectionResult(gc)
      case _ =>
        sys.error(s"Unexpected result for MultiPoint-MultiPolygon symDifference: ${geom.getGeometryType}")
    }
}

abstract sealed trait OneDimensionOneDimensionSymDifferenceResult
object OneDimensionOneDimensionSymDifferenceResult {
  implicit def jtsToResult(geom: jts.Geometry): OneDimensionOneDimensionSymDifferenceResult =
    geom match {
      case g: jts.Geometry if g.isEmpty => NoResult
      case l: jts.LineString => LineResult(l)
      case ml: jts.MultiLineString => MultiLineResult(ml)
      case _ =>
        sys.error(s"Unexpected result for OneDimension-OneDimension symDifference: ${geom.getGeometryType}")
    }
}

abstract sealed trait OneDimensionPolygonSymDifferenceResult
object OneDimensionPolygonSymDifferenceResult {
  implicit def jtsToResult(geom: jts.Geometry): OneDimensionPolygonSymDifferenceResult =
    geom match {
      case p: jts.Polygon => PolygonResult(p)
      case gc: jts.GeometryCollection => GeometryCollectionResult(gc)
      case _ =>
        sys.error(s"Unexpected result for Line-Polygon symDifference: ${geom.getGeometryType}")
    }
}

abstract sealed trait OneDimensionMultiPolygonSymDifferenceResult
object OneDimensionMultiPolygonSymDifferenceResult {
  implicit def jtsToResult(geom: jts.Geometry): OneDimensionMultiPolygonSymDifferenceResult =
    geom match {
      case l: jts.LineString => LineResult(l)
      case p: jts.Polygon => PolygonResult(p)
      case mp: jts.MultiPolygon => MultiPolygonResult(mp)
      case gc: jts.GeometryCollection => GeometryCollectionResult(gc)
      case _ =>
        sys.error(s"Unexpected result for Line-Polygon symDifference: ${geom.getGeometryType}")
    }
}

abstract sealed trait TwoDimensionsTwoDimensionsSymDifferenceResult
object TwoDimensionsTwoDimensionsSymDifferenceResult {
  implicit def jtsToResult(geom: jts.Geometry): TwoDimensionsTwoDimensionsSymDifferenceResult =
    geom match {
      case g: jts.Geometry if g.isEmpty => NoResult
      case p: jts.Polygon => PolygonResult(p)
      case mp: jts.MultiPolygon => MultiPolygonResult(mp)
      case _ =>
        sys.error(s"Unexpected result for TwoDimensions-TwoDimensions symDifference: ${geom.getGeometryType}")
    }
}

// -- Misc.
abstract sealed trait PointOrNoResult
object PointOrNoResult {
  implicit def jtsToResult(geom: jts.Geometry): PointOrNoResult =
    geom match {
      case g: jts.Geometry if g.isEmpty => NoResult
      case p: jts.Point => PointResult(p)
      case _ =>
        sys.error(s"Unexpected result: ${geom.getGeometryType}")
    }
}


case object NoResult extends Result
  with PointGeometryIntersectionResult
  with OneDimensionAtLeastOneDimensionIntersectionResult
  with TwoDimensionsTwoDimensionsIntersectionResult
  with MultiPointAtLeastOneDimensionIntersectionResult
  with OneDimensionBoundaryResult
  with PointGeometryDifferenceResult
  with LineAtLeastOneDimensionDifferenceResult
  with TwoDimensionsTwoDimensionsDifferenceResult
  with MultiPointGeometryDifferenceResult
  with PointPointSymDifferenceResult
  with OneDimensionOneDimensionSymDifferenceResult
  with TwoDimensionsTwoDimensionsSymDifferenceResult
  with ZeroDimensionsMultiPointSymDifferenceResult
  with MultiPointMultiPointIntersectionResult
  with MultiPointMultiPointUnionResult
  with MultiPointMultiLineUnionResult
  with MultiPointMultiPolygonUnionResult
  with PointOrNoResult
  with MultiPointMultiLineSymDifferenceResult
  with MultiPointMultiPolygonSymDifferenceResult

case class PointResult(p: Point) extends Result
  with PointGeometryIntersectionResult
  with OneDimensionAtLeastOneDimensionIntersectionResult
  with TwoDimensionsTwoDimensionsIntersectionResult
  with MultiPointAtLeastOneDimensionIntersectionResult
  with PointZeroDimensionsUnionResult
  with PointGeometryDifferenceResult
  with MultiPointGeometryDifferenceResult
  with ZeroDimensionsMultiPointSymDifferenceResult
  with MultiPointMultiPointIntersectionResult
  with LineMultiPolygonUnionResult
  with PointMultiLineUnionResult
  with PointMultiPolygonUnionResult
  with PointMultiLineSymDifferenceResult
  with PointMultiPolygonSymDifferenceResult
  with MultiPointMultiPointUnionResult
  with PointOrNoResult

case class LineResult(l: Line) extends Result
  with OneDimensionAtLeastOneDimensionIntersectionResult
  with TwoDimensionsTwoDimensionsIntersectionResult
  with ZeroDimensionsLineUnionResult
  with LineOneDimensionUnionResult
  with LineAtLeastOneDimensionDifferenceResult
  with PointMultiLineUnionResult
  with PolygonBoundaryResult
  with ZeroDimensionsLineSymDifferenceResult
  with OneDimensionOneDimensionSymDifferenceResult
  with PointMultiLineSymDifferenceResult
  with LineMultiPolygonUnionResult
  with OneDimensionMultiPolygonSymDifferenceResult
  with MultiPointMultiLineUnionResult
  with MultiPointMultiLineSymDifferenceResult

object LineResult {
  implicit def jtsToResult(geom: jts.Geometry): LineResult =
    geom match {
      case ml: jts.LineString => LineResult(ml)
      case _ =>
        sys.error(s"Unexpected result: ${geom.getGeometryType}")
    }
}

case class PolygonResult(p: Polygon) extends Result
  with TwoDimensionsTwoDimensionsIntersectionResult
  with AtMostOneDimensionPolygonUnionResult
  with TwoDimensionsTwoDimensionsUnionResult
  with LineMultiPolygonUnionResult
  with PolygonAtMostOneDimensionDifferenceResult
  with TwoDimensionsTwoDimensionsDifferenceResult
  with ZeroDimensionsPolygonSymDifferenceResult
  with OneDimensionPolygonSymDifferenceResult
  with TwoDimensionsTwoDimensionsSymDifferenceResult
  with PointMultiPolygonSymDifferenceResult
  with OneDimensionMultiPolygonSymDifferenceResult
  with PointMultiPolygonUnionResult
  with MultiPointMultiPolygonUnionResult
  with MultiPointMultiPolygonSymDifferenceResult

case class MultiPointResult(ps: Set[Point]) extends Result
  with TwoDimensionsTwoDimensionsIntersectionResult
  with MultiPointAtLeastOneDimensionIntersectionResult
  with PointZeroDimensionsUnionResult
  with OneDimensionBoundaryResult
  with MultiPointGeometryDifferenceResult
  with PointPointSymDifferenceResult
  with ZeroDimensionsMultiPointSymDifferenceResult
  with OneDimensionAtLeastOneDimensionIntersectionResult
  with MultiPointMultiPointIntersectionResult
  with MultiPointMultiPointUnionResult
  with MultiPointMultiLineUnionResult
  with MultiPointMultiPolygonUnionResult
  with MultiPointMultiLineSymDifferenceResult
  with MultiPointMultiPolygonSymDifferenceResult

case class MultiLineResult(ls: Set[Line]) extends Result
  with TwoDimensionsTwoDimensionsIntersectionResult
  with LineOneDimensionUnionResult
  with LineAtLeastOneDimensionDifferenceResult
  with PointMultiLineUnionResult
  with MultiLinePointDifferenceResult
  with PolygonBoundaryResult
  with OneDimensionOneDimensionSymDifferenceResult
  with PointMultiLineSymDifferenceResult
  with OneDimensionAtLeastOneDimensionIntersectionResult
  with MultiPointMultiLineUnionResult
  with MultiPointMultiLineSymDifferenceResult

object MultiLineResult {
  implicit def jtsToResult(geom: jts.Geometry): MultiLineResult =
    geom match {
      case ml: jts.MultiLineString => MultiLineResult(ml)
      case _ =>
        sys.error(s"Unexpected result: ${geom.getGeometryType}")
    }
}

case class MultiPolygonResult(ps: Set[Polygon]) extends Result
  with TwoDimensionsTwoDimensionsIntersectionResult
  with TwoDimensionsTwoDimensionsUnionResult
  with LineMultiPolygonUnionResult
  with TwoDimensionsTwoDimensionsDifferenceResult
  with MultiPolygonXDifferenceResult
  with TwoDimensionsTwoDimensionsSymDifferenceResult
  with PointMultiPolygonSymDifferenceResult
  with OneDimensionMultiPolygonSymDifferenceResult
  with PointMultiPolygonUnionResult
  with MultiPointMultiPolygonUnionResult
  with MultiPointMultiPolygonSymDifferenceResult

case class GeometryCollectionResult(gc: GeometryCollection) extends Result
  with TwoDimensionsTwoDimensionsIntersectionResult
  with ZeroDimensionsLineUnionResult
  with AtMostOneDimensionPolygonUnionResult
  with LineMultiPolygonUnionResult
  with PointMultiLineUnionResult
  with ZeroDimensionsLineSymDifferenceResult
  with ZeroDimensionsPolygonSymDifferenceResult
  with OneDimensionPolygonSymDifferenceResult
  with PointMultiLineSymDifferenceResult
  with PointMultiPolygonSymDifferenceResult
  with OneDimensionMultiPolygonSymDifferenceResult
  with OneDimensionAtLeastOneDimensionIntersectionResult
  with PointMultiPolygonUnionResult
  with MultiPointMultiLineUnionResult
  with MultiPointMultiPolygonUnionResult
  with MultiPointMultiLineSymDifferenceResult
  with MultiPointMultiPolygonSymDifferenceResult