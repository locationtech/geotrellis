/*
 * Copyright 2018 Azavea
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

package geotrellis.vector.conf

import geotrellis.util.LazyLogging

import org.locationtech.jts.geom.PrecisionModel
import org.locationtech.jts.precision.GeometryPrecisionReducer

case class Simplification(scale: Double = 1e12) {
  // 12 digits is maximum to avoid [[TopologyException]], see https://web.archive.org/web/20160226031453/http://tsusiatsoftware.net/jts/jts-faq/jts-faq.html#D9
  lazy val simplifier: GeometryPrecisionReducer = new GeometryPrecisionReducer(new PrecisionModel(scale))
}
case class Precision(`type`: String = "floating")
case class JtsConfig(precision: Precision = Precision(), simplification: Simplification = Simplification()) extends LazyLogging {
  val precisionType: String = precision.`type`
  val precisionModel: PrecisionModel = precisionType match {
    case "floating" => new PrecisionModel()
    case "floating_single" => new PrecisionModel(PrecisionModel.FLOATING_SINGLE)
    case "fixed" => new PrecisionModel(simplification.scale)
    case _ => throw new IllegalArgumentException(s"""Unrecognized JTS precision model, ${precisionType}; expected "floating", "floating_single", or "fixed" """)
  }
  val simplifier: GeometryPrecisionReducer = simplification.simplifier
}

object JtsConfig {
  lazy val conf: JtsConfig = pureconfig.loadConfigOrThrow[JtsConfig]("geotrellis.jts")
  implicit def jtsConfigToClass(obj: JtsConfig.type): JtsConfig = conf
}
