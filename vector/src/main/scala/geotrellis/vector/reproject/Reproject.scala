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

package geotrellis.vector.reproject

import geotrellis.proj4._
import geotrellis.vector._

/** This object contains various overloads for performing reprojections over geometries */
object Reproject {
  def apply(t: (Double, Double), src: CRS, dest: CRS): (Double, Double) =
    apply(t, Transform(src, dest))

  def apply(t: (Double, Double), transform: Transform): (Double, Double) =
    transform(t._1, t._2)

  def apply(p: Point, src: CRS, dest: CRS): Point =
    apply(p, Transform(src, dest))

  def apply(p: Point, transform: Transform): Point =
    Point(transform(p.x, p.y))

  // For features, name explicitly, or else type erasure kicks in.
  def pointFeature[D](pf: PointFeature[D], src: CRS, dest: CRS): PointFeature[D] =
    PointFeature[D](apply(pf.geom, src, dest), pf.data)

  def pointFeature[D](pf: PointFeature[D], transform: Transform): PointFeature[D] =
    PointFeature[D](apply(pf.geom, transform), pf.data)

  def apply(l: LineString, src: CRS, dest: CRS): LineString =
    apply(l, Transform(src, dest))

  def apply(l: LineString, transform: Transform): LineString =
    LineString(l.points.map { p => transform(p.x, p.y) })

  def lineStringFeature[D](lf: LineStringFeature[D], src: CRS, dest: CRS): LineStringFeature[D] =
    LineStringFeature(apply(lf.geom, src, dest), lf.data)

  def lineStringFeature[D](lf: LineStringFeature[D], transform: Transform): LineStringFeature[D] =
    LineStringFeature(apply(lf.geom, transform), lf.data)

  def apply(p: Polygon, src: CRS, dest: CRS): Polygon =
    apply(p, Transform(src, dest))

  def apply(p: Polygon, transform: Transform): Polygon =
    Polygon(
      apply(p.exterior, transform),
      p.holes.map{ apply(_, transform) }
    )

  /** Naively reproject an extent
    *
    * @note This method is unsafe when attempting to reproject a gridded space (as is the
    * case when dealing with rasters) and should not be used. Instead, reproject a RasterExtent
    * to ensure that underlying cells are fully and accurately captured by reprojection.
    */
  def apply(extent: Extent, src: CRS, dest: CRS): Extent =
    apply(extent.toPolygon, src, dest).extent

  def apply(extent: Extent, transform: Transform): Extent =
    apply(extent.toPolygon, transform).extent

  /** Performs adaptive refinement to produce a Polygon representation of the projected region.
    *
    * Generally, rectangular regions become curvilinear regions after geodetic projection.
    * This function creates a polygon giving an accurate representation of the post-projection
    * region.  This function does its work by recursively splitting extent edges, until a relative
    * error criterion is met.  That is,for an edge in the source CRS, endpoints, a and b, generate
    * the midpoint, m.  These are mapped to the destination CRS as a', b', and c'.  If the
    * distance from m' to the line a'-b' is greater than relErr * distance(a', b'), then m' is
    * included in the refined polygon.  The process recurses on (a, m) and (m, b) until
    * termination.
    *
    * @param transform
    * @param relError   A tolerance value telling how much deflection is allowed in terms of
    *                   distance from the original line to the new point
    */
  def reprojectExtentAsPolygon(extent: Extent, transform: Transform, relError: Double): Polygon = {
    import math.{abs, pow, sqrt}

    def refine(p0: (Point, (Double, Double)), p1: (Point, (Double, Double))): List[(Point, (Double, Double))] = {
      val ((a, (x0, y0)), (b, (x1, y1))) = (p0, p1)
      val m = Point(0.5 * (a.x + b.x), 0.5 * (a.y + b.y))
      val (x2, y2) = transform(m.x, m.y)

      val deflect = abs((y2 - y1) * x0 - (x2 - x1) * y0 + x2 * y1 - y2 * x1) / sqrt(pow(y2 - y1, 2) + pow(x2 - x1, 2))
      val length = sqrt(pow(x0 - x1, 2) + pow(y0 - y1, 2))

      val p2 = m -> (x2, y2)
      if (java.lang.Double.isNaN(deflect)) {
        throw new IllegalArgumentException(s"Encountered NaN during a refinement step: ($deflect / $length). Input $extent is likely not in source projection.")
      } else if (deflect / length < relError) {
        List(p2)
      } else {
        refine(p0, p2) ++ (p2 :: refine(p2, p1))
      }
    }

    val pts = Array(extent.southWest, extent.southEast, extent.northEast, extent.northWest)
      .map{ p => (p, transform(p.x, p.y)) }

    Polygon ( ((pts(0) :: refine(pts(0), pts(1))) ++
               (pts(1) :: refine(pts(1), pts(2))) ++
               (pts(2) :: refine(pts(2), pts(3))) ++
               (pts(3) :: refine(pts(3), pts(0))) ++
               List(pts(0))).map{ case (_, (x, y)) => Point(x, y) } )
  }

  def polygonFeature[D](pf: PolygonFeature[D], src: CRS, dest: CRS): PolygonFeature[D] =
    PolygonFeature(apply(pf.geom, src, dest), pf.data)

  def polygonFeature[D](pf: PolygonFeature[D], transform: Transform): PolygonFeature[D] =
    PolygonFeature(apply(pf.geom, transform), pf.data)

