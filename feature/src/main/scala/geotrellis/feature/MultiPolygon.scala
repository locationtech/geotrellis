/**************************************************************************
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
 **************************************************************************/

package geotrellis.feature

import GeomFactory._

import com.vividsolutions.jts.{geom => jts}

object MultiPolygon {
  def apply(ps: Polygon*): MultiPolygon = 
    apply(ps)

  def apply(ps: Traversable[Polygon]): MultiPolygon =
    MultiPolygon(factory.createMultiPolygon(ps.map(_.jtsGeom).toArray))
}

case class MultiPolygon(jtsGeom: jts.MultiPolygon) extends MultiGeometry 
                                                   with Relatable
                                                   with TwoDimensions {

  lazy val area: Double =
    jtsGeom.getArea

  lazy val boundary: MultiLineResult =
    jtsGeom.getBoundary

  // -- Intersection

  def &(p: Point): PointOrNoResult =
    intersection(p)
  def intersection(p: Point): PointOrNoResult =
    p.intersection(this)

  def &(l: Line): MultiLineIntersectionResult =
    intersection(l)
  def intersection(l: Line): MultiLineIntersectionResult =
    l.intersection(this)

  def &(p: Polygon): MultiPolygonIntersectionResult =
    intersection(p)
  def intersection(p: Polygon): MultiPolygonIntersectionResult =
    p.intersection(this)

  def &(ls: MultiLine): MultiLineIntersectionResult =
    intersection(ls)
  def intersection(ls: MultiLine): MultiLineIntersectionResult =
    ls.intersection(this)

  def &(ps: MultiPolygon): MultiPolygonIntersectionResult =
    intersection(ps)
  def intersection(ps: MultiPolygon): MultiPolygonIntersectionResult =
    jtsGeom.intersection(ps.jtsGeom)

  // -- Union

  def |(p: Point): AtMostOneDimensionMultiPolygonUnionResult =
    union(p)
  def union(p: Point): AtMostOneDimensionMultiPolygonUnionResult =
    p.union(this)

  def |(l: Line): AtMostOneDimensionMultiPolygonUnionResult =
    union(l)
  def union(l: Line): AtMostOneDimensionMultiPolygonUnionResult =
    l.union(this)

  def |(p: Polygon): PolygonPolygonUnionResult =
    union(p)
  def union(p: Polygon): PolygonPolygonUnionResult =
    p.union(this)

  def |(ps: MultiPoint): AtMostOneDimensionMultiPolygonUnionResult =
    union(ps)
  def union(ps: MultiPoint): AtMostOneDimensionMultiPolygonUnionResult =
    ps.union(this)

  def |(ls: MultiLine) = union(ls)
  def union(ls: MultiLine): AtMostOneDimensionMultiPolygonUnionResult =
    ls.union(this)

  def |(ps: MultiPolygon): PolygonPolygonUnionResult =
    union(ps)
  def union(ps: MultiPolygon): PolygonPolygonUnionResult =
    jtsGeom.union(ps.jtsGeom)

  // -- Difference

  def -(p: Point): MultiPolygonXDifferenceResult =
    difference(p)
  def difference(p: Point): MultiPolygonXDifferenceResult =
    jtsGeom.difference(p.jtsGeom)

  def -(l: Line): MultiPolygonXDifferenceResult =
    difference(l)
  def difference(l: Line): MultiPolygonXDifferenceResult =
    jtsGeom.difference(l.jtsGeom)

  def -(p: Polygon): PolygonPolygonDifferenceResult =
    difference(p)
  def difference(p: Polygon): PolygonPolygonDifferenceResult =
    jtsGeom.difference(p.jtsGeom)

  def -(ps: MultiPoint): MultiPolygonXDifferenceResult =
    difference(ps)
  def difference(ps: MultiPoint): MultiPolygonXDifferenceResult =
    jtsGeom.difference(ps.jtsGeom)

  def -(ls: MultiLine): MultiPolygonXDifferenceResult =
    difference(ls)
  def difference(ls: MultiLine): MultiPolygonXDifferenceResult =
    jtsGeom.difference(ls.jtsGeom)

  def -(ps: MultiPolygon): PolygonPolygonDifferenceResult =
    difference(ps)
  def difference(ps: MultiPolygon): PolygonPolygonDifferenceResult =
    jtsGeom.difference(ps.jtsGeom)

  // -- SymDifference

  def symDifference(g: ZeroDimensions): ZeroDimensionsMultiPolygonSymDifferenceResult =
    jtsGeom.symDifference(g.jtsGeom)

  def symDifference(g: OneDimension): OneDimensionMultiPolygonSymDifferenceResult =
    jtsGeom.symDifference(g.jtsGeom)

  def symDifference(g: TwoDimensions): TwoDimensionsSymDifferenceResult =
    jtsGeom.symDifference(g.jtsGeom)

  // -- Predicates

  def contains(g: Geometry): Boolean =
    jtsGeom.contains(g.jtsGeom)

  def coveredBy(g: TwoDimensions): Boolean =
    jtsGeom.coveredBy(g.jtsGeom)

  def covers(g: Geometry): Boolean =
    jtsGeom.covers(g.jtsGeom)

  def crosses(g: OneDimension): Boolean =
    jtsGeom.crosses(g.jtsGeom)

  def crosses(ps: MultiPoint): Boolean =
    jtsGeom.crosses(ps.jtsGeom)

  def overlaps(g: TwoDimensions): Boolean =
    jtsGeom.crosses(g.jtsGeom)

  def touches(g: AtLeastOneDimension): Boolean =
    jtsGeom.touches(g.jtsGeom)

  def within(g: TwoDimensions): Boolean =
    jtsGeom.within(g.jtsGeom)

}
