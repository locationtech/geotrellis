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

class MultiLineString[D](override val geom:jts.MultiLineString, data:D) 
extends GeometryCollection(geom, data) {
  def flatten:List[LineString[D]] = 
    (0 until geom.getNumGeometries).map( 
      i => new JtsLineString(geom.getGeometryN(i).asInstanceOf[jts.LineString],data)).toList
}

/// MultiLineString implementation
/**
 * A MultiLineString feature is a set of lines with associated data.
 */
object MultiLineString {
  val factory = Feature.factory 

  /**
   * Create an empty MultiLineString.
   */
  def empty():MultiLineString[_] = 
    JtsMultiLineString(factory.createMultiLineString(Array[jts.LineString]()), None)

  /**
   * Create an empty MultiLineString feature with data.
   *
   * @param   data  Data of this feature
   */
  def empty[D](data: D):MultiLineString[D] = 
    JtsMultiLineString(factory.createMultiLineString(Array[jts.LineString]()), data)

  /**
   * Create a MultiLineString feature.
   *
   * @param   g     JTS MutliLineString object
   * @param   data  Data of this feature
   */
  def apply[D](g: jts.MultiLineString, data: D):MultiLineString[D] = 
    JtsMultiLineString(g, data)

  /**
   * Create a MultiLineString feature with sequences of coordinate values.
   *
   * A MultiLineString feature is *
   * The coordinate values are represented as a sequence of coordinates, each
   * represented as a sequence of two double values (x and y).
   *
   * @param coords    Sequence of x and y sequences
   * @param data      The data of this feature
   */
  def apply[D](multiLineCoords: Seq[Seq[Seq[Double]]], data: D):MultiLineString[D] = {
    val jtsLines = multiLineCoords.map( coords => {
      val coordArray = Array (
          new jts.Coordinate(coords(0)(0), coords(0)(1)),
          new jts.Coordinate(coords(1)(0), coords(1)(1))
      )
      factory.createLineString (coordArray)
    }).toArray
    MultiLineString(factory.createMultiLineString(jtsLines), data) 
  }
}

case class JtsMultiLineString[D](g: jts.MultiLineString, d: D) extends MultiLineString(g,d)
