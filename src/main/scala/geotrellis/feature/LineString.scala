/*******************************************************************************
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
 ******************************************************************************/

package geotrellis.feature

import geotrellis._

import com.vividsolutions.jts.{ geom => jts }

class LineString[D] (override val geom:jts.LineString, data:D) extends SingleGeometry(geom,data)

/// Line implementation
object LineString {
  val factory = Feature.factory

  /**
   * Create an Empty LineString (aka a line) feature.
   *
   */
  def empty(): LineString[_] =
    JtsLineString(factory.createLineString(Array[jts.Coordinate]()), None)

  /**
   * Create an Empty LineString (aka a line) feature with data.
   *
   * @param   data  Data of this feature
   */
  def empty[D](data: D): LineString[D] =
    JtsLineString(factory.createLineString(Array[jts.Coordinate]()), data)

  /**
   * Create a LineString (aka a line) feature.
   *
   * @param   g     JTS LineString object
   * @param   data  Data of this feature
   */
  def apply[D](g: jts.LineString, data: D): LineString[D] =
    JtsLineString(g, data)

  /**
   * Create a LineString (aka a line) feature.
   *
   * @param   g     jts.Geometry object
   * @param   data  Data of this feature
   */
  def apply[D](g: jts.Geometry, data: D): LineString[D] =
    JtsLineString(g.asInstanceOf[jts.LineString], data)

  /**
   * Create a LineString (aka a line) given x and y coordinates, as integers.
   *
   * @param x0  x coordinate of first point
   * @param y0  y coordinate of first point
   * @param x1  x coordinate of second point
   * @param y1  y coordinate of second point
   * @param data  Data value of this feature
   */
  def apply[D](x0: Double, y0: Double, x1: Double, y1: Double, data: D): LineString[D] = {
    val g = factory.createLineString(Array(new jts.Coordinate(x0, y0), new jts.Coordinate(x1, y1)))
    JtsLineString(g, data)
  }

  /**
   * Create a LineString (aka a line) given x and y coordinates, as integers.
   *
   * @param x0  x coordinate of first point
   * @param y0  y coordinate of first point
   * @param x1  x coordinate of second point
   * @param y1  y coordinate of second point
   * @param data  Data value of this feature
   */
  def apply[D](x0: Int, y0: Int, x1: Int, y1: Int, data: D): LineString[D] = {
    val g = factory.createLineString(Array(new jts.Coordinate(x0, y0), new jts.Coordinate(x1, y1)))
    JtsLineString(g, data)
  }

  /**
   * Create a LineString (aka a line) given x and y coordinates, as integers.
   *
   * @param tpls  Seq of (x,y) tuples
   * @param data  Data value of this feature
   */
  def apply[D](tpls: Seq[(Int, Int)], data: D): LineString[D] = {
    val coords = tpls.map { t => new jts.Coordinate(t._1, t._2) }.toArray
    val g = factory.createLineString(coords)
    JtsLineString(g, data)
  }

  /**
   * Create a LineString (aka a line) given x and y coordinates, as integers.
   *
   * @param tpls  Seq of (x,y) tuples
   * @param data  Data value of this feature
   */
  def apply[D](tpls: Seq[(Double, Double)], data: D)(implicit d:DummyImplicit): LineString[D] = {
    val coords = tpls.map { t => new jts.Coordinate(t._1, t._2) }.toArray
    val g = factory.createLineString(coords)
    JtsLineString(g, data)
  }
}

/**
 * Implementation of LineString feature with underlying jts instance.
 */
case class JtsLineString[D](g: jts.LineString, d: D) extends LineString(g,d)
