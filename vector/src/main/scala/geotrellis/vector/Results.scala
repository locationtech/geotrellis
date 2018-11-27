/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.vector

import org.locationtech.jts.{geom => jts}
import scala.reflect._

private[vector] trait GeometryResultMethods extends Serializable {
  /** Returns this result as an option, unless this is NoResult, in which case it returns None */
  def toGeometry(): Option[Geometry]

  /**
    * A typesafe method for casting to a particular geometry type.  If
    * this fails to cast or is [[NoResult]], will result in None.
    */
  def as[G <: Geometry : ClassTag]: Option[G] =
    toGeometry.flatMap { g =>
      if(classTag[G].runtimeClass.isInstance(g)) Some(g.asInstanceOf[G])
      else None
    }

  /** returns this result as a MultiPoint if it's a Point or MultiPoint, otherwise returns None */
  def asMultiPoint: Option[MultiPoint] =
    toGeometry.flatMap { g =>
      g match {
        case p: Point => Some(MultiPoint(p))
        case mp: MultiPoint => Some(mp)
        case _ => None
      }
    }

  /** returns this result as a MultiLine if it's a Line or MultiLine, otherwise returns None */
  def asMultiLine: Option[MultiLine] =
    toGeometry.flatMap { g =>
      g match {
        case l: Line => Some(MultiLine(l))
        case ml: MultiLine => Some(ml)
        case _ => None
      }
    }

  /** returns this result as a MultiPolygon if it's a Polygon or MultiPolygon, otherwise returns None */
  def asMultiPolygon: Option[MultiPolygon] =
    toGeometry.flatMap { g =>
      g match {
        case p: Polygon => Some(MultiPolygon(p))
        case mp: MultiPolygon => Some(mp)
        case _ => None
      }
    }

  /** returns this result as a MultiPoint if it's a Point or MultiPoint, otherwise returns None */
  def asGeometryCollection: GeometryCollection =
    toGeometry match {
      case Some(g) => GeometryCollection(Seq(g))
      case None => GeometryCollection()
    }
}

abstract sealed trait GeometryResult extends GeometryResultMethods
object GeometryResult {
  implicit def resultToGeometry(result: GeometryResult): Option[Geometry] =
    result.toGeometry

