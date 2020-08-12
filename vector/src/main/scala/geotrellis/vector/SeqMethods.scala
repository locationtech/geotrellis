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

import scala.collection.JavaConverters._

import org.locationtech.jts.operation.union.CascadedPolygonUnion

trait SeqMethods {

  implicit class SeqLineStringExtensions(val lines: Traversable[LineString]) {

    val ml: MultiLineString = MultiLineString(lines)

    def unionGeometries = ml.union
    def intersectionGeometries: MultiLineStringMultiLineStringIntersectionResult =
    lines.reduce[Geometry] {
      _.intersection(_)
    }
    def differenceGeometries: MultiLineStringMultiLineStringDifferenceResult =
    lines.reduce[Geometry] {
      _.difference(_)
    }
    def symDifferenceGeometries: MultiLineStringMultiLineStringSymDifferenceResult =
    lines.reduce[Geometry] {
      _.symDifference(_)
    }

    def toMultiLineString = ml

    def extent: Extent = ml.extent
  }

  implicit class SeqPointExtensions(val points: Traversable[Point]) {

    val mp: MultiPoint = MultiPoint(points)

    def unionGeometries() = mp.union
    def intersectionGeometries() =
      points.reduce[Geometry] {
        _.intersection(_)
      }
    def differenceGeometries() =
      points.reduce[Geometry] {
        _.difference(_)
      }
    def symDifferenceGeometries() =
      points.reduce[Geometry] {
        _.symDifference(_)
      }

    def toMultiPoint = mp

    def extent: Extent = mp.extent
  }

  implicit class SeqPolygonExtensions(val polygons: Traversable[Polygon]) {

    val mp: MultiPolygon = MultiPolygon(polygons)

    def unionGeometries(): TwoDimensionsTwoDimensionsSeqUnionResult =
      if(polygons.isEmpty) NoResult
      else new CascadedPolygonUnion(polygons.toSeq.asJava).union()

    def intersectionGeometries() =
      polygons.reduce[Geometry] {
        _.intersection(_)
      }
    def differenceGeometries() =
      polygons.reduce[Geometry] {
        _.difference(_)
      }
    def symDifferenceGeometries() =
      polygons.reduce[Geometry] {
        _.symDifference(_)
      }

    def toMultiPolygon() = mp

    def extent: Extent = mp.extent
  }

  implicit class SeqMultiLineStringExtensions(val multilines: Traversable[MultiLineString]) {

    private val seq = multilines.map(_.lines).flatten
    val ml: MultiLineString = MultiLineString(seq)

    def unionGeometries() = ml.union
    def intersectionGeometries() = seq.intersectionGeometries
    def differenceGeometries() = seq.differenceGeometries
    def symDifferenceGeometries() = seq.symDifferenceGeometries

    def extent: Extent = ml.extent
  }

  implicit class SeqMultiPointExtensions(val multipoints: Traversable[MultiPoint]) {

    private val seq = multipoints.map(_.points).flatten
    val mp: MultiPoint = MultiPoint(seq)

    def unionGeometries() = mp.union
    def intersectionGeometries() = seq.intersectionGeometries
    def differenceGeometries() = seq.differenceGeometries
    def symDifferenceGeometries() = seq.symDifferenceGeometries

    def extent: Extent = mp.extent
  }

  implicit class SeqMultiPolygonExtensions(val multipolygons: Traversable[MultiPolygon]) {

    private val seq = multipolygons.map(_.polygons).flatten
    val mp: MultiPolygon = MultiPolygon(seq)

    def unionGeometries(): TwoDimensionsTwoDimensionsSeqUnionResult =
      if(multipolygons.isEmpty) NoResult
      else new CascadedPolygonUnion(mp.polygons.toSeq.asJava).union

    def intersectionGeometries() = seq.intersectionGeometries
    def differenceGeometries() = seq.differenceGeometries
    def symDifferenceGeometries() = seq.symDifferenceGeometries

    def extent: Extent = mp.extent
  }
}
