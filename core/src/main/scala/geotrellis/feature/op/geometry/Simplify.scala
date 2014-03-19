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

import com.vividsolutions.jts.simplify.TopologyPreservingSimplifier

/**
  * Simplify a polygon or multipolygon.
  *
  * This operation uses a topology preserving simplifier that ensures the result has the same
  * characteristics as the input.  
  *
  * @param g  Geometry to simplify
  * @param distance  Distance tolerance for simplification
  *
  *
  *  @see [[http://tsusiatsoftware.net/jts/javadoc/com/vividsolutions/jts/simplify/TopologyPreservingSimplifier.html "JTS documentation"]]
  */
case class Simplify[A](g:Op[Geometry[A]], distanceTolerance:Op[Double]) extends Op2(g,distanceTolerance) ({
  (g,distanceTolerance) => 
    Result(g.mapGeom( TopologyPreservingSimplifier.simplify(_, distanceTolerance)))
})
