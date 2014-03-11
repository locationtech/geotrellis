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
 * Given a Geometry object, inspect the underlying geometry type
 * and recursively flatten it if it is a GeometryCollection
 */
case class FlattenGeometry[D](g1: Op[Geometry[D]]) extends Operation[List[Geometry[D]]] {

  def flattenGeometry(g: jts.Geometry): List[jts.Geometry] = {
    val numGeometries = g.getNumGeometries
    if (numGeometries > 1) {
      (0 until numGeometries).flatMap({ i =>
        flattenGeometry(g.getGeometryN(i))
      }).toList
    } else {
      List(g)
    }   
  }

  def _run() = runAsync(List(g1))
  val nextSteps: Steps = {
    case a :: Nil => {
      val g = a.asInstanceOf[Geometry[D]]
      val geoms = flattenGeometry(g.geom)
      Result(geoms.map(geom => Feature(geom, g.data)))
    }
  }
}
