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
    transform(p.x, p.y)

  // For features, name explicitly, or else type erasure kicks in.
  def pointFeature[D](pf: PointFeature[D], src: CRS, dest: CRS): PointFeature[D] =
    PointFeature[D](apply(pf.geom, src, dest), pf.data)

  def pointFeature[D](pf: PointFeature[D], transform: Transform): PointFeature[D] =
    PointFeature[D](apply(pf.geom, transform), pf.data)

  def apply(l: Line, src: CRS, dest: CRS): Line =
    apply(l, Transform(src, dest))

  def apply(l: Line, transform: Transform): Line =
    Line(l.points.map { p => transform(p.x, p.y) })

  def lineFeature[D](lf: LineFeature[D], src: CRS, dest: CRS): LineFeature[D] =
    LineFeature(apply(lf.geom, src, dest), lf.data)

  def lineFeature[D](lf: LineFeature[D], transform: Transform): LineFeature[D] =
    LineFeature(apply(lf.geom, transform), lf.data)

  def apply(p: Polygon, src: CRS, dest: CRS): Polygon =
    apply(p, Transform(src, dest))

  def apply(p: Polygon, transform: Transform): Polygon =
    Polygon(
      apply(p.exterior, transform),
      p.holes.map{ apply(_, transform) }
    )

  def apply(extent: Extent, src: CRS, dest: CRS): Extent =
    apply(extent.toPolygon, src, dest).envelope

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

  def apply(ml: MultiLine, src: CRS, dest: CRS): MultiLine =
    apply(ml, Transform(src, dest))

  def apply(ml: MultiLine, transform: Transform): MultiLine =
    MultiLine(ml.lines.map(apply(_, transform)))

  def multiLineFeature[D](mlf: MultiLineFeature[D], src: CRS, dest: CRS): MultiLineFeature[D] =
    MultiLineFeature(apply(mlf, src, dest), mlf.data)

  def multiLineFeature[D](mlf: MultiLineFeature[D], transform: Transform): MultiLineFeature[D] =
    MultiLineFeature(apply(mlf, transform), mlf.data)

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
      gc.points.map{ apply(_, src, dest) },
      gc.lines.map{ apply(_, src, dest) },
      gc.polygons.map{ apply(_, src, dest) },
      gc.multiPoints.map{ apply(_, src, dest) },
      gc.multiLines.map{ apply(_, src, dest) },
      gc.multiPolygons.map{ apply(_, src, dest) },
      gc.geometryCollections.map{ apply(_, src, dest) }
    )

  def apply(gc: GeometryCollection, transform: Transform): GeometryCollection =
    GeometryCollection(
      gc.points.map{ apply(_, transform) },
      gc.lines.map{ apply(_, transform) },
      gc.polygons.map{ apply(_, transform) },
      gc.multiPoints.map{ apply(_, transform) },
      gc.multiLines.map{ apply(_, transform) },
      gc.multiPolygons.map{ apply(_, transform) },
      gc.geometryCollections.map{ apply(_, transform) }
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
      case l: Line => apply(l, transform)
      case p: Polygon => apply(p, transform)
      case e: Extent => apply(e, transform)
      case mp: MultiPoint => apply(mp, transform)
      case ml: MultiLine => apply(ml, transform)
      case mp: MultiPolygon => apply(mp, transform)
      case gc: GeometryCollection => apply(gc, transform)
    }

  def geometryFeature[D](f: Feature[Geometry, D], src: CRS, dest: CRS): Feature[Geometry, D] =
    geometryFeature(f, Transform(src, dest))

  def geometryFeature[D](f: Feature[Geometry, D], transform: Transform): Feature[Geometry, D] =
    f.geom match {
      case p: Point => pointFeature(Feature(p, f.data), transform)
      case l: Line => lineFeature(Feature(l, f.data), transform)
      case p: Polygon => polygonFeature(Feature(p, f.data), transform)
      case mp: MultiPoint => multiPointFeature(Feature(mp, f.data), transform)
      case ml: MultiLine => multiLineFeature(Feature(ml, f.data), transform)
      case mp: MultiPolygon => multiPolygonFeature(Feature(mp, f.data), transform)
      case gc: GeometryCollection => geometryCollectionFeature(Feature(gc, f.data), transform)
    }
}
