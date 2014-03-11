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

package geotrellis.feature.op.geometry

import geotrellis._
import geotrellis.feature._
import geotrellis.{ op => liftOp }
import com.vividsolutions.jts.{ geom => jts }

import scala.reflect.runtime.currentMirror
import scala.reflect.runtime.universe._

  /**
   * Returns a Geometry as a Polygon Set.
   */
case class AsPolygonSet[D](geom: Op[Geometry[D]]) extends Operation[List[Polygon[D]]] {
  def flattenGeometry(g: jts.Geometry):List[jts.Polygon] = g match {
    case g: jts.GeometryCollection => (0 until g.getNumGeometries).flatMap(
      i => flattenGeometry(g.getGeometryN(i))).toList
    case l: jts.LineString => List()
    case p: jts.Point => List()
    case p: jts.Polygon => List(p)
  }

  def _run() = runAsync(geom :: Nil)

  val nextSteps:Steps = {
    case a :: Nil => {
      val g = a.asInstanceOf[Geometry[D]]
      val geoms = flattenGeometry(g.geom)
      val r:List[Polygon[D]] = geoms.map(geom => Polygon(geom, g.data))
      Result(r)
    }
  }
}
