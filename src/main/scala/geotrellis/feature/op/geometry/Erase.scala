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

package geotrellis.feature.op.geometry

import geotrellis._
import geotrellis.feature._
import com.vividsolutions.jts.geom

/**
 * Returns a geometry that contains the points in the first geometry that 
 * aren't in the second geometry.
 *  @param g      first geometry  
 *  @param other  other geometry 
 * 
 *  @tparam A    type of feature data
 *
 *  @see [[http://tsusiatsoftware.net/jts/javadoc/com/vividsolutions/jts/geom/Geometry.html#difference(com.vividsolutions.jts.geom.Geometry) "JTS documentation"]]
  */
case class Erase[A](g:Op[Geometry[A]], other:Op[Geometry[_]]) extends Op2(g,other) ({
  (g,other) => Result(g.mapGeom(_.difference(other.geom)))
})


object GetDifference {
/**
 * Returns a geometry that contains the points in the first geometry that 
 * aren't in the second geometry.  Creates a [geotrellis.feature.op.overlay.Erase] operation.
 *
 *  @param g      first geometry  
 *  @param other  other geometry 
 * 
 *  @tparam A    type of feature data
 *
 *  @see [[http://tsusiatsoftware.net/jts/javadoc/com/vividsolutions/jts/geom/Geometry.html#difference(com.vividsolutions.jts.geom.Geometry) "JTS documentation"]]
  */
  def apply[A](g:Op[Geometry[A]], other:Op[Geometry[_]]) = Erase(g, other)
}
