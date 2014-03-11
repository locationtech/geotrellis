/***
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
 ***/

package geotrellis

import com.vividsolutions.jts.{geom => jts}

import scala.collection.mutable

package object feature {

  implicit def tupleToPoint(t: (Double, Double)): Point =
    Point(t._1,t._2)

  implicit def coordinateToPoint(c: jts.Coordinate): Point =
    Point(c.x,c.y)

  implicit def tupleSetToMultiPoint(ts: Set[(Double, Double)]): Set[Point] =
    ts map(t => Point(t._1, t._2))

  implicit def tupleListToPointList(tl: List[(Double, Double)]): List[Point] =
    tl map(t => Point(t._1, t._2))

  implicit def coordinateArrayToMultiPoint(ca: Array[jts.Coordinate]): MultiPoint = {
    val ps = (for (i <- 0 until ca.length) yield {
      Point(ca(i).x, ca(i).y)
    }).toSet
    MultiPoint(ps)
  }

  implicit def pointListToCoordinateArray(ps: List[Point]): Array[jts.Coordinate] =
    ps map(p => new jts.Coordinate(p.x, p.y)) toArray

  implicit def multiPointToSetPoint(mp: jts.MultiPoint): Set[Point] = {
    val len = mp.getNumGeometries
      (for (i <- 0 until len) yield {
        Point(mp.getGeometryN(i).asInstanceOf[jts.Point])
      }).toSet
  }

  implicit def multiLineToSetLine(ml: jts.MultiLineString): Set[Line] = {
    val len = ml.getNumGeometries
    (for (i <- 0 until len) yield {
      Line(ml.getGeometryN(i).asInstanceOf[jts.LineString])
    }).toSet
  }

  implicit def multiPolygonToSetPolygon(mp: jts.MultiPolygon): Set[Polygon] = {
    val len = mp.getNumGeometries
    (for (i <- 0 until len) yield {
      Polygon(mp.getGeometryN(i).asInstanceOf[jts.Polygon])
    }).toSet
  }

  implicit def geometryCollectionToSetGeometry(gc: jts.GeometryCollection): Set[Geometry] = {
    val len = gc.getNumGeometries
    (for (i <- 0 until len) yield {
      gc.getGeometryN(i) match {
        case p: jts.Point => Set[Geometry](Point(p))
        case mp: jts.MultiPoint => multiPointToSetPoint(mp)
        case l: jts.LineString => Set[Geometry](Line(l))
        case ml: jts.MultiLineString => multiLineToSetLine(ml)
        case p: jts.Polygon => Set[Geometry](Polygon(p))
        case mp: jts.MultiPolygon => multiPolygonToSetPolygon(mp)
        case gc: jts.GeometryCollection => geometryCollectionToSetGeometry(gc)
      }
    }).toSet.flatten
  }

  implicit def seqPointToMultiPoint(ps: Set[Point]): MultiPoint = MultiPoint(ps)

  implicit def seqLineToMultiLine(ps: Set[Line]): MultiLine = MultiLine(ps)

  implicit def seqPolygonToMultiPolygon(ps: Set[Polygon]): MultiPolygon = MultiPolygon(ps)

  implicit def seqGeometryToGeometryCollection(gs: Set[Geometry]): GeometryCollection = {
    val points = mutable.Set[Point]()
    val lines = mutable.Set[Line]()
    val polygons = mutable.Set[Polygon]()

    for(g <- gs) {
      g match {
        case p: Point => points += p
        case l: Line => lines += l
        case p: Polygon => polygons += p
        case _ => sys.error(s"Unknown Geometry type: $g")
      }
    }
    GeometryCollection(points.toSet, lines.toSet, polygons.toSet)
  }
}
