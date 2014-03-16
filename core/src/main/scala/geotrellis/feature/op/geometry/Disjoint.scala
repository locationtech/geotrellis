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

package geotrellis.feature.op.geometry

import geotrellis._
import geotrellis.feature._
import com.vividsolutions.jts.geom

/** Tests if two geometries are disjoint.
  *  @param g      first geometry 
  *  @param other  other geometry 
  * 
  *  @see [[http://tsusiatsoftware.net/jts/javadoc/com/vividsolutions/jts/geom/Geometry.html#intersection(com.vividsolutions.jts.geom.Geometry) "JTS documentation"]]
  */
case class Disjoint (g:Op[Geometry[_]], other:Op[Geometry[_]]) extends Op2(g,other) ({
  (g,other) => Result(g.geom.disjoint(other.geom))
})

