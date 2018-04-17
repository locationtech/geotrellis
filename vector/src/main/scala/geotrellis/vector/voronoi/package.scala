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

import geotrellis.vector._

import org.locationtech.jts.{ geom => jts }


package object voronoi {

  implicit def linesToCollection(lines: Seq[Line]): jts.GeometryCollection  =
    new jts.GeometryCollection(lines.map({ line => line.jtsGeom }).toArray, GeomFactory.factory)

  implicit def linesToCollection(lines: Array[Line]): jts.GeometryCollection  =
    new jts.GeometryCollection(lines.map({ line => line.jtsGeom }), GeomFactory.factory)

  implicit def polygonsToCollection(polygons: Seq[Polygon]): jts.GeometryCollection =
    new jts.GeometryCollection(polygons.map({ poly => poly.jtsGeom }).toArray, GeomFactory.factory)

  implicit def polygonsToCollection(polygons: Array[Polygon]): jts.GeometryCollection =
    new jts.GeometryCollection(polygons.map({ poly => poly.jtsGeom }), GeomFactory.factory)
}
