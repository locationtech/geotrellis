/*
 * Copyright 2018 Azavea
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

package geotrellis.vector.mesh

import com.vividsolutions.jts.geom.Coordinate
import geotrellis.util.Constants.{DOUBLE_EPSILON => EPSILON}
import geotrellis.vector.Point

/**
 * Provides an interface to a collection of Coordinates that have integer
 * indices.  The wrapped collection may be sparsely indexed.
 */
trait IndexedPointSet extends Serializable {
  /**
   * Returns the number of points in the collection.
   */
  def length: Int


  def getX(i: Int): Double
  def getY(i: Int): Double
  def getZ(i: Int): Double

  /**
   * Returns the coordinate at index `i`.
   */
  def getCoordinate(i: Int): Coordinate = new Coordinate(getX(i), getY(i), getZ(i))
  def getPoint(i: Int): Point = Point.jtsCoord2Point(getCoordinate(i))
  def apply(i: Int): Coordinate = getCoordinate(i)
  def distance(i1: Int, i2: Int): Double = {
    val dx = getX(i1) - getX(i2)
    val dy = getY(i1) - getY(i2)

    math.sqrt((dx * dx) + (dy * dy))
  }
}

/**
 * A more specific version of IndexedPointSet guaranteeing that every index from
 * 0 to length-1 has an associated Coordinate.
 */
trait CompleteIndexedPointSet extends IndexedPointSet

object IndexedPointSet {

  def apply(points: Array[Coordinate]): CompleteIndexedPointSet =
    new CompleteIndexedPointSet {
      def length = points.length
      def getX(i: Int) = points(i).x
      def getY(i: Int) = points(i).y
      def getZ(i: Int) = points(i).z
      override def getCoordinate(i: Int) = points(i)
    }

  def apply(points: Map[Int, Coordinate]): IndexedPointSet =
    apply(points, points.size)

  def apply(points: Int => Coordinate, len: Int): IndexedPointSet =
    new IndexedPointSet {
      def length = len
      def getX(i: Int) = points(i).x
      def getY(i: Int) = points(i).y
      def getZ(i: Int) = points(i).z
      override def getCoordinate(i: Int) = points(i)
    }

  implicit def coordinateArrayToIndexedPointSet(points: Array[Coordinate]): CompleteIndexedPointSet =
    apply(points)
}
