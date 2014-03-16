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

class Geometry[D] (val geom:jts.Geometry, val data:D) extends Feature[jts.Geometry, D] 

class SingleGeometry[D] (override val geom:jts.Geometry, data:D) extends Geometry(geom, data)

case class JtsGeometry[D](g: jts.Geometry, d: D) extends Geometry(g,d)

/**
 * Turn tuples into JtsCoordinates.
 */
trait UsesCoords {
  def makeCoord(x: Double, y: Double) = { new jts.Coordinate(x, y) }

  def makeCoords(tpls: Array[(Double, Double)]) = {
    tpls.map { pt => makeCoord(pt._1, pt._2) }.toArray
  }
}

object Geometry {
  def apply[D](geom:jts.Geometry, data:D) = new Geometry(geom, data)
}

