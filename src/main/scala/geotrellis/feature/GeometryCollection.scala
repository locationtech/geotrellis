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

class GeometryCollection[D](override val geom:jts.GeometryCollection, data:D) extends Geometry(geom,data)

case class JtsGeometryCollection[D](g: jts.GeometryCollection, d:D) extends GeometryCollection(g,d)

object GeometryCollection {
  val factory = Feature.factory

  /**
   * Create an empty geometry collection feature.
   */
  def empty():GeometryCollection[_] = 
    JtsGeometryCollection(factory.createGeometryCollection(Array[jts.Geometry]()), None)

  /**
   * Create an empty geometry collection feature with data.
   *
   * @param   data  The data of this feature
   */
  def empty[D](d:D):GeometryCollection[D] = 
    JtsGeometryCollection(factory.createGeometryCollection(Array[jts.Geometry]()), d)

  /**
   * Create a geometry collection feature from a JTS GeometryCollection object.
   *
   * @param   gc    JTS GeometryColleciton object
   * @param   data  The data of this feature
   */
  def apply[D](gc:jts.GeometryCollection, d:D):GeometryCollection[D] = 
    JtsGeometryCollection(gc, d)

  /**
   * Create a geometry collection feature from a sequence of JTS Geometry objects.
   *
   * @param   geometries    Seq of JTS Geometry objects.
   * @param   data          The data of this feature
   */
  def apply[D](geometries:Seq[jts.Geometry], d:D):GeometryCollection[D] = {
    val gc = factory.createGeometryCollection(geometries.toArray)
    JtsGeometryCollection(gc,d)
  }
}
