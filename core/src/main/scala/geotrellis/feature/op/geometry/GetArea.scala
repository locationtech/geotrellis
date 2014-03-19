/*
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
 */

package geotrellis.feature.op.geometry

import geotrellis._
import geotrellis.feature._
import com.vividsolutions.jts.geom

/** Returns the area of a geometry.
  * 
  *  @param g      geometry for area calculation
  * 
  *  @see [[http://tsusiatsoftware.net/jts/javadoc/com/vividsolutions/jts/geom/Geometry.html#getArea() "JTS documentation"]]
  */
case class GetArea(g:Op[Geometry[_]]) extends Op1(g) ({
  (g) => Result(g.geom.getArea)
})

