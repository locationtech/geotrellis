/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.vector

import geotrellis.util.LazyLogging
import geotrellis.vector.conf.JtsConfig

import org.locationtech.jts.geom
import org.locationtech.jts.geom.impl.CoordinateArraySequenceFactory
import org.locationtech.jts.geom.{CoordinateSequenceFactory, GeometryFactory, PrecisionModel}
import org.locationtech.jts.precision.GeometryPrecisionReducer

object GeomFactory extends LazyLogging {
  val precisionType: String = JtsConfig.precisionType
  val precisionModel: PrecisionModel = JtsConfig.precisionModel
  lazy val simplifier: GeometryPrecisionReducer = JtsConfig.simplifier

  val factory: GeometryFactory = new geom.GeometryFactory(precisionModel)
}
