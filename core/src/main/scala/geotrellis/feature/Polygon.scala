/**************************************************************************
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
 **************************************************************************/

package geotrellis.feature

import geotrellis._

import com.vividsolutions.jts.{ geom => jts }


class Polygon[D] (override val geom:jts.Polygon, data:D) extends SingleGeometry(geom,data)

/// Polygon implementation
object Polygon {
  val factory = Feature.factory

  /**
   * Create an empty polygon feature.
   */
  def empty(): Polygon[_] =
    JtsPolygon(factory.createPolygon(null,null), None)

  /**
   * Create an empty polygon feature with data.
   *
   * @param   data  The data of this feature
   */
  def empty[D](data: D): Polygon[D] =
    JtsPolygon(factory.createPolygon(null,null), data)

  /**
   * Create a polgyon feature from a JTS Polygon object.
   *
   * @param   p     JTS Polygon object
   * @param   data  The data of this feature
   */
  def apply[D](p: jts.Polygon, data: D): Polygon[D] =
    JtsPolygon(p, data)

  /**
   * Create a polygon using a list of tuples.
   *
   * This method is not very efficient -- use only for small polygons.
   *
   * @param tpls  Seq of (x,y) tuples
   * @param data  The data of this feature
   */
  def apply[D](tpls: Seq[(Double, Double)], data: D): Polygon[D] = {
    val jtsCoords = tpls.map { case (x, y) => new jts.Coordinate(x, y) }.toArray
    Polygon(jtsCoords, data)
  }

  def apply[D](tpls: Seq[(Int, Int)], data: D)(implicit di: DummyImplicit): Polygon[D] =
    Polygon(tpls.map { case (x, y) => (x.toDouble, y.toDouble) }, data)

  /**
   * Create a polygon using a one-dimensional array with alternating x and y values.
   *
   * @param coords  Array of alternating x and y values
   * @param data    The data of this feature
   */
  def apply[D](coords: Array[Double], data: D): Polygon[D] = {
    val jtsCoords = (0 until (coords.length / 2)).map {
      (i) =>
        new jts.Coordinate(coords(i), coords(i + 1))
    }.toArray
    Polygon(jtsCoords, data)
  }

  /**
   * Create a polygon with an array of JTS Coordinate objects.
   *
   * @param coords  Coordinates of the polygon exterior
   * @param data    The data of this feature
   */
  def apply[D](coords: Array[jts.Coordinate], data: D): Polygon[D] = {
    val shell = factory.createLinearRing(coords)
    val jts = factory.createPolygon(shell, Array())
    JtsPolygon(jts, data)
  }

  /**
   * Create a polygon with arrays of JTS coordinate objects.
   *
   * @param exterior  Coordinates of the exterior shell
   * @param holes     Interior holes represented by array of coordinate arrays
   * @param data      The data of this feature
   */
  def apply[D](exterior: Array[jts.Coordinate], holes: Array[Array[jts.Coordinate]], data: D): Polygon[D] = {
    val shellRing = factory.createLinearRing(exterior)
    val holeRings = holes.map( factory.createLinearRing(_) ).toArray
    val jts = factory.createPolygon(shellRing, holeRings)
    JtsPolygon(createJtsPolygon(exterior, holes), data)
  }

  protected[geotrellis] def createJtsPolygon(exterior: Array[jts.Coordinate], holes: Array[Array[jts.Coordinate]]) = {
    val shellRing = factory.createLinearRing(exterior)
    val holeRings = holes.map(factory.createLinearRing(_)).toArray
    factory.createPolygon(shellRing, holeRings)
  }

  protected[geotrellis] def createJtsPolygonFromArrays(exterior: Array[Array[Double]], holes: Array[Array[Array[Double]]]) = {
    val shellRing = (0 until exterior.length).map {
      (i) => new jts.Coordinate(exterior(i)(0), exterior(i)(1))
    }.toArray
    val holeRings = holes.map(
      ring => ring.map(
        coordArray => {
          new jts.Coordinate(coordArray(0), coordArray(1))
        }))
    val polygon = createJtsPolygon(shellRing, holeRings)
    polygon
  }

  protected[geotrellis] def createJtsPolygonFromSeqs(exterior: Seq[Seq[Double]], holes: Seq[Seq[Seq[Double]]]) = {
    val shellRing = (0 until exterior.length).map {
      (i) => new jts.Coordinate(exterior(i)(0), exterior(i)(1))
    }.toArray
    val holeRings = holes.map(
      ring => ring.map(
        coordArray => {
          new jts.Coordinate(coordArray(0), coordArray(1))
        }).toArray).toArray
    createJtsPolygon(shellRing, holeRings)
  }

  /**
   * Create a polygon using an array of rings, the first being the exterior ring.
   *
   * Each ring array is an array of two element coordinate arrays, e.g. Array(x,y)
   */
  def apply[D](coords: Array[Array[Array[Double]]], data: D): Polygon[D] =
    Polygon(createJtsPolygonFromArrays(coords.head, coords.tail), data)

  /**
   * Create a polygon using an array of rings, the first being the exterior ring.
   *
   * Each ring array is an array of two element coordinate arrays, e.g. Array(1.0, 2.0)
   *
   * The top level list is the list of rings, the first inner list is a list of coordinates,
   * and each inner list has two elements, x and y.
   *
   * @param coords   A list of polygon rings, represented as a list of two element lists.
   * @param data     The data for this feature.
   */
  def apply[D](coords: Seq[Seq[Seq[Double]]], data: D)(implicit dummy: DI, dummy2: DI): Polygon[D] = {
    val exterior = coords.head
    val shellRing = (0 until exterior.length).map {
      (i) => new jts.Coordinate(exterior(i)(0), exterior(i)(1))
    }.toArray
    val holeRings = coords.tail.map(
      ring => ring.map(
        coordArray => {
          new jts.Coordinate(coordArray(0), coordArray(1))
        }).toArray).toArray

    Polygon(shellRing, holeRings, data)
  }

  /**
   * Create a polgyon feature from a JTS Geometry object.
   *
   * Beware: Only use when you are certain the Geometry object
   * is a polygon.
   *
   * @param g   JTS Geometry
   * @param d   The data of this feature
   */
  def apply[D](g: jts.Geometry, data: D): Polygon[D] =
    JtsPolygon(g.asInstanceOf[jts.Polygon], data)

}

case class JtsPolygon[D](g: jts.Polygon, d: D) extends Polygon(g, d)
