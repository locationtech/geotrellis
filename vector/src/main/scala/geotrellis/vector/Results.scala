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

  /** returns this result as a MultiLineStrng if it's a LineString or MultiLineString, otherwise returns None */
  def asMultiLineString: Option[MultiLineString] =
    toGeometry.flatMap { g =>
      g match {
        case l: LineString => Some(MultiLineString(l))
        case ml: MultiLineString => Some(ml)
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

  implicit def jtsToResult(geom: Geometry): GeometryResult =
    geom match {
      case null => NoResult
      case g: Geometry if g.isEmpty => NoResult
      case p: Point => PointResult(p)
      case l: LineString => LineStringResult(l)
      case p: Polygon => PolygonResult(p)
      case mp: MultiPoint => MultiPointResult(mp)
      case ml: MultiLineString => MultiLineStringResult(ml)
      case mp: MultiPolygon => MultiPolygonResult(mp)
      case gc: GeometryCollection => GeometryCollectionResult(gc)
      case _ =>
        sys.error(s"Unexpected result: ${geom.getGeometryType}")
    }
}

// -- Intersection

abstract sealed trait OneDimensionAtLeastOneDimensionIntersectionResult extends GeometryResultMethods
object OneDimensionAtLeastOneDimensionIntersectionResult {
  implicit def jtsToResult(geom: Geometry): OneDimensionAtLeastOneDimensionIntersectionResult =
    geom match {
      case g: Geometry if g.isEmpty => NoResult
      case p: Point => PointResult(p)
      case l: LineString => LineStringResult(l)
      case mp: MultiPoint => MultiPointResult(mp)
      case ml: MultiLineString => MultiLineStringResult(ml)
      case gc: GeometryCollection => GeometryCollectionResult(gc)
      case _ =>
        sys.error(s"Unexpected result for OneDimension-AtLeastOneDimension intersection: ${geom.getGeometryType}")
    }
}

abstract sealed trait TwoDimensionsTwoDimensionsIntersectionResult extends GeometryResultMethods
object TwoDimensionsTwoDimensionsIntersectionResult {
  implicit def jtsToResult(geom: Geometry): TwoDimensionsTwoDimensionsIntersectionResult =
    geom match {
      case g: Geometry if g.isEmpty => NoResult
      case p: Point => PointResult(p)
      case l: LineString => LineStringResult(l)
      case p: Polygon => PolygonResult(p)
      case mp: MultiPoint => MultiPointResult(mp)
      case ml: MultiLineString => MultiLineStringResult(ml)
      case mp: MultiPolygon => MultiPolygonResult(mp)
      case gc: GeometryCollection => GeometryCollectionResult(gc)
      case _ =>
        sys.error(s"Unexpected result for TwoDimensions-TwoDimensions intersection: ${geom.getGeometryType}")
    }
}

abstract sealed trait MultiLineStringMultiLineStringIntersectionResult extends GeometryResultMethods
object MultiLineStringMultiLineStringIntersectionResult {
  implicit def jtsToResult(geom: Geometry): MultiLineStringMultiLineStringIntersectionResult =
    geom match {
      case g: Geometry if g.isEmpty => NoResult
      case p: Point => PointResult(p)
      case l: LineString => LineStringResult(l)
      case mp: MultiPoint => MultiPointResult(mp)
      case ml: MultiLineString => MultiLineStringResult(ml)
      case _ =>
        sys.error(s"Unexpected result for MultiLineString-MultiLineString intersection: ${geom.getGeometryType}")
    }
}

abstract sealed trait MultiPointMultiPointIntersectionResult extends GeometryResultMethods
object MultiPointMultiPointIntersectionResult {
  implicit def jtsToResult(geom: Geometry): MultiPointMultiPointIntersectionResult =
    geom match {
      case g: Geometry if g.isEmpty => NoResult
      case p: Point => PointResult(p)
      case mp: MultiPoint => MultiPointResult(mp)
      case x =>
        sys.error(s"Unexpected result for MultiPoint-MultiPoint intersection: ${geom.getGeometryType}")
    }
}

abstract sealed trait MultiPolygonMultiPolygonIntersectionResult extends GeometryResultMethods
object MultiPolygonMultiPolygonIntersectionResult {
  implicit def jtsToResult(geom: Geometry): MultiPolygonMultiPolygonIntersectionResult =
    geom match {
      case g: Geometry if g.isEmpty => NoResult
      case p: Point => PointResult(p)
      case l: LineString => LineStringResult(l)
      case p: Polygon => PolygonResult(p)
      case mp: MultiPoint => MultiPointResult(mp)
      case ml: MultiLineString => MultiLineStringResult(ml)
      case mp: MultiPolygon => MultiPolygonResult(mp)
      case _ =>
        sys.error(s"Unexpected result for MultiPolygon-MultiPolygon intersection: ${geom.getGeometryType}")
    }
}

abstract sealed trait MultiPointAtLeastOneDimensionIntersectionResult extends GeometryResultMethods
object MultiPointAtLeastOneDimensionIntersectionResult {
  implicit def jtsToResult(geom: Geometry): MultiPointAtLeastOneDimensionIntersectionResult =
    geom match {
      case g: Geometry if g.isEmpty => NoResult
      case p: Point => PointResult(p)
      case mp: MultiPoint => MultiPointResult(mp)
      case x =>
        sys.error(s"Unexpected result for MultiPoint-AtLeastOneDimension intersection: ${geom.getGeometryType}")
    }
}

// -- Union

abstract sealed trait PointZeroDimensionsUnionResult extends GeometryResultMethods
object PointZeroDimensionsUnionResult {
  implicit def jtsToResult(geom: Geometry): PointZeroDimensionsUnionResult =
    geom match {
      case p: Point => PointResult(p)
      case mp: MultiPoint => MultiPointResult(mp)
      case _ =>
        sys.error(s"Unexpected result for Point-ZeroDimensions union: ${geom.getGeometryType}")
    }
}

abstract sealed trait MultiPointMultiPointUnionResult extends GeometryResultMethods
object MultiPointMultiPointUnionResult {
  implicit def jtsToResult(geom: Geometry): MultiPointMultiPointUnionResult =
    geom match {
      case g: Geometry if g.isEmpty => NoResult
      case p: Point => PointResult(p)
      case mp: MultiPoint => MultiPointResult(mp)
      case _ =>
        sys.error(s"Unexpected result for MultiPoint-MultiPoint union: ${geom.getGeometryType}")
    }
}

abstract sealed trait ZeroDimensionsLineStringUnionResult extends GeometryResultMethods
object ZeroDimensionsLineStringUnionResult {
  implicit def jtsToResult(geom: Geometry): ZeroDimensionsLineStringUnionResult =
    geom match {
      case l: LineString => LineStringResult(l)
      case gc: GeometryCollection => GeometryCollectionResult(gc)
      case _ =>
        sys.error(s"Unexpected result for ZeroDimensions-LineString union: ${geom.getGeometryType}")
    }
}

abstract sealed trait PointMultiLineStringUnionResult extends GeometryResultMethods
object PointMultiLineStringUnionResult {
  implicit def jtsToResult(geom: Geometry): PointMultiLineStringUnionResult =
    geom match {
      case p: Point => PointResult(p)
      case l: LineString => LineStringResult(l)
      case ml: MultiLineString => MultiLineStringResult(ml)
      case gc: GeometryCollection => GeometryCollectionResult(gc)
      case _ =>
        sys.error(s"Unexpected result for Point-MultiLineString union: ${geom.getGeometryType}")
    }
}

abstract sealed trait MultiPointMultiLineStringUnionResult extends GeometryResultMethods
object MultiPointMultiLineStringUnionResult {
  implicit def jtsToResult(geom: Geometry): MultiPointMultiLineStringUnionResult =
    geom match {
      case g: Geometry if g.isEmpty => NoResult
      case l: LineString => LineStringResult(l)
      case mp: MultiPoint => MultiPointResult(mp)
      case ml: MultiLineString => MultiLineStringResult(ml)
      case gc: GeometryCollection => GeometryCollectionResult(gc)
      case _ =>
        sys.error(s"Unexpected result for MultiPoint-MultiLineString union: ${geom.getGeometryType}")
    }
}

abstract sealed trait LineStringOneDimensionUnionResult extends GeometryResultMethods
object LineStringOneDimensionUnionResult {
  implicit def jtsToResult(geom: Geometry): LineStringOneDimensionUnionResult =
    geom match {
      case l: LineString => LineStringResult(l)
      case ml: MultiLineString => MultiLineStringResult(ml)
      case _ =>
        sys.error(s"Unexpected result for LineString-OneDimension union: ${geom.getGeometryType}")
    }
}

abstract sealed trait MultiLineStringMultiLineStringUnionResult extends GeometryResultMethods
object MultiLineStringMultiLineStringUnionResult {
  implicit def jtsToResult(geom: Geometry): MultiLineStringMultiLineStringUnionResult =
    geom match {
      case g: Geometry if g.isEmpty => NoResult
      case l: LineString => LineStringResult(l)
      case ml: MultiLineString => MultiLineStringResult(ml)
      case _ =>
        sys.error(s"Unexpected result for MultiLineString-MultiLineString union: ${geom.getGeometryType}")
    }
}

abstract sealed trait AtMostOneDimensionPolygonUnionResult extends GeometryResultMethods
object AtMostOneDimensionPolygonUnionResult {
  implicit def jtsToResult(geom: Geometry): AtMostOneDimensionPolygonUnionResult =
    geom match {
      case p: Polygon => PolygonResult(p)
      case gc: GeometryCollection => GeometryCollectionResult(gc)
      case _ =>
        sys.error(s"Unexpected result for AtMostOneDimension-Polygon union: ${geom.getGeometryType}")
    }
}

abstract sealed trait TwoDimensionsTwoDimensionsUnionResult extends GeometryResultMethods
object TwoDimensionsTwoDimensionsUnionResult {
  implicit def jtsToResult(geom: Geometry): TwoDimensionsTwoDimensionsUnionResult =
    geom match {
      case p: Polygon => PolygonResult(p)
      case mp: MultiPolygon => MultiPolygonResult(mp)
      case _ =>
        sys.error(s"Unexpected result for TwoDimensions-TwoDimensions union: ${geom.getGeometryType}")
    }
}

abstract sealed trait TwoDimensionsTwoDimensionsSeqUnionResult extends GeometryResultMethods
object TwoDimensionsTwoDimensionsSeqUnionResult {
  implicit def jtsToResult(geom: Geometry): TwoDimensionsTwoDimensionsSeqUnionResult =
    geom match {
      case g: Geometry if g.isEmpty => NoResult
      case p: Polygon => PolygonResult(p)
      case mp: MultiPolygon => MultiPolygonResult(mp)
      case _ =>
        sys.error(s"Unexpected result for TwoDimensions-TwoDimensions union: ${geom.getGeometryType}")
    }
}

abstract sealed trait PointMultiPolygonUnionResult extends GeometryResultMethods
object PointMultiPolygonUnionResult {
  implicit def jtsToResult(geom: Geometry): PointMultiPolygonUnionResult =
    geom match {
      case pt: Point => PointResult(pt)
      case p: Polygon => PolygonResult(p)
      case mp: MultiPolygon => MultiPolygonResult(mp)
      case gc: GeometryCollection => GeometryCollectionResult(gc)
      case _ =>
        sys.error(s"Unexpected result for Point-MultiPolygon union: ${geom.getGeometryType}")
    }
}

abstract sealed trait LineStringMultiPolygonUnionResult extends GeometryResultMethods
object LineStringMultiPolygonUnionResult {
  implicit def jtsToResult(geom: Geometry): LineStringMultiPolygonUnionResult =
    geom match {
      case l: LineString => LineStringResult(l)
      case p: Polygon => PolygonResult(p)
      case mp: MultiPolygon => MultiPolygonResult(mp)
      case gc: GeometryCollection => GeometryCollectionResult(gc)
      case _ =>
        sys.error(s"Unexpected result for LineString-MultiPolygon union: ${geom.getGeometryType}")
    }
}

abstract sealed trait MultiPointMultiPolygonUnionResult extends GeometryResultMethods
object MultiPointMultiPolygonUnionResult {
  implicit def jtsToResult(geom: Geometry): MultiPointMultiPolygonUnionResult =
    geom match {
      case g: Geometry if g.isEmpty => NoResult
      case p: Polygon => PolygonResult(p)
      case mpt: MultiPoint => MultiPointResult(mpt)
      case mp: MultiPolygon => MultiPolygonResult(mp)
      case gc: GeometryCollection => GeometryCollectionResult(gc)
      case _ =>
        sys.error(s"Unexpected result for MultiPoint-MultiPolygon union: ${geom.getGeometryType}")
    }
}

abstract sealed trait MultiLineStringMultiPolygonUnionResult extends GeometryResultMethods
object MultiLineStringMultiPolygonUnionResult {
  implicit def jtsToResult(geom: Geometry): MultiLineStringMultiPolygonUnionResult =
    geom match {
      case g: Geometry if g.isEmpty => NoResult
      case p: Polygon => PolygonResult(p)
      case ml: MultiLineString => MultiLineStringResult(ml)
      case mp: MultiPolygon => MultiPolygonResult(mp)
      case gc: GeometryCollection => GeometryCollectionResult(gc)
      case _ =>
        sys.error(s"Unexpected result for MultiPoint-MultiPolygon union: ${geom.getGeometryType}")
    }
}

// -- Difference

abstract sealed trait PointGeometryDifferenceResult extends GeometryResultMethods
object PointGeometryDifferenceResult {
  implicit def jtsToResult(geom: Geometry): PointGeometryDifferenceResult =
    geom match {
      case g: Geometry if g.isEmpty => NoResult
      case p: Point => PointResult(p)
      case _ =>
        sys.error(s"Unexpected result for Point-Geometry difference: ${geom.getGeometryType}")
    }
}

abstract sealed trait LineStringAtLeastOneDimensionDifferenceResult extends GeometryResultMethods
object LineStringAtLeastOneDimensionDifferenceResult {
  implicit def jtsToResult(geom: Geometry): LineStringAtLeastOneDimensionDifferenceResult =
    geom match {
      case g: Geometry if g.isEmpty => NoResult
      case l: LineString => LineStringResult(l)
      case ml: MultiLineString => MultiLineStringResult(ml)
      case _ =>
        sys.error(s"Unexpected result for LineString-AtLeastOneDimension difference: ${geom.getGeometryType}")
    }
}

abstract sealed trait PolygonAtMostOneDimensionDifferenceResult extends GeometryResultMethods
object PolygonAtMostOneDimensionDifferenceResult {
  implicit def jtsToResult(geom: Geometry): PolygonAtMostOneDimensionDifferenceResult =
    geom match {
      case p: Polygon => PolygonResult(p)
      case _ =>
        sys.error(s"Unexpected result for Polygon-AtMostOneDimension difference: ${geom.getGeometryType}")
    }
}

abstract sealed trait TwoDimensionsTwoDimensionsDifferenceResult extends GeometryResultMethods
object TwoDimensionsTwoDimensionsDifferenceResult {
  implicit def jtsToResult(geom: Geometry): TwoDimensionsTwoDimensionsDifferenceResult =
    geom match {
      case g: Geometry if g.isEmpty => NoResult
      case p: Polygon => PolygonResult(p)
      case mp: MultiPolygon => MultiPolygonResult(mp)
      case _ =>
        sys.error(s"Unexpected result for TwoDimensions-TwoDimensions difference: ${geom.getGeometryType}")
    }
}

abstract sealed trait MultiPointGeometryDifferenceResult extends GeometryResultMethods
object MultiPointGeometryDifferenceResult {
  implicit def jtsToResult(geom: Geometry): MultiPointGeometryDifferenceResult =
    geom match {
      case g: Geometry if g.isEmpty => NoResult
      case p: Point => PointResult(p)
      case mp: MultiPoint => MultiPointResult(mp)
      case _ =>
        sys.error(s"Unexpected result for MultiPoint-Geometry difference: ${geom.getGeometryType}")
    }
}

abstract sealed trait MultiLineStringGeometryDifferenceResult extends GeometryResultMethods
object MultiLineStringGeometryDifferenceResult {
  implicit def jtsToResult(geom: Geometry): MultiLineStringGeometryDifferenceResult =
    geom match {
      case g: Geometry if g.isEmpty => NoResult
      case l: LineString => LineStringResult(l)
      case ml: MultiLineString => MultiLineStringResult(ml)
      case _ =>
        sys.error(s"Unexpected result for MultiLineString-Geometry difference: ${geom.getGeometryType}")
    }
}

abstract sealed trait MultiPolygonXDifferenceResult extends GeometryResultMethods
object MultiPolygonXDifferenceResult {
  implicit def jtsToResult(geom: Geometry): MultiPolygonXDifferenceResult =
    geom match {
      case mp: MultiPolygon => MultiPolygonResult(mp)
      case _ =>
        sys.error(s"Unexpected result for Polygon difference: ${geom.getGeometryType}")
    }
}


abstract sealed trait MultiLineStringMultiLineStringDifferenceResult extends GeometryResultMethods
object MultiLineStringMultiLineStringDifferenceResult {
  implicit def jtsToResult(geom: Geometry): MultiLineStringMultiLineStringDifferenceResult =
    geom match {
      case g: Geometry if g.isEmpty => NoResult
      case p: Point => PointResult(p)
      case l: LineString => LineStringResult(l)
      case mp: MultiPoint => MultiPointResult(mp)
      case ml: MultiLineString => MultiLineStringResult(ml)
      case _ =>
        sys.error(s"Unexpected result for MultiLineString-MultiLineString difference: ${geom.getGeometryType}")
    }
}

abstract sealed trait MultiPointMultiPointDifferenceResult extends GeometryResultMethods
object MultiPointMultiPointDifferenceResult {
  implicit def jtsToResult(geom: Geometry): MultiPointMultiPointDifferenceResult =
    geom match {
      case g: Geometry if g.isEmpty => NoResult
      case p: Point => PointResult(p)
      case mp: MultiPoint => MultiPointResult(mp)
      case x =>
        sys.error(s"Unexpected result for MultiPoint-MultiPoint difference: ${geom.getGeometryType}")
    }
}

abstract sealed trait MultiPolygonMultiPolygonDifferenceResult extends GeometryResultMethods
object MultiPolygonMultiPolygonDifferenceResult {
  implicit def jtsToResult(geom: Geometry): MultiPolygonMultiPolygonDifferenceResult =
    geom match {
      case g: Geometry if g.isEmpty => NoResult
      case p: Point => PointResult(p)
      case l: LineString => LineStringResult(l)
      case p: Polygon => PolygonResult(p)
      case mp: MultiPoint => MultiPointResult(mp)
      case ml: MultiLineString => MultiLineStringResult(ml)
      case mp: MultiPolygon => MultiPolygonResult(mp)
      case _ =>
        sys.error(s"Unexpected result for MultiPolygon-MultiPolygon difference: ${geom.getGeometryType}")
    }
}
// -- Boundary

abstract sealed trait OneDimensionBoundaryResult extends GeometryResultMethods
object OneDimensionBoundaryResult {
  implicit def jtsToResult(geom: Geometry): OneDimensionBoundaryResult =
    geom match {
      case g: Geometry if g.isEmpty => NoResult
      case mp: MultiPoint => MultiPointResult(mp)
      case _ =>
        sys.error(s"Unexpected result for LineString boundary: ${geom.getGeometryType}")
    }
}

abstract sealed trait PolygonBoundaryResult extends GeometryResultMethods
object PolygonBoundaryResult {
  implicit def jtsToResult(geom: Geometry): PolygonBoundaryResult =
    geom match {
      case l: LineString => LineStringResult(l)
      case ml: MultiLineString => MultiLineStringResult(ml)
      case _ =>
        sys.error(s"Unexpected result for Polygon boundary: ${geom.getGeometryType}")
    }
}

// -- SymDifference

abstract sealed trait PointPointSymDifferenceResult extends GeometryResultMethods
object PointPointSymDifferenceResult {
  implicit def jtsToResult(geom: Geometry): PointPointSymDifferenceResult =
    geom match {
      case g: Geometry if g.isEmpty => NoResult
      case mp: MultiPoint => MultiPointResult(mp)
      case _ =>
        sys.error(s"Unexpected result for Point-Point symDifference: ${geom.getGeometryType}")

    }
}

abstract sealed trait ZeroDimensionsMultiPointSymDifferenceResult extends GeometryResultMethods
object ZeroDimensionsMultiPointSymDifferenceResult {
  implicit def jtsToResult(geom: Geometry): ZeroDimensionsMultiPointSymDifferenceResult =
    geom match {
      case g: Geometry if g.isEmpty => NoResult
      case p: Point => PointResult(p)
      case mp: MultiPoint => MultiPointResult(mp)
      case _ =>
        sys.error(s"Unexpected result for ZeroDimensions-MultiPoint symDifference: ${geom.getGeometryType}")
    }
}

abstract sealed trait ZeroDimensionsLineStringSymDifferenceResult extends GeometryResultMethods
object ZeroDimensionsLineStringSymDifferenceResult {
  implicit def jtsToResult(geom: Geometry): ZeroDimensionsLineStringSymDifferenceResult =
    geom match {
      case l: LineString => LineStringResult(l)
      case gc: GeometryCollection => GeometryCollectionResult(gc)
      case _ =>
        sys.error(s"Unexpected result for ZeroDimensions-LineString symDifference: ${geom.getGeometryType}")
    }
}

abstract sealed trait PointMultiLineStringSymDifferenceResult extends GeometryResultMethods
object PointMultiLineStringSymDifferenceResult {
  implicit def jtsToResult(geom: Geometry): PointMultiLineStringSymDifferenceResult =
    geom match {
      case p: Point => PointResult(p)
      case l: LineString => LineStringResult(l)
      case ml: MultiLineString => MultiLineStringResult(ml)
      case gc: GeometryCollection => GeometryCollectionResult(gc)
      case _ =>
        sys.error(s"Unexpected result for Point-MultiLineString symDifference: ${geom.getGeometryType}")
    }
}

abstract sealed trait MultiPointMultiLineStringSymDifferenceResult extends GeometryResultMethods
object MultiPointMultiLineStringSymDifferenceResult {
  implicit def jtsToResult(geom: Geometry): MultiPointMultiLineStringSymDifferenceResult =
    geom match {
      case g: Geometry if g.isEmpty => NoResult
      case l: LineString => LineStringResult(l)
      case mp: MultiPoint => MultiPointResult(mp)
      case ml: MultiLineString => MultiLineStringResult(ml)
      case gc: GeometryCollection => GeometryCollectionResult(gc)
      case _ =>
        sys.error(s"Unexpected result for MultiPoint-MultiLineString symDifference: ${geom.getGeometryType}")
    }
}

abstract sealed trait PointMultiPolygonSymDifferenceResult extends GeometryResultMethods
object PointMultiPolygonSymDifferenceResult {
  implicit def jtsToResult(geom: Geometry): PointMultiPolygonSymDifferenceResult =
    geom match {
      case pt: Point => PointResult(pt)
      case p: Polygon => PolygonResult(p)
      case mp: MultiPolygon => MultiPolygonResult(mp)
      case gc: GeometryCollection => GeometryCollectionResult(gc)
      case _ =>
        sys.error(s"Unexpected result for Point-MultiPolygon symDifference: ${geom.getGeometryType}")
    }
}

abstract sealed trait MultiPointMultiPolygonSymDifferenceResult extends GeometryResultMethods
object MultiPointMultiPolygonSymDifferenceResult {
  implicit def jtsToResult(geom: Geometry): MultiPointMultiPolygonSymDifferenceResult =
    geom match {
      case g: Geometry if g.isEmpty => NoResult
      case p: Polygon => PolygonResult(p)
      case mpt: MultiPoint => MultiPointResult(mpt)
      case mp: MultiPolygon => MultiPolygonResult(mp)
      case gc: GeometryCollection => GeometryCollectionResult(gc)
      case _ =>
        sys.error(s"Unexpected result for MultiPoint-MultiPolygon symDifference: ${geom.getGeometryType}")
    }
}

abstract sealed trait OneDimensionOneDimensionSymDifferenceResult extends GeometryResultMethods
object OneDimensionOneDimensionSymDifferenceResult {
  implicit def jtsToResult(geom: Geometry): OneDimensionOneDimensionSymDifferenceResult =
    geom match {
      case g: Geometry if g.isEmpty => NoResult
      case l: LineString => LineStringResult(l)
      case ml: MultiLineString => MultiLineStringResult(ml)
      case _ =>
        sys.error(s"Unexpected result for OneDimension-OneDimension symDifference: ${geom.getGeometryType}")
    }
}

abstract sealed trait AtMostOneDimensionPolygonSymDifferenceResult extends GeometryResultMethods
object AtMostOneDimensionPolygonSymDifferenceResult {
  implicit def jtsToResult(geom: Geometry): AtMostOneDimensionPolygonSymDifferenceResult =
    geom match {
      case p: Polygon => PolygonResult(p)
      case gc: GeometryCollection => GeometryCollectionResult(gc)
      case _ =>
        sys.error(s"Unexpected result for AtMostOneDimension-Polygon symDifference: ${geom.getGeometryType}")
    }
}

abstract sealed trait LineStringMultiPolygonSymDifferenceResult extends GeometryResultMethods
object LineStringMultiPolygonSymDifferenceResult {
  implicit def jtsToResult(geom: Geometry): LineStringMultiPolygonSymDifferenceResult =
    geom match {
      case l: LineString => LineStringResult(l)
      case p: Polygon => PolygonResult(p)
      case mp: MultiPolygon => MultiPolygonResult(mp)
      case gc: GeometryCollection => GeometryCollectionResult(gc)
      case _ =>
        sys.error(s"Unexpected result for LineString-MultiPolygon symDifference: ${geom.getGeometryType}")
    }
}

abstract sealed trait MultiLineStringMultiPolygonSymDifferenceResult extends GeometryResultMethods
object MultiLineStringMultiPolygonSymDifferenceResult {
  implicit def jtsToResult(geom: Geometry): MultiLineStringMultiPolygonSymDifferenceResult =
    geom match {
      case g: Geometry if g.isEmpty => NoResult
      case p: Polygon => PolygonResult(p)
      case ml: MultiLineString => MultiLineStringResult(ml)
      case mp: MultiPolygon => MultiPolygonResult(mp)
      case gc: GeometryCollection => GeometryCollectionResult(gc)
      case _ =>
        sys.error(s"Unexpected result for MultiLineString-MultiPolygon symDifference: ${geom.getGeometryType}")
    }
}

abstract sealed trait TwoDimensionsTwoDimensionsSymDifferenceResult extends GeometryResultMethods
object TwoDimensionsTwoDimensionsSymDifferenceResult {
  implicit def jtsToResult(geom: Geometry): TwoDimensionsTwoDimensionsSymDifferenceResult =
    geom match {
      case g: Geometry if g.isEmpty => NoResult
      case p: Polygon => PolygonResult(p)
      case mp: MultiPolygon => MultiPolygonResult(mp)
      case _ =>
        sys.error(s"Unexpected result for TwoDimensions-TwoDimensions symDifference: ${geom.getGeometryType}")
    }
}

abstract sealed trait MultiLineStringMultiLineStringSymDifferenceResult extends GeometryResultMethods
object MultiLineStringMultiLineStringSymDifferenceResult {
  implicit def jtsToResult(geom: Geometry): MultiLineStringMultiLineStringSymDifferenceResult =
    geom match {
      case g: Geometry if g.isEmpty => NoResult
      case p: Point => PointResult(p)
      case l: LineString => LineStringResult(l)
      case mp: MultiPoint => MultiPointResult(mp)
      case ml: MultiLineString => MultiLineStringResult(ml)
      case _ =>
        sys.error(s"Unexpected result for MultiLineString-MultiLineString symDifference: ${geom.getGeometryType}")
    }
}

abstract sealed trait MultiPointMultiPointSymDifferenceResult extends GeometryResultMethods
object MultiPointMultiPointSymDifferenceResult {
  implicit def jtsToResult(geom: Geometry): MultiPointMultiPointSymDifferenceResult =
    geom match {
      case g: Geometry if g.isEmpty => NoResult
      case p: Point => PointResult(p)
      case mp: MultiPoint => MultiPointResult(mp)
      case x =>
        sys.error(s"Unexpected result for MultiPoint-MultiPoint symDifference: ${geom.getGeometryType}")
    }
}

abstract sealed trait MultiPolygonMultiPolygonSymDifferenceResult extends GeometryResultMethods
object MultiPolygonMultiPolygonSymDifferenceResult {
  implicit def jtsToResult(geom: Geometry): MultiPolygonMultiPolygonSymDifferenceResult =
    geom match {
      case g: Geometry if g.isEmpty => NoResult
      case p: Point => PointResult(p)
      case l: LineString => LineStringResult(l)
      case p: Polygon => PolygonResult(p)
      case mp: MultiPoint => MultiPointResult(mp)
      case ml: MultiLineString => MultiLineStringResult(ml)
      case mp: MultiPolygon => MultiPolygonResult(mp)
      case _ =>
        sys.error(s"Unexpected result for MultiPolygon-MultiPolygon symDifference: ${geom.getGeometryType}")
    }
}


// -- Misc.

abstract sealed trait PointOrNoResult extends GeometryResultMethods
object PointOrNoResult {
  implicit def jtsToResult(geom: Geometry): PointOrNoResult =
    geom match {
      case g: Geometry if g.isEmpty => NoResult
      case p: Point => PointResult(p)
      case _ =>
        sys.error(s"Unexpected result: ${geom.getGeometryType}")
    }
}

abstract sealed trait PolygonOrNoResult extends GeometryResultMethods
object PolygonOrNoResult {
  implicit def jtsToResult(geom: Geometry): PolygonOrNoResult =
    geom match {
      case g: Geometry if g.isEmpty => NoResult
      case p: Polygon => PolygonResult(p)
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
    with LineStringAtLeastOneDimensionDifferenceResult
    with TwoDimensionsTwoDimensionsDifferenceResult
    with MultiPointGeometryDifferenceResult
    with PointPointSymDifferenceResult
    with OneDimensionOneDimensionSymDifferenceResult
    with TwoDimensionsTwoDimensionsSymDifferenceResult
    with ZeroDimensionsMultiPointSymDifferenceResult
    with TwoDimensionsTwoDimensionsSeqUnionResult
    with MultiPointMultiPointIntersectionResult
    with MultiPointMultiPointUnionResult
    with MultiPointMultiLineStringUnionResult
    with MultiPointMultiPolygonUnionResult
    with PointOrNoResult
    with PolygonOrNoResult
    with MultiPointMultiLineStringSymDifferenceResult
    with MultiPointMultiPolygonSymDifferenceResult
    with MultiLineStringMultiLineStringUnionResult
    with MultiLineStringMultiPolygonUnionResult
    with MultiLineStringGeometryDifferenceResult
    with MultiLineStringMultiPolygonSymDifferenceResult
    with MultiLineStringMultiLineStringIntersectionResult
    with MultiPolygonMultiPolygonIntersectionResult
    with MultiLineStringMultiLineStringSymDifferenceResult
    with MultiPointMultiPointSymDifferenceResult
    with MultiPolygonMultiPolygonSymDifferenceResult
    with MultiLineStringMultiLineStringDifferenceResult
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
    with LineStringMultiPolygonUnionResult
    with PointMultiLineStringUnionResult
    with PointMultiPolygonUnionResult
    with PointMultiLineStringSymDifferenceResult
    with PointMultiPolygonSymDifferenceResult
    with MultiPointMultiPointUnionResult
    with MultiLineStringMultiLineStringIntersectionResult
    with MultiPolygonMultiPolygonIntersectionResult
    with MultiLineStringMultiLineStringSymDifferenceResult
    with MultiPointMultiPointSymDifferenceResult
    with MultiPolygonMultiPolygonSymDifferenceResult
    with MultiLineStringMultiLineStringDifferenceResult
    with MultiPointMultiPointDifferenceResult
    with MultiPolygonMultiPolygonDifferenceResult
    with PointOrNoResult {
  def toGeometry(): Option[Geometry] = Some(geom)
}

case class LineStringResult(geom: LineString) extends GeometryResult
    with OneDimensionAtLeastOneDimensionIntersectionResult
    with TwoDimensionsTwoDimensionsIntersectionResult
    with ZeroDimensionsLineStringUnionResult
    with LineStringOneDimensionUnionResult
    with LineStringAtLeastOneDimensionDifferenceResult
    with PointMultiLineStringUnionResult
    with PolygonBoundaryResult
    with ZeroDimensionsLineStringSymDifferenceResult
    with OneDimensionOneDimensionSymDifferenceResult
    with PointMultiLineStringSymDifferenceResult
    with LineStringMultiPolygonUnionResult
    with LineStringMultiPolygonSymDifferenceResult
    with MultiPointMultiLineStringUnionResult
    with MultiPointMultiLineStringSymDifferenceResult
    with MultiLineStringMultiLineStringUnionResult
    with MultiLineStringGeometryDifferenceResult
    with MultiLineStringMultiLineStringIntersectionResult
    with MultiPolygonMultiPolygonIntersectionResult
    with MultiLineStringMultiLineStringSymDifferenceResult
    with MultiPolygonMultiPolygonSymDifferenceResult
    with MultiLineStringMultiLineStringDifferenceResult
    with MultiPolygonMultiPolygonDifferenceResult {
  def toGeometry(): Option[Geometry] = Some(geom)
}

object LineStringResult {
  implicit def jtsToResult(geom: Geometry): LineStringResult =
    geom match {
      case ml: LineString => LineStringResult(ml)
      case _ =>
        sys.error(s"Unexpected result: ${geom.getGeometryType}")
    }
}

case class PolygonResult(geom: Polygon) extends GeometryResult
    with TwoDimensionsTwoDimensionsIntersectionResult
    with AtMostOneDimensionPolygonUnionResult
    with TwoDimensionsTwoDimensionsUnionResult
    with TwoDimensionsTwoDimensionsSeqUnionResult
    with LineStringMultiPolygonUnionResult
    with PolygonAtMostOneDimensionDifferenceResult
    with AtMostOneDimensionPolygonSymDifferenceResult
    with TwoDimensionsTwoDimensionsDifferenceResult
    with TwoDimensionsTwoDimensionsSymDifferenceResult
    with PointMultiPolygonSymDifferenceResult
    with LineStringMultiPolygonSymDifferenceResult
    with PointMultiPolygonUnionResult
    with PolygonOrNoResult
    with MultiPointMultiPolygonUnionResult
    with MultiPointMultiPolygonSymDifferenceResult
    with MultiLineStringMultiPolygonUnionResult
    with MultiLineStringMultiPolygonSymDifferenceResult
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
    with MultiPointMultiLineStringUnionResult
    with MultiPointMultiPolygonUnionResult
    with MultiPointMultiLineStringSymDifferenceResult
    with MultiPointMultiPolygonSymDifferenceResult
    with MultiLineStringMultiLineStringIntersectionResult
    with MultiPolygonMultiPolygonIntersectionResult
    with MultiLineStringMultiLineStringSymDifferenceResult
    with MultiPointMultiPointSymDifferenceResult
    with MultiPolygonMultiPolygonSymDifferenceResult
    with MultiLineStringMultiLineStringDifferenceResult
    with MultiPointMultiPointDifferenceResult
    with MultiPolygonMultiPolygonDifferenceResult {
  def toGeometry(): Option[Geometry] = Some(geom)
}

case class MultiLineStringResult(geom: MultiLineString) extends GeometryResult
    with TwoDimensionsTwoDimensionsIntersectionResult
    with LineStringOneDimensionUnionResult
    with LineStringAtLeastOneDimensionDifferenceResult
    with PointMultiLineStringUnionResult
    with MultiLineStringGeometryDifferenceResult
    with PolygonBoundaryResult
    with OneDimensionOneDimensionSymDifferenceResult
    with PointMultiLineStringSymDifferenceResult
    with OneDimensionAtLeastOneDimensionIntersectionResult
    with MultiPointMultiLineStringUnionResult
    with MultiPointMultiLineStringSymDifferenceResult
    with MultiLineStringMultiLineStringUnionResult
    with MultiLineStringMultiPolygonUnionResult
    with MultiLineStringMultiPolygonSymDifferenceResult
    with MultiLineStringMultiLineStringIntersectionResult
    with MultiPolygonMultiPolygonIntersectionResult
    with MultiLineStringMultiLineStringSymDifferenceResult
    with MultiPolygonMultiPolygonSymDifferenceResult
    with MultiLineStringMultiLineStringDifferenceResult
    with MultiPolygonMultiPolygonDifferenceResult {
  def toGeometry(): Option[Geometry] = Some(geom)
}

object MultiLineStringResult {
  implicit def jtsToResult(geom: Geometry): MultiLineStringResult =
    geom match {
      case ml: MultiLineString => MultiLineStringResult(ml)
      case _ =>
        sys.error(s"Unexpected result: ${geom.getGeometryType}")
    }
}

case class MultiPolygonResult(geom: MultiPolygon) extends GeometryResult
    with TwoDimensionsTwoDimensionsIntersectionResult
    with TwoDimensionsTwoDimensionsUnionResult
    with TwoDimensionsTwoDimensionsSeqUnionResult
    with LineStringMultiPolygonUnionResult
    with TwoDimensionsTwoDimensionsDifferenceResult
    with MultiPolygonXDifferenceResult
    with TwoDimensionsTwoDimensionsSymDifferenceResult
    with PointMultiPolygonSymDifferenceResult
    with LineStringMultiPolygonSymDifferenceResult
    with PointMultiPolygonUnionResult
    with MultiPointMultiPolygonUnionResult
    with MultiPointMultiPolygonSymDifferenceResult
    with MultiLineStringMultiPolygonUnionResult
    with MultiLineStringMultiPolygonSymDifferenceResult
    with MultiPolygonMultiPolygonIntersectionResult
    with MultiPolygonMultiPolygonSymDifferenceResult
    with MultiPolygonMultiPolygonDifferenceResult {
  def toGeometry(): Option[Geometry] = Some(geom)
}

case class GeometryCollectionResult(geom: GeometryCollection) extends GeometryResult
    with TwoDimensionsTwoDimensionsIntersectionResult
    with ZeroDimensionsLineStringUnionResult
    with AtMostOneDimensionPolygonUnionResult
    with LineStringMultiPolygonUnionResult
    with PointMultiLineStringUnionResult
    with ZeroDimensionsLineStringSymDifferenceResult
    with AtMostOneDimensionPolygonSymDifferenceResult
    with PointMultiLineStringSymDifferenceResult
    with PointMultiPolygonSymDifferenceResult
    with LineStringMultiPolygonSymDifferenceResult
    with OneDimensionAtLeastOneDimensionIntersectionResult
    with PointMultiPolygonUnionResult
    with MultiPointMultiLineStringUnionResult
    with MultiPointMultiPolygonUnionResult
    with MultiPointMultiLineStringSymDifferenceResult
    with MultiPointMultiPolygonSymDifferenceResult
    with MultiLineStringMultiPolygonUnionResult
    with MultiLineStringMultiPolygonSymDifferenceResult {
  def toGeometry(): Option[Geometry] = Some(geom)
}
