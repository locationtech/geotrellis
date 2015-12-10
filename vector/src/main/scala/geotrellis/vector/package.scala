/*
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
 */

package geotrellis

import com.vividsolutions.jts.{geom => jts}

import scala.collection.mutable
import scala.collection.JavaConversions._

package object vector extends SeqMethods {

  type PointFeature[D] = Feature[Point, D]
  type LineFeature[D] = Feature[Line, D]
  type PolygonFeature[D] = Feature[Polygon, D]
  type ExtentFeature[D] = Feature[Extent, D]
  type MultiPointFeature[D] = Feature[MultiPoint, D]
  type MultiLineFeature[D] = Feature[MultiLine, D]
  type MultiPolygonFeature[D] = Feature[MultiPolygon, D]
  type GeometryCollectionFeature[D] = Feature[GeometryCollection, D]


  implicit def tupleOfIntToPoint(t: (Double, Double)): Point =
    Point(t._1,t._2)

  implicit def tupleOfDoubleToPoint(t: (Int, Int)): Point =
    Point(t._1,t._2)

  implicit def coordinateToPoint(c: jts.Coordinate): Point =
    Point(c.x,c.y)

  implicit def tupleSeqToMultiPoint(ts: Set[(Double, Double)]): Set[Point] =
    ts map(t => Point(t._1, t._2))

  implicit def tupleListToPointList(tl: List[(Double, Double)]): List[Point] =
    tl map(t => Point(t._1, t._2))

  implicit def tupleListToPointList(tl: Seq[(Double, Double)]): Seq[Point] =
    tl map(t => Point(t._1, t._2))

  implicit def coordinateArrayToMultiPoint(ca: Array[jts.Coordinate]): MultiPoint = {
    val ps = (for (i <- 0 until ca.length) yield {
      Point(ca(i).x, ca(i).y)
    }).toSeq
    MultiPoint(ps)
  }

  implicit def pointListToCoordinateArray(ps: List[Point]): Array[jts.Coordinate] =
    ps map(p => new jts.Coordinate(p.x, p.y)) toArray

  implicit def multiPointToSeqPoint(mp: jts.MultiPoint): Seq[Point] = {
    val len = mp.getNumGeometries
      (for (i <- 0 until len) yield {
        Point(mp.getGeometryN(i).asInstanceOf[jts.Point])
      }).toSeq
  }

  implicit def multiLineToSeqLine(ml: jts.MultiLineString): Seq[Line] = {
    val len = ml.getNumGeometries
    (for (i <- 0 until len) yield {
      Line(ml.getGeometryN(i).asInstanceOf[jts.LineString])
    }).toSeq
  }

  implicit def multiPolygonToSeqPolygon(mp: jts.MultiPolygon): Seq[Polygon] = {
    val len = mp.getNumGeometries
    (for (i <- 0 until len) yield {
      Polygon(mp.getGeometryN(i).asInstanceOf[jts.Polygon])
    }).toSeq
  }

  implicit def seqPointToMultiPoint(ps: Seq[Point]): MultiPoint = MultiPoint(ps)
  implicit def arrayPointToMultiPoint(ps: Array[Point]): MultiPoint = MultiPoint(ps)

  implicit def seqLineToMultiLine(ps: Seq[Line]): MultiLine = MultiLine(ps)
  implicit def arrayLineToMultiLine(ps: Array[Line]): MultiLine = MultiLine(ps)

  implicit def seqPolygonToMultiPolygon(ps: Seq[Polygon]): MultiPolygon = MultiPolygon(ps)
  implicit def arrayPolygonToMultiPolygon(ps: Array[Polygon]): MultiPolygon = MultiPolygon(ps)

  implicit def seqGeometryToGeometryCollection(gs: Seq[Geometry]): GeometryCollection =
    GeometryCollection(gs)
}
