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

import geotrellis.vector.GeomFactory._

import org.locationtech.jts.{geom => jts}


trait GeometryCollectionConstructors {
  def apply(points: Seq[jts.Point] = Seq(), lines: Seq[jts.LineString] = Seq(), polygons: Seq[jts.Polygon] = Seq(),
             multiPoints: Seq[jts.MultiPoint] = Seq(),
             multiLines: Seq[jts.MultiLineString] = Seq(),
             multiPolygons: Seq[jts.MultiPolygon] = Seq(),
             geometryCollections: Seq[jts.GeometryCollection] = Seq()
           ): jts.GeometryCollection =
    apply(points ++ lines ++ polygons ++ multiPoints ++ multiLines ++ multiPolygons ++ geometryCollections)

  def apply(geoms: Traversable[jts.Geometry]): jts.GeometryCollection =
    factory.createGeometryCollection(geoms.toArray)

  def unapply(gc: jts.GeometryCollection):
      Some[(Seq[jts.Point], Seq[jts.LineString], Seq[jts.Polygon],
            Seq[jts.MultiPoint], Seq[jts.MultiLineString], Seq[jts.MultiPolygon],
            Seq[jts.GeometryCollection])] =
    Some((gc.getAll[jts.Point], gc.getAll[jts.LineString], gc.getAll[jts.Polygon],
          gc.getAll[jts.MultiPoint], gc.getAll[jts.MultiLineString], gc.getAll[jts.MultiPolygon],
          gc.getAll[jts.GeometryCollection]))
}

/** Companion object to [[GeometryCollection]] */
object GeometryCollection extends GeometryCollectionConstructors