  implicit def jtsToResult(geom: jts.Geometry): GeometryResult =
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

abstract sealed trait OneDimensionAtLeastOneDimensionIntersectionResult extends GeometryResultMethods
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

abstract sealed trait TwoDimensionsTwoDimensionsIntersectionResult extends GeometryResultMethods
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

abstract sealed trait MultiLineMultiLineIntersectionResult extends GeometryResultMethods
object MultiLineMultiLineIntersectionResult {
  implicit def jtsToResult(geom: jts.Geometry): MultiLineMultiLineIntersectionResult =
    geom match {
      case g: jts.Geometry if g.isEmpty => NoResult
      case p: jts.Point => PointResult(p)
      case l: jts.LineString => LineResult(l)
      case mp: jts.MultiPoint => MultiPointResult(mp)
      case ml: jts.MultiLineString => MultiLineResult(ml)
      case _ =>
        sys.error(s"Unexpected result for MultiLine-MultiLine intersection: ${geom.getGeometryType}")
    }
}

abstract sealed trait MultiPointMultiPointIntersectionResult extends GeometryResultMethods
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

abstract sealed trait MultiPolygonMultiPolygonIntersectionResult extends GeometryResultMethods
object MultiPolygonMultiPolygonIntersectionResult {
  implicit def jtsToResult(geom: jts.Geometry): MultiPolygonMultiPolygonIntersectionResult =
    geom match {
      case g: jts.Geometry if g.isEmpty => NoResult
      case p: jts.Point => PointResult(p)
      case l: jts.LineString => LineResult(l)
      case p: jts.Polygon => PolygonResult(p)
      case mp: jts.MultiPoint => MultiPointResult(mp)
      case ml: jts.MultiLineString => MultiLineResult(ml)
      case mp: jts.MultiPolygon => MultiPolygonResult(mp)
      case _ =>
        sys.error(s"Unexpected result for MultiPolygon-MultiPolygon intersection: ${geom.getGeometryType}")
    }
}

abstract sealed trait MultiPointAtLeastOneDimensionIntersectionResult extends GeometryResultMethods
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

abstract sealed trait PointZeroDimensionsUnionResult extends GeometryResultMethods
object PointZeroDimensionsUnionResult {
  implicit def jtsToResult(geom: jts.Geometry): PointZeroDimensionsUnionResult =
    geom match {
      case p: jts.Point => PointResult(p)
      case mp: jts.MultiPoint => MultiPointResult(mp)
      case _ =>
        sys.error(s"Unexpected result for Point-ZeroDimensions union: ${geom.getGeometryType}")
    }
}

abstract sealed trait MultiPointMultiPointUnionResult extends GeometryResultMethods
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

abstract sealed trait ZeroDimensionsLineUnionResult extends GeometryResultMethods
object ZeroDimensionsLineUnionResult {
  implicit def jtsToResult(geom: jts.Geometry): ZeroDimensionsLineUnionResult =
    geom match {
      case l: jts.LineString => LineResult(l)
      case gc: jts.GeometryCollection => GeometryCollectionResult(gc)
      case _ =>
        sys.error(s"Unexpected result for ZeroDimensions-Line union: ${geom.getGeometryType}")
    }
}

abstract sealed trait PointMultiLineUnionResult extends GeometryResultMethods
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

abstract sealed trait MultiPointMultiLineUnionResult extends GeometryResultMethods
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

abstract sealed trait LineOneDimensionUnionResult extends GeometryResultMethods
object LineOneDimensionUnionResult {
  implicit def jtsToResult(geom: jts.Geometry): LineOneDimensionUnionResult =
    geom match {
      case l: jts.LineString => LineResult(l)
      case ml: jts.MultiLineString => MultiLineResult(ml)
      case _ =>
        sys.error(s"Unexpected result for Line-OneDimension union: ${geom.getGeometryType}")
    }
}

abstract sealed trait MultiLineMultiLineUnionResult extends GeometryResultMethods
object MultiLineMultiLineUnionResult {
  implicit def jtsToResult(geom: jts.Geometry): MultiLineMultiLineUnionResult =
    geom match {
      case g: jts.Geometry if g.isEmpty => NoResult
      case l: jts.LineString => LineResult(l)
      case ml: jts.MultiLineString => MultiLineResult(ml)
      case _ =>
        sys.error(s"Unexpected result for MultiLine-MultiLine union: ${geom.getGeometryType}")
    }
}

abstract sealed trait AtMostOneDimensionPolygonUnionResult extends GeometryResultMethods
object AtMostOneDimensionPolygonUnionResult {
  implicit def jtsToResult(geom: jts.Geometry): AtMostOneDimensionPolygonUnionResult =
    geom match {
      case p: jts.Polygon => PolygonResult(Polygon(p))
      case gc: jts.GeometryCollection => GeometryCollectionResult(gc)
      case _ =>
        sys.error(s"Unexpected result for AtMostOneDimension-Polygon union: ${geom.getGeometryType}")
    }
}

abstract sealed trait TwoDimensionsTwoDimensionsUnionResult extends GeometryResultMethods
object TwoDimensionsTwoDimensionsUnionResult {
  implicit def jtsToResult(geom: jts.Geometry): TwoDimensionsTwoDimensionsUnionResult =
    geom match {
      case p: jts.Polygon => PolygonResult(p)
      case mp: jts.MultiPolygon => MultiPolygonResult(mp)
      case _ =>
        sys.error(s"Unexpected result for TwoDimensions-TwoDimensions union: ${geom.getGeometryType}")
    }
}

abstract sealed trait TwoDimensionsTwoDimensionsSeqUnionResult extends GeometryResultMethods
object TwoDimensionsTwoDimensionsSeqUnionResult {
  implicit def jtsToResult(geom: jts.Geometry): TwoDimensionsTwoDimensionsSeqUnionResult =
    geom match {
      case g: jts.Geometry if g.isEmpty => NoResult
      case p: jts.Polygon => PolygonResult(p)
      case mp: jts.MultiPolygon => MultiPolygonResult(mp)
      case _ =>
        sys.error(s"Unexpected result for TwoDimensions-TwoDimensions union: ${geom.getGeometryType}")
    }
}

abstract sealed trait PointMultiPolygonUnionResult extends GeometryResultMethods
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

abstract sealed trait LineMultiPolygonUnionResult extends GeometryResultMethods
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

abstract sealed trait MultiPointMultiPolygonUnionResult extends GeometryResultMethods
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

abstract sealed trait MultiLineMultiPolygonUnionResult extends GeometryResultMethods
object MultiLineMultiPolygonUnionResult {
  implicit def jtsToResult(geom: jts.Geometry): MultiLineMultiPolygonUnionResult =
    geom match {
      case g: jts.Geometry if g.isEmpty => NoResult
      case p: jts.Polygon => PolygonResult(p)
      case ml: jts.MultiLineString => MultiLineResult(ml)
      case mp: jts.MultiPolygon => MultiPolygonResult(mp)
      case gc: jts.GeometryCollection => GeometryCollectionResult(gc)
      case _ =>
        sys.error(s"Unexpected result for MultiPoint-MultiPolygon union: ${geom.getGeometryType}")
    }
}

// -- Difference

abstract sealed trait PointGeometryDifferenceResult extends GeometryResultMethods
object PointGeometryDifferenceResult {
  implicit def jtsToResult(geom: jts.Geometry): PointGeometryDifferenceResult =
    geom match {
      case g: jts.Geometry if g.isEmpty => NoResult
      case p: jts.Point => PointResult(p)
      case _ =>
        sys.error(s"Unexpected result for Point-Geometry difference: ${geom.getGeometryType}")
    }
}

abstract sealed trait LineAtLeastOneDimensionDifferenceResult extends GeometryResultMethods
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

abstract sealed trait PolygonAtMostOneDimensionDifferenceResult extends GeometryResultMethods
object PolygonAtMostOneDimensionDifferenceResult {
  implicit def jtsToResult(geom: jts.Geometry): PolygonAtMostOneDimensionDifferenceResult =
    geom match {
      case p: jts.Polygon => PolygonResult(p)
      case _ =>
        sys.error(s"Unexpected result for Polygon-AtMostOneDimension difference: ${geom.getGeometryType}")
    }
}

abstract sealed trait TwoDimensionsTwoDimensionsDifferenceResult extends GeometryResultMethods
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

abstract sealed trait MultiPointGeometryDifferenceResult extends GeometryResultMethods
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

abstract sealed trait MultiLineGeometryDifferenceResult extends GeometryResultMethods
object MultiLineGeometryDifferenceResult {
  implicit def jtsToResult(geom: jts.Geometry): MultiLineGeometryDifferenceResult =
    geom match {
      case g: jts.Geometry if g.isEmpty => NoResult
      case l: jts.LineString => LineResult(l)
      case ml: jts.MultiLineString => MultiLineResult(ml)
      case _ =>
        sys.error(s"Unexpected result for MultiLine-Geometry difference: ${geom.getGeometryType}")
    }
}

abstract sealed trait MultiPolygonXDifferenceResult extends GeometryResultMethods
object MultiPolygonXDifferenceResult {
  implicit def jtsToResult(geom: jts.Geometry): MultiPolygonXDifferenceResult =
    geom match {
      case mp: jts.MultiPolygon => MultiPolygonResult(mp)
      case _ =>
        sys.error(s"Unexpected result for Polygon difference: ${geom.getGeometryType}")
    }
}


abstract sealed trait MultiLineMultiLineDifferenceResult extends GeometryResultMethods
object MultiLineMultiLineDifferenceResult {
  implicit def jtsToResult(geom: jts.Geometry): MultiLineMultiLineDifferenceResult =
    geom match {
      case g: jts.Geometry if g.isEmpty => NoResult
      case p: jts.Point => PointResult(p)
      case l: jts.LineString => LineResult(l)
      case mp: jts.MultiPoint => MultiPointResult(mp)
      case ml: jts.MultiLineString => MultiLineResult(ml)
      case _ =>
        sys.error(s"Unexpected result for MultiLine-MultiLine difference: ${geom.getGeometryType}")
    }
}

abstract sealed trait MultiPointMultiPointDifferenceResult extends GeometryResultMethods
object MultiPointMultiPointDifferenceResult {
  implicit def jtsToResult(geom: jts.Geometry): MultiPointMultiPointDifferenceResult =
    geom match {
      case g: jts.Geometry if g.isEmpty => NoResult
      case p: jts.Point => PointResult(p)
      case mp: jts.MultiPoint => MultiPointResult(mp)
      case x =>
        sys.error(s"Unexpected result for MultiPoint-MultiPoint difference: ${geom.getGeometryType}")
    }
}

abstract sealed trait MultiPolygonMultiPolygonDifferenceResult extends GeometryResultMethods
object MultiPolygonMultiPolygonDifferenceResult {
  implicit def jtsToResult(geom: jts.Geometry): MultiPolygonMultiPolygonDifferenceResult =
    geom match {
      case g: jts.Geometry if g.isEmpty => NoResult
      case p: jts.Point => PointResult(p)
      case l: jts.LineString => LineResult(l)
      case p: jts.Polygon => PolygonResult(p)
      case mp: jts.MultiPoint => MultiPointResult(mp)
      case ml: jts.MultiLineString => MultiLineResult(ml)
      case mp: jts.MultiPolygon => MultiPolygonResult(mp)
      case _ =>
        sys.error(s"Unexpected result for MultiPolygon-MultiPolygon difference: ${geom.getGeometryType}")
    }
}
// -- Boundary

abstract sealed trait OneDimensionBoundaryResult extends GeometryResultMethods
object OneDimensionBoundaryResult {
  implicit def jtsToResult(geom: jts.Geometry): OneDimensionBoundaryResult =
    geom match {
      case g: jts.Geometry if g.isEmpty => NoResult
      case mp: jts.MultiPoint => MultiPointResult(mp)
      case _ =>
        sys.error(s"Unexpected result for Line boundary: ${geom.getGeometryType}")
    }
}

abstract sealed trait PolygonBoundaryResult extends GeometryResultMethods
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

abstract sealed trait PointPointSymDifferenceResult extends GeometryResultMethods
object PointPointSymDifferenceResult {
  implicit def jtsToResult(geom: jts.Geometry): PointPointSymDifferenceResult =
    geom match {
      case g: jts.Geometry if g.isEmpty => NoResult
      case mp: jts.MultiPoint => MultiPointResult(mp)
      case _ =>
        sys.error(s"Unexpected result for Point-Point symDifference: ${geom.getGeometryType}")

    }
}

abstract sealed trait ZeroDimensionsMultiPointSymDifferenceResult extends GeometryResultMethods
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

abstract sealed trait ZeroDimensionsLineSymDifferenceResult extends GeometryResultMethods
object ZeroDimensionsLineSymDifferenceResult {
  implicit def jtsToResult(geom: jts.Geometry): ZeroDimensionsLineSymDifferenceResult =
    geom match {
      case l: jts.LineString => LineResult(l)
      case gc: jts.GeometryCollection => GeometryCollectionResult(gc)
      case _ =>
        sys.error(s"Unexpected result for ZeroDimensions-Line symDifference: ${geom.getGeometryType}")
    }
}

abstract sealed trait PointMultiLineSymDifferenceResult extends GeometryResultMethods
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

abstract sealed trait MultiPointMultiLineSymDifferenceResult extends GeometryResultMethods
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

abstract sealed trait PointMultiPolygonSymDifferenceResult extends GeometryResultMethods
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

abstract sealed trait MultiPointMultiPolygonSymDifferenceResult extends GeometryResultMethods
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

abstract sealed trait OneDimensionOneDimensionSymDifferenceResult extends GeometryResultMethods
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

abstract sealed trait AtMostOneDimensionPolygonSymDifferenceResult extends GeometryResultMethods
object AtMostOneDimensionPolygonSymDifferenceResult {
  implicit def jtsToResult(geom: jts.Geometry): AtMostOneDimensionPolygonSymDifferenceResult =
    geom match {
      case p: jts.Polygon => PolygonResult(p)
      case gc: jts.GeometryCollection => GeometryCollectionResult(gc)
      case _ =>
        sys.error(s"Unexpected result for AtMostOneDimension-Polygon symDifference: ${geom.getGeometryType}")
    }
}

abstract sealed trait LineMultiPolygonSymDifferenceResult extends GeometryResultMethods
object LineMultiPolygonSymDifferenceResult {
  implicit def jtsToResult(geom: jts.Geometry): LineMultiPolygonSymDifferenceResult =
    geom match {
      case l: jts.LineString => LineResult(l)
      case p: jts.Polygon => PolygonResult(p)
      case mp: jts.MultiPolygon => MultiPolygonResult(mp)
      case gc: jts.GeometryCollection => GeometryCollectionResult(gc)
      case _ =>
        sys.error(s"Unexpected result for Line-MultiPolygon symDifference: ${geom.getGeometryType}")
    }
}

abstract sealed trait MultiLineMultiPolygonSymDifferenceResult extends GeometryResultMethods
object MultiLineMultiPolygonSymDifferenceResult {
  implicit def jtsToResult(geom: jts.Geometry): MultiLineMultiPolygonSymDifferenceResult =
    geom match {
      case g: jts.Geometry if g.isEmpty => NoResult
      case p: jts.Polygon => PolygonResult(p)
      case ml: jts.MultiLineString => MultiLineResult(ml)
      case mp: jts.MultiPolygon => MultiPolygonResult(mp)
      case gc: jts.GeometryCollection => GeometryCollectionResult(gc)
      case _ =>
        sys.error(s"Unexpected result for MultiLine-MultiPolygon symDifference: ${geom.getGeometryType}")
    }
}

abstract sealed trait TwoDimensionsTwoDimensionsSymDifferenceResult extends GeometryResultMethods
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

abstract sealed trait MultiLineMultiLineSymDifferenceResult extends GeometryResultMethods
object MultiLineMultiLineSymDifferenceResult {
  implicit def jtsToResult(geom: jts.Geometry): MultiLineMultiLineSymDifferenceResult =
    geom match {
      case g: jts.Geometry if g.isEmpty => NoResult
      case p: jts.Point => PointResult(p)
      case l: jts.LineString => LineResult(l)
      case mp: jts.MultiPoint => MultiPointResult(mp)
      case ml: jts.MultiLineString => MultiLineResult(ml)
      case _ =>
        sys.error(s"Unexpected result for MultiLine-MultiLine symDifference: ${geom.getGeometryType}")
    }
}

abstract sealed trait MultiPointMultiPointSymDifferenceResult extends GeometryResultMethods
object MultiPointMultiPointSymDifferenceResult {
  implicit def jtsToResult(geom: jts.Geometry): MultiPointMultiPointSymDifferenceResult =
    geom match {
      case g: jts.Geometry if g.isEmpty => NoResult
      case p: jts.Point => PointResult(p)
      case mp: jts.MultiPoint => MultiPointResult(mp)
      case x =>
        sys.error(s"Unexpected result for MultiPoint-MultiPoint symDifference: ${geom.getGeometryType}")
    }
}

abstract sealed trait MultiPolygonMultiPolygonSymDifferenceResult extends GeometryResultMethods
object MultiPolygonMultiPolygonSymDifferenceResult {
  implicit def jtsToResult(geom: jts.Geometry): MultiPolygonMultiPolygonSymDifferenceResult =
    geom match {
      case g: jts.Geometry if g.isEmpty => NoResult
      case p: jts.Point => PointResult(p)
      case l: jts.LineString => LineResult(l)
      case p: jts.Polygon => PolygonResult(p)
      case mp: jts.MultiPoint => MultiPointResult(mp)
      case ml: jts.MultiLineString => MultiLineResult(ml)
      case mp: jts.MultiPolygon => MultiPolygonResult(mp)
      case _ =>
        sys.error(s"Unexpected result for MultiPolygon-MultiPolygon symDifference: ${geom.getGeometryType}")
    }
}


// -- Misc.

abstract sealed trait PointOrNoResult extends GeometryResultMethods
object PointOrNoResult {
  implicit def jtsToResult(geom: jts.Geometry): PointOrNoResult =
    geom match {
      case g: jts.Geometry if g.isEmpty => NoResult
      case p: jts.Point => PointResult(p)
      case _ =>
        sys.error(s"Unexpected result: ${geom.getGeometryType}")
    }
}

abstract sealed trait PolygonOrNoResult extends GeometryResultMethods
object PolygonOrNoResult {
  implicit def jtsToResult(geom: jts.Geometry): PolygonOrNoResult =
    geom match {
      case g: jts.Geometry if g.isEmpty => NoResult
      case p: jts.Polygon => PolygonResult(p)
      case _ =>
        sys.error(s"Unexpected result: ${geom.getGeometryType}")
    }
}

case object NoResult extends GeometryResult
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
    with TwoDimensionsTwoDimensionsSeqUnionResult
    with MultiPointMultiPointIntersectionResult
    with MultiPointMultiPointUnionResult
    with MultiPointMultiLineUnionResult
    with MultiPointMultiPolygonUnionResult
    with PointOrNoResult
    with PolygonOrNoResult
    with MultiPointMultiLineSymDifferenceResult
    with MultiPointMultiPolygonSymDifferenceResult
    with MultiLineMultiLineUnionResult
    with MultiLineMultiPolygonUnionResult
    with MultiLineGeometryDifferenceResult
    with MultiLineMultiPolygonSymDifferenceResult
    with MultiLineMultiLineIntersectionResult
    with MultiPolygonMultiPolygonIntersectionResult
    with MultiLineMultiLineSymDifferenceResult
    with MultiPointMultiPointSymDifferenceResult
    with MultiPolygonMultiPolygonSymDifferenceResult
    with MultiLineMultiLineDifferenceResult
    with MultiPointMultiPointDifferenceResult
    with MultiPolygonMultiPolygonDifferenceResult {
  def toGeometry(): Option[Geometry] = None
}

case class PointResult(geom: Point) extends GeometryResult
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
    with MultiLineMultiLineIntersectionResult
    with MultiPolygonMultiPolygonIntersectionResult
    with MultiLineMultiLineSymDifferenceResult
    with MultiPointMultiPointSymDifferenceResult
    with MultiPolygonMultiPolygonSymDifferenceResult
    with MultiLineMultiLineDifferenceResult
    with MultiPointMultiPointDifferenceResult
    with MultiPolygonMultiPolygonDifferenceResult
    with PointOrNoResult {
  def toGeometry(): Option[Geometry] = Some(geom)
}

case class LineResult(geom: Line) extends GeometryResult
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
    with LineMultiPolygonSymDifferenceResult
    with MultiPointMultiLineUnionResult
    with MultiPointMultiLineSymDifferenceResult
    with MultiLineMultiLineUnionResult
    with MultiLineGeometryDifferenceResult
    with MultiLineMultiLineIntersectionResult
    with MultiPolygonMultiPolygonIntersectionResult
    with MultiLineMultiLineSymDifferenceResult
    with MultiPolygonMultiPolygonSymDifferenceResult
    with MultiLineMultiLineDifferenceResult
    with MultiPolygonMultiPolygonDifferenceResult {
  def toGeometry(): Option[Geometry] = Some(geom)
}

object LineResult {
  implicit def jtsToResult(geom: jts.Geometry): LineResult =
    geom match {
      case ml: jts.LineString => LineResult(ml)
      case _ =>
        sys.error(s"Unexpected result: ${geom.getGeometryType}")
    }
}

case class PolygonResult(geom: Polygon) extends GeometryResult
    with TwoDimensionsTwoDimensionsIntersectionResult
    with AtMostOneDimensionPolygonUnionResult
    with TwoDimensionsTwoDimensionsUnionResult
    with TwoDimensionsTwoDimensionsSeqUnionResult
    with LineMultiPolygonUnionResult
    with PolygonAtMostOneDimensionDifferenceResult
    with AtMostOneDimensionPolygonSymDifferenceResult
    with TwoDimensionsTwoDimensionsDifferenceResult
    with TwoDimensionsTwoDimensionsSymDifferenceResult
    with PointMultiPolygonSymDifferenceResult
    with LineMultiPolygonSymDifferenceResult
    with PointMultiPolygonUnionResult
    with PolygonOrNoResult
    with MultiPointMultiPolygonUnionResult
    with MultiPointMultiPolygonSymDifferenceResult
    with MultiLineMultiPolygonUnionResult
    with MultiLineMultiPolygonSymDifferenceResult
    with MultiPolygonMultiPolygonIntersectionResult
    with MultiPolygonMultiPolygonSymDifferenceResult
    with MultiPolygonMultiPolygonDifferenceResult {
  def toGeometry(): Option[Geometry] = Some(geom)
}

case class MultiPointResult(geom: MultiPoint) extends GeometryResult
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
    with MultiLineMultiLineIntersectionResult
    with MultiPolygonMultiPolygonIntersectionResult
    with MultiLineMultiLineSymDifferenceResult
    with MultiPointMultiPointSymDifferenceResult
    with MultiPolygonMultiPolygonSymDifferenceResult
    with MultiLineMultiLineDifferenceResult
    with MultiPointMultiPointDifferenceResult
    with MultiPolygonMultiPolygonDifferenceResult {
  def toGeometry(): Option[Geometry] = Some(geom)
}

case class MultiLineResult(geom: MultiLine) extends GeometryResult
    with TwoDimensionsTwoDimensionsIntersectionResult
    with LineOneDimensionUnionResult
    with LineAtLeastOneDimensionDifferenceResult
    with PointMultiLineUnionResult
    with MultiLineGeometryDifferenceResult
    with PolygonBoundaryResult
    with OneDimensionOneDimensionSymDifferenceResult
    with PointMultiLineSymDifferenceResult
    with OneDimensionAtLeastOneDimensionIntersectionResult
    with MultiPointMultiLineUnionResult
    with MultiPointMultiLineSymDifferenceResult
    with MultiLineMultiLineUnionResult
    with MultiLineMultiPolygonUnionResult
    with MultiLineMultiPolygonSymDifferenceResult
    with MultiLineMultiLineIntersectionResult
    with MultiPolygonMultiPolygonIntersectionResult
    with MultiLineMultiLineSymDifferenceResult
    with MultiPolygonMultiPolygonSymDifferenceResult
    with MultiLineMultiLineDifferenceResult
    with MultiPolygonMultiPolygonDifferenceResult {
  def toGeometry(): Option[Geometry] = Some(geom)
}

object MultiLineResult {
  implicit def jtsToResult(geom: jts.Geometry): MultiLineResult =
    geom match {
      case ml: jts.MultiLineString => MultiLineResult(ml)
      case _ =>
        sys.error(s"Unexpected result: ${geom.getGeometryType}")
    }
}

case class MultiPolygonResult(geom: MultiPolygon) extends GeometryResult
    with TwoDimensionsTwoDimensionsIntersectionResult
    with TwoDimensionsTwoDimensionsUnionResult
    with TwoDimensionsTwoDimensionsSeqUnionResult
    with LineMultiPolygonUnionResult
    with TwoDimensionsTwoDimensionsDifferenceResult
    with MultiPolygonXDifferenceResult
    with TwoDimensionsTwoDimensionsSymDifferenceResult
    with PointMultiPolygonSymDifferenceResult
    with LineMultiPolygonSymDifferenceResult
    with PointMultiPolygonUnionResult
    with MultiPointMultiPolygonUnionResult
    with MultiPointMultiPolygonSymDifferenceResult
    with MultiLineMultiPolygonUnionResult
    with MultiLineMultiPolygonSymDifferenceResult
    with MultiPolygonMultiPolygonIntersectionResult
    with MultiPolygonMultiPolygonSymDifferenceResult
    with MultiPolygonMultiPolygonDifferenceResult {
  def toGeometry(): Option[Geometry] = Some(geom)
}

case class GeometryCollectionResult(geom: GeometryCollection) extends GeometryResult
    with TwoDimensionsTwoDimensionsIntersectionResult
    with ZeroDimensionsLineUnionResult
    with AtMostOneDimensionPolygonUnionResult
    with LineMultiPolygonUnionResult
    with PointMultiLineUnionResult
    with ZeroDimensionsLineSymDifferenceResult
    with AtMostOneDimensionPolygonSymDifferenceResult
    with PointMultiLineSymDifferenceResult
    with PointMultiPolygonSymDifferenceResult
    with LineMultiPolygonSymDifferenceResult
    with OneDimensionAtLeastOneDimensionIntersectionResult
    with PointMultiPolygonUnionResult
    with MultiPointMultiLineUnionResult
    with MultiPointMultiPolygonUnionResult
    with MultiPointMultiLineSymDifferenceResult
    with MultiPointMultiPolygonSymDifferenceResult
    with MultiLineMultiPolygonUnionResult
    with MultiLineMultiPolygonSymDifferenceResult {
  def toGeometry(): Option[Geometry] = Some(geom)
}
