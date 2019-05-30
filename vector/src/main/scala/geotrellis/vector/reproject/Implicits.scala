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
import org.locationtech.jts.geom._

object Implicits extends Implicits

trait Implicits {

  implicit class ReprojectTuple(t: (Double, Double)) {
    def reproject(src: CRS, dest: CRS): (Double, Double) = Reproject(t, src, dest)
    def reproject(transform: Transform): (Double, Double) = Reproject(t, transform)
  }

  implicit class ReprojectPoint(p: Point) {
    def reproject(src: CRS, dest: CRS): Point = Reproject(p, src, dest)
    def reproject(transform: Transform): Point = Reproject(p, transform)
  }

  implicit class ReprojectPointFeature[D](pf: PointFeature[D]) {
    def reproject(src: CRS, dest: CRS): PointFeature[D] = Reproject.pointFeature(pf, src, dest)
    def reproject(transform: Transform): PointFeature[D] = Reproject.pointFeature(pf, transform)
  }

  implicit class ReprojectLineString(l: LineString) {
    def reproject(src: CRS, dest: CRS): LineString = Reproject(l, src, dest)
    def reproject(transform: Transform): LineString = Reproject(l, transform)
  }

  implicit class ReprojectLineStringFeature[D](lf: LineStringFeature[D]) {
    def reproject(src: CRS, dest: CRS): LineStringFeature[D] = Reproject.lineStringFeature(lf, src, dest)
    def reproject(transform: Transform): LineStringFeature[D] = Reproject.lineStringFeature(lf, transform)
  }

  implicit class ReprojectPolygon(p: Polygon) {
    def reproject(src: CRS, dest: CRS): Polygon = Reproject(p, src, dest)
    def reproject(transform: Transform): Polygon = Reproject(p, transform)
  }

  implicit class ReprojectExtent(e: Extent) {
    def reproject(src: CRS, dest: CRS): Extent = Reproject(e, src, dest)
    def reproject(transform: Transform): Extent = Reproject(e, transform)
    def reprojectAsPolygon(src: CRS, dest: CRS, relErr: Double): Polygon = e.reprojectAsPolygon(Transform(src, dest), relErr)
    def reprojectAsPolygon(transform: Transform, relErr: Double): Polygon = Reproject.reprojectExtentAsPolygon(e, transform, relErr)
  }

  implicit class ReprojectPolygonFeature[D](pf: PolygonFeature[D]) {
    def reproject(src: CRS, dest: CRS): PolygonFeature[D] = Reproject.polygonFeature(pf, src, dest)
    def reproject(transform: Transform): PolygonFeature[D] = Reproject.polygonFeature(pf, transform)
  }

  implicit class ReprojectMultiPoint(mp: MultiPoint) {
    def reproject(src: CRS, dest: CRS): MultiPoint = Reproject(mp, src, dest)
    def reproject(transform: Transform): MultiPoint = Reproject(mp, transform)
  }

  implicit class ReprojectMultiPointFeature[D](mpf: MultiPointFeature[D]) {
    def reproject(src: CRS, dest: CRS): MultiPointFeature[D] = Reproject.multiPointFeature(mpf, src, dest)
    def reproject(transform: Transform): MultiPointFeature[D] = Reproject.multiPointFeature(mpf, transform)
  }

  implicit class ReprojectMutliLineString(ml: MultiLineString) {
    def reproject(src: CRS, dest: CRS): MultiLineString = Reproject(ml, src, dest)
    def reproject(transform: Transform): MultiLineString = Reproject(ml, transform)
  }

  implicit class ReprojectMutliLineStringFeature[D](mlf: MultiLineStringFeature[D]) {
    def reproject(src: CRS, dest: CRS): MultiLineStringFeature[D] = Reproject.multiLineStringFeature(mlf, src, dest)
    def reproject(transform: Transform): MultiLineStringFeature[D] = Reproject.multiLineStringFeature(mlf, transform)
  }

  implicit class ReprojectMutliPolygon(mp: MultiPolygon) {
    def reproject(src: CRS, dest: CRS): MultiPolygon = Reproject(mp, src, dest)
    def reproject(transform: Transform): MultiPolygon = Reproject(mp, transform)
  }

  implicit class ReprojectMutliPolygonFeature[D](mpf: MultiPolygonFeature[D]) {
    def reproject(src: CRS, dest: CRS): MultiPolygonFeature[D] = Reproject.multiPolygonFeature(mpf, src, dest)
    def reproject(transform: Transform): MultiPolygonFeature[D] = Reproject.multiPolygonFeature(mpf, transform)
  }

  implicit class ReprojectGeometryCollection(gc: GeometryCollection) {
    def reproject(src: CRS, dest: CRS): GeometryCollection = Reproject(gc, src, dest)
    def reproject(transform: Transform): GeometryCollection = Reproject(gc, transform)
  }

  implicit class ReprojectGeometryCollectionFeature[D](gcf: GeometryCollectionFeature[D]) {
    def reproject(src: CRS, dest: CRS): GeometryCollectionFeature[D] = Reproject.geometryCollectionFeature(gcf, src, dest)
    def reproject(transform: Transform): GeometryCollectionFeature[D] = Reproject.geometryCollectionFeature(gcf, transform)
  }

  implicit class ReprojectGeometry(g: Geometry) {
    def reproject(src: CRS, dest: CRS): Geometry = Reproject(g, src, dest)
    def reproject(transform: Transform): Geometry = Reproject(g, transform)
  }

  implicit class ReprojectFeature[D](f: Feature[Geometry, D]) {
    def reproject(src: CRS, dest: CRS): Feature[Geometry, D] = Reproject.geometryFeature(f, src, dest)
    def reproject(transform: Transform): Feature[Geometry, D] = Reproject.geometryFeature(f, transform)
  }
}
