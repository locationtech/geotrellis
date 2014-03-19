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

/**
 * Returns this Geometry's bounding box.
 * 
 * @see [[http://www.vividsolutions.com/jts/javadoc/com/vividsolutions/jts/geom/Geometry.html#getEnvelope()]]
 */
case class GetEnvelope[A](f:Op[Geometry[A]]) extends Op1(f) ({
  (f) => Result(f.mapGeom(_.getEnvelope))
})

