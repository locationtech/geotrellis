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

import GeomFactory._

import org.locationtech.jts.{geom => jts}
import scala.collection.mutable

/** Builder for GeometryCollection.
  * @note This builder can accumulate from both geotrellis geometries and JTS geometries
  */
class GeometryCollectionBuilder extends Serializable {
  val points = mutable.ListBuffer[Point]()
  val lines = mutable.ListBuffer[Line]()
  val polygons = mutable.ListBuffer[Polygon]()
  val multiPoints = mutable.ListBuffer[MultiPoint]()
  val multiLines = mutable.ListBuffer[MultiLine]()
  val multiPolygons = mutable.ListBuffer[MultiPolygon]()
  val collections = mutable.ListBuffer[GeometryCollection]()

  def add(geom: Geometry) = geom match {
    case p: Point => points += p
    case mp: MultiPoint => multiPoints += mp
    case l: Line => lines += l
    case ml: MultiLine => multiLines += ml
    case p: Polygon => polygons += p
    case e: Extent => polygons += e.toPolygon
    case mp: MultiPolygon => multiPolygons += mp
    case gc: GeometryCollection => collections += gc
    case _ => throw new MatchError(s"Unexpected geometry of type ${geom.getClass.getName}: $geom")
  }
  def +=(geom: Geometry) = add(geom)

  def addAll(geoms: Traversable[Geometry]) =
    geoms.foreach(g=> add(g))

  def ++=(geoms: Traversable[Geometry]) =
    addAll(geoms)

  def add(geom: jts.Geometry) =
    geom match {
      //implicit conversions are happening here
      case p: jts.Point => points += p
      case mp: jts.MultiPoint => multiPoints += mp
      case l: jts.LineString => lines += l
      case ml: jts.MultiLineString => multiLines += ml
      case p: jts.Polygon => polygons += p
      case mp: jts.MultiPolygon => multiPolygons += mp
      case gc: jts.GeometryCollection => collections += gc
      case _ => throw new MatchError(s"Unexpected geometry of type ${geom.getClass.getName}: $geom")
    }
  def +=(geom: jts.Geometry) = add(geom)

  def addAll(geoms: Traversable[jts.Geometry])(implicit d: DummyImplicit) =
    geoms.foreach(g=> add(g))
  def ++=(geoms: Traversable[jts.Geometry])(implicit d: DummyImplicit) =
    addAll(geoms)

  def result(): GeometryCollection = {
    val jtsGeom = factory.createGeometryCollection(
      (
        points ++ lines ++ polygons ++
        multiPoints ++ multiLines ++ multiPolygons ++
        collections
      ).map(_.jtsGeom).toArray
    )

    new GeometryCollection(points, lines, polygons,
      multiPoints, multiLines, multiPolygons,
      collections, jtsGeom)
  }
}