  def apply(mp: MultiPoint, src: CRS, dest: CRS): MultiPoint =
    apply(mp, Transform(src, dest))

  def apply(mp: MultiPoint, transform: Transform): MultiPoint =
    MultiPoint(mp.points.map { p => transform(p.x, p.y) })

  def multiPointFeature[D](mpf: MultiPointFeature[D], src: CRS, dest: CRS): MultiPointFeature[D] =
    MultiPointFeature(apply(mpf.geom, src, dest), mpf.data)

  def multiPointFeature[D](mpf: MultiPointFeature[D], transform: Transform): MultiPointFeature[D] =
    MultiPointFeature(apply(mpf.geom, transform), mpf.data)

  def apply(ml: MultiLineString, src: CRS, dest: CRS): MultiLineString =
    apply(ml, Transform(src, dest))

  def apply(ml: MultiLineString, transform: Transform): MultiLineString =
    MultiLineString(ml.lines.map(apply(_, transform)))

  def multiLineStringFeature[D](mlf: MultiLineStringFeature[D], src: CRS, dest: CRS): MultiLineStringFeature[D] =
    MultiLineStringFeature(apply(mlf, src, dest), mlf.data)

  def multiLineStringFeature[D](mlf: MultiLineStringFeature[D], transform: Transform): MultiLineStringFeature[D] =
    MultiLineStringFeature(apply(mlf, transform), mlf.data)

  def apply(mp: MultiPolygon, src: CRS, dest: CRS): MultiPolygon =
    apply(mp, Transform(src, dest))

  def apply(mp: MultiPolygon, transform: Transform): MultiPolygon =
    MultiPolygon(mp.polygons.map(apply(_, transform)))

  def multiPolygonFeature[D](mpf: MultiPolygonFeature[D], src: CRS, dest: CRS): MultiPolygonFeature[D] =
    MultiPolygonFeature(apply(mpf.geom, src, dest), mpf.data)

  def multiPolygonFeature[D](mpf: MultiPolygonFeature[D], transform: Transform): MultiPolygonFeature[D] =
    MultiPolygonFeature(apply(mpf.geom, transform: Transform), mpf.data)

  def apply(gc: GeometryCollection, src: CRS, dest: CRS): GeometryCollection =
    GeometryCollection(
      gc.getAll[Point].map{ apply(_, src, dest) },
      gc.getAll[LineString].map{ apply(_, src, dest) },
      gc.getAll[Polygon].map{ apply(_, src, dest) },
      gc.getAll[MultiPoint].map{ apply(_, src, dest) },
      gc.getAll[MultiLineString].map{ apply(_, src, dest) },
      gc.getAll[MultiPolygon].map{ apply(_, src, dest) },
      gc.getAll[GeometryCollection].map{ apply(_, src, dest) }
    )

  def apply(gc: GeometryCollection, transform: Transform): GeometryCollection =
    GeometryCollection(
      gc.getAll[Point].map{ apply(_, transform) },
      gc.getAll[LineString].map{ apply(_, transform) },
      gc.getAll[Polygon].map{ apply(_, transform) },
      gc.getAll[MultiPoint].map{ apply(_, transform) },
      gc.getAll[MultiLineString].map{ apply(_, transform) },
      gc.getAll[MultiPolygon].map{ apply(_, transform) },
      gc.getAll[GeometryCollection].map{ apply(_, transform) }
    )


  def geometryCollectionFeature[D](gcf: GeometryCollectionFeature[D], src: CRS, dest: CRS): GeometryCollectionFeature[D] =
    GeometryCollectionFeature(apply(gcf.geom, src, dest), gcf.data)

  def geometryCollectionFeature[D](gcf: GeometryCollectionFeature[D], transform: Transform): GeometryCollectionFeature[D] =
    GeometryCollectionFeature(apply(gcf.geom, transform), gcf.data)

  def apply(g: Geometry, src: CRS, dest: CRS): Geometry =
    apply(g, Transform(src, dest))

  def apply(g: Geometry, transform: Transform): Geometry =
    g match {
      case p: Point => apply(p, transform)
      case l: LineString => apply(l, transform)
      case p: Polygon => apply(p, transform)
      case mp: MultiPoint => apply(mp, transform)
      case ml: MultiLineString => apply(ml, transform)
      case mp: MultiPolygon => apply(mp, transform)
      case gc: GeometryCollection => apply(gc, transform)
    }

  def geometryFeature[D](f: Feature[Geometry, D], src: CRS, dest: CRS): Feature[Geometry, D] =
    geometryFeature(f, Transform(src, dest))

  def geometryFeature[D](f: Feature[Geometry, D], transform: Transform): Feature[Geometry, D] =
    f.geom match {
      case p: Point => pointFeature(Feature(p, f.data), transform)
      case l: LineString => lineStringFeature(Feature(l, f.data), transform)
      case p: Polygon => polygonFeature(Feature(p, f.data), transform)
      case mp: MultiPoint => multiPointFeature(Feature(mp, f.data), transform)
      case ml: MultiLineString => multiLineStringFeature(Feature(ml, f.data), transform)
      case mp: MultiPolygon => multiPolygonFeature(Feature(mp, f.data), transform)
      case gc: GeometryCollection => geometryCollectionFeature(Feature(gc, f.data), transform)
    }
}
